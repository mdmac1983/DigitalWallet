package app.orionmd.digitalwallet.ui.sales

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.SaleItem
import app.orionmd.digitalwallet.data.SaleRepository
import app.orionmd.digitalwallet.data.SaleStatus
import app.orionmd.digitalwallet.data.balanceDue
import app.orionmd.digitalwallet.data.netFee
import app.orionmd.digitalwallet.databinding.ActivitySaleEditBinding
import kotlinx.coroutines.launch
import java.util.Locale

class SaleEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SALE_ID = "extra_sale_id"
    }

    private lateinit var binding: ActivitySaleEditBinding
    private lateinit var repository: SaleRepository

    private var saleId: Long = -1L
    private var existing: SaleItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaleEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = SaleRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        saleId = intent.getLongExtra(EXTRA_SALE_ID, -1L)
        binding.toolbar.title = getString(if (saleId == -1L) R.string.new_sale_title else R.string.edit_sale_title)

        binding.togglePaid.check(R.id.button_status_unpaid)
        applyStatusVisibility(SaleStatus.UNPAID)
        binding.togglePaid.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                applyStatusVisibility(statusForCheckedId(checkedId))
                updateNetFeePreview()
            }
        }
        binding.buttonSave.setOnClickListener { save() }

        listOf(binding.editPrice, binding.editCommissionFee, binding.editPartnerFee, binding.editAmountPaid).forEach { field ->
            field.doAfterTextChanged { updateNetFeePreview() }
        }
        updateNetFeePreview()

        if (saleId != -1L) {
            lifecycleScope.launch {
                existing = repository.getById(saleId)
                existing?.let { populate(it) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (saleId != -1L) menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_delete -> { confirmDelete(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populate(item: SaleItem) = with(binding) {
        editDate.setText(item.date)
        editClientId.setText(item.clientId)
        editService.setText(item.service)
        editPrice.setText(item.price)
        editCommissionFee.setText(item.commissionFee)
        editPartnerFee.setText(item.partnerFee)
        editAmountPaid.setText(item.amountPaid)
        editNotes.setText(item.notes)
        val checkedId = when (item.status) {
            SaleStatus.PAID -> R.id.button_status_paid
            SaleStatus.PARTIAL -> R.id.button_status_partial
            SaleStatus.UNPAID -> R.id.button_status_unpaid
        }
        togglePaid.check(checkedId)
        applyStatusVisibility(item.status)
        updateNetFeePreview()
    }

    private fun statusForCheckedId(checkedId: Int): SaleStatus = when (checkedId) {
        R.id.button_status_paid -> SaleStatus.PAID
        R.id.button_status_partial -> SaleStatus.PARTIAL
        else -> SaleStatus.UNPAID
    }

    /** The amount-paid field only makes sense for a partially-paid bill - Unpaid is always $0
     * paid and Paid is (by default) the full net fee, so it stays hidden for those. */
    private fun applyStatusVisibility(status: SaleStatus) {
        binding.layoutAmountPaid.visibility = if (status == SaleStatus.PARTIAL) View.VISIBLE else View.GONE
    }

    private fun currentItem(): SaleItem {
        val status = statusForCheckedId(binding.togglePaid.checkedButtonId)
        return SaleItem(
            id = if (saleId == -1L) 0 else saleId,
            date = binding.editDate.text?.toString().orEmpty(),
            clientId = binding.editClientId.text?.toString().orEmpty(),
            service = binding.editService.text?.toString().orEmpty(),
            price = binding.editPrice.text?.toString().orEmpty(),
            commissionFee = binding.editCommissionFee.text?.toString().orEmpty(),
            partnerFee = binding.editPartnerFee.text?.toString().orEmpty(),
            status = status,
            amountPaid = if (status == SaleStatus.PARTIAL) binding.editAmountPaid.text?.toString().orEmpty() else "",
            notes = binding.editNotes.text?.toString().orEmpty()
        )
    }

    private fun updateNetFeePreview() {
        val item = currentItem()
        binding.textNetFee.text = getString(
            R.string.sale_net_fee_format,
            String.format(Locale.US, "$%,.2f", item.netFee())
        )
        binding.textBalanceDue.text = getString(
            R.string.sale_balance_due_format,
            String.format(Locale.US, "$%,.2f", item.balanceDue())
        )
    }

    private fun save() {
        val item = currentItem()
        lifecycleScope.launch {
            repository.save(item)
            finish()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    existing?.let { repository.delete(it) }
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
