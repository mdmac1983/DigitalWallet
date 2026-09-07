package app.orionmd.digitalwallet.ui.finances

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.HoldingItem
import app.orionmd.digitalwallet.data.HoldingRepository
import app.orionmd.digitalwallet.data.HoldingType
import app.orionmd.digitalwallet.databinding.ActivityHoldingEditBinding
import kotlinx.coroutines.launch

class HoldingEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HOLDING_ID = "extra_holding_id"
    }

    private lateinit var binding: ActivityHoldingEditBinding
    private lateinit var repository: HoldingRepository

    private var holdingId: Long = -1L
    private var existing: HoldingItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHoldingEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = HoldingRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        holdingId = intent.getLongExtra(EXTRA_HOLDING_ID, -1L)
        binding.toolbar.title = getString(if (holdingId == -1L) R.string.new_holding_title else R.string.edit_holding_title)

        binding.toggleType.check(R.id.button_type_stock)
        applyTypeVisibility(HoldingType.STOCK)
        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) applyTypeVisibility(typeForCheckedId(checkedId))
        }
        binding.buttonSave.setOnClickListener { save() }

        if (holdingId != -1L) {
            lifecycleScope.launch {
                existing = repository.getById(holdingId)
                existing?.let { populate(it) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (holdingId != -1L) menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_delete -> { confirmDelete(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populate(item: HoldingItem) = with(binding) {
        val checkedId = when (item.holdingType) {
            HoldingType.CRYPTO -> R.id.button_type_crypto
            HoldingType.CASH -> R.id.button_type_cash
            HoldingType.STOCK -> R.id.button_type_stock
        }
        toggleType.check(checkedId)
        applyTypeVisibility(item.holdingType)
        editSymbol.setText(item.symbol)
        editName.setText(item.name)
        editQuantity.setText(item.quantity)
        editCostBasis.setText(item.costBasis)
        editCurrentPrice.setText(item.currentPrice)
        editNotes.setText(item.notes)
    }

    private fun typeForCheckedId(checkedId: Int): HoldingType = when (checkedId) {
        R.id.button_type_crypto -> HoldingType.CRYPTO
        R.id.button_type_cash -> HoldingType.CASH
        else -> HoldingType.STOCK
    }

    /** Cash doesn't have a symbol/name/quantity/cost-basis in the usual sense - it's just an
     * amount. Rather than a separate screen, the same form relabels/hides fields for it, and
     * internally records quantity=1 so [app.orionmd.digitalwallet.data.totalValue] (quantity *
     * currentPrice) keeps meaning "the cash amount" without any special-casing there. */
    private fun applyTypeVisibility(type: HoldingType) = with(binding) {
        val isCash = type == HoldingType.CASH
        layoutName.visibility = if (isCash) View.GONE else View.VISIBLE
        layoutQuantity.visibility = if (isCash) View.GONE else View.VISIBLE
        layoutCostBasis.visibility = if (isCash) View.GONE else View.VISIBLE
        layoutSymbol.hint = getString(if (isCash) R.string.field_cash_label else R.string.field_symbol)
        layoutCurrentPrice.hint = getString(if (isCash) R.string.field_cash_amount else R.string.field_current_price)
    }

    private fun save() {
        val type = typeForCheckedId(binding.toggleType.checkedButtonId)
        val isCash = type == HoldingType.CASH
        val item = HoldingItem(
            id = if (holdingId == -1L) 0 else holdingId,
            holdingType = type,
            symbol = binding.editSymbol.text?.toString().orEmpty().trim().let {
                if (isCash && it.isBlank()) getString(R.string.holding_type_cash) else it
            },
            name = if (isCash) "" else binding.editName.text?.toString().orEmpty(),
            quantity = if (isCash) "1" else binding.editQuantity.text?.toString().orEmpty(),
            costBasis = if (isCash) "" else binding.editCostBasis.text?.toString().orEmpty(),
            currentPrice = binding.editCurrentPrice.text?.toString().orEmpty(),
            notes = binding.editNotes.text?.toString().orEmpty()
        )
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
