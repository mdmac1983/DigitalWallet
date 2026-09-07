package app.orionmd.digitalwallet.ui.statements

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.FinanceItem
import app.orionmd.digitalwallet.data.FinanceRepository
import app.orionmd.digitalwallet.data.FinanceType
import app.orionmd.digitalwallet.databinding.ActivityReviewParsedBinding
import app.orionmd.digitalwallet.databinding.DialogEditParsedBinding
import app.orionmd.digitalwallet.ui.finances.FinanceEditActivity
import kotlinx.coroutines.launch

/**
 * Shows every candidate [ParsedTransaction] found by [StatementParser] so the user can uncheck
 * junk, fix a misread amount/date, or add a category - nothing reaches [FinanceRepository] until
 * they tap "Add selected". Reads its input from [ParsedTransactionBuffer] rather than Intent
 * extras since the candidate list never needs Parcelable support.
 */
class ReviewParsedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewParsedBinding
    private lateinit var repository: FinanceRepository
    private lateinit var adapter: ParsedTransactionAdapter
    private val items = mutableListOf<ParsedTransaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewParsedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = FinanceRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        items.addAll(ParsedTransactionBuffer.pending)
        adapter = ParsedTransactionAdapter(items) { position, item -> showEditDialog(position, item) }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        binding.buttonAddSelected.setOnClickListener { addSelected() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showEditDialog(position: Int, item: ParsedTransaction) {
        val dialogBinding = DialogEditParsedBinding.inflate(LayoutInflater.from(this))
        dialogBinding.toggleType.check(
            if (item.type == FinanceType.INCOME) R.id.button_type_income else R.id.button_type_expense
        )
        dialogBinding.editAmount.setText(item.amount)
        dialogBinding.editDate.setText(item.date)
        dialogBinding.editCategory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, FinanceEditActivity.DEFAULT_CATEGORIES)
        )
        dialogBinding.editCategory.setText(item.category, false)
        dialogBinding.editSource.setText(item.description)

        AlertDialog.Builder(this)
            .setTitle(R.string.review_edit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.button_save) { _, _ ->
                items[position] = item.copy(
                    type = if (dialogBinding.toggleType.checkedButtonId == R.id.button_type_income) FinanceType.INCOME else FinanceType.EXPENSE,
                    amount = dialogBinding.editAmount.text?.toString().orEmpty(),
                    date = dialogBinding.editDate.text?.toString().orEmpty(),
                    category = dialogBinding.editCategory.text?.toString().orEmpty(),
                    description = dialogBinding.editSource.text?.toString().orEmpty()
                )
                adapter.notifyItemUpdated(position)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addSelected() {
        val selected = items.filter { it.include }
        if (selected.isEmpty()) {
            finish()
            return
        }
        val statementId = ParsedTransactionBuffer.statementId.takeIf { it > 0 }
        val defaultSource = ParsedTransactionBuffer.defaultSource
        lifecycleScope.launch {
            selected.forEach { parsed ->
                repository.save(
                    FinanceItem(
                        type = parsed.type,
                        amount = parsed.amount,
                        date = parsed.date,
                        category = parsed.category,
                        source = parsed.description.ifBlank { defaultSource },
                        notes = "",
                        statementId = statementId
                    )
                )
            }
            Toast.makeText(
                this@ReviewParsedActivity,
                getString(R.string.parsed_added_format, selected.size),
                Toast.LENGTH_SHORT
            ).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
