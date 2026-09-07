package app.orionmd.digitalwallet.ui.finances

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.FinanceItem
import app.orionmd.digitalwallet.data.FinanceRepository
import app.orionmd.digitalwallet.data.FinanceType
import app.orionmd.digitalwallet.databinding.ActivityFinanceEditBinding
import kotlinx.coroutines.launch

class FinanceEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FINANCE_ID = "extra_finance_id"

        /** Suggested categories shown in the dropdown; the field also accepts free text. */
        val DEFAULT_CATEGORIES = listOf(
            "Salary", "Freelance", "Interest / Dividends", "Rent / Mortgage", "Utilities",
            "Groceries", "Transportation", "Insurance", "Bills", "Entertainment",
            "Investment", "Transfer", "Other"
        )
    }

    private lateinit var binding: ActivityFinanceEditBinding
    private lateinit var repository: FinanceRepository

    private var financeId: Long = -1L
    private var existing: FinanceItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinanceEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = FinanceRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        financeId = intent.getLongExtra(EXTRA_FINANCE_ID, -1L)
        binding.toolbar.title = getString(if (financeId == -1L) R.string.new_finance_title else R.string.edit_finance_title)

        binding.editCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, DEFAULT_CATEGORIES))
        binding.toggleType.check(R.id.button_type_expense)

        binding.buttonSave.setOnClickListener { save() }

        if (financeId != -1L) {
            lifecycleScope.launch {
                existing = repository.getById(financeId)
                existing?.let { populate(it) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (financeId != -1L) menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_delete -> { confirmDelete(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populate(item: FinanceItem) = with(binding) {
        toggleType.check(if (item.type == FinanceType.INCOME) R.id.button_type_income else R.id.button_type_expense)
        editAmount.setText(item.amount)
        editDate.setText(item.date)
        editCategory.setText(item.category, false)
        editSource.setText(item.source)
        editNotes.setText(item.notes)
        editTypeLabel.setText(item.typeLabel)
    }

    private fun save() {
        val type = if (binding.toggleType.checkedButtonId == R.id.button_type_income) FinanceType.INCOME else FinanceType.EXPENSE
        val item = FinanceItem(
            id = if (financeId == -1L) 0 else financeId,
            type = type,
            amount = binding.editAmount.text?.toString().orEmpty(),
            date = binding.editDate.text?.toString().orEmpty(),
            category = binding.editCategory.text?.toString().orEmpty(),
            source = binding.editSource.text?.toString().orEmpty(),
            notes = binding.editNotes.text?.toString().orEmpty(),
            typeLabel = binding.editTypeLabel.text?.toString().orEmpty().trim()
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
