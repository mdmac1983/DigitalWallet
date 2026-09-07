package app.orionmd.digitalwallet.ui.statements

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.DocumentStore
import app.orionmd.digitalwallet.data.StatementItem
import app.orionmd.digitalwallet.data.StatementRepository
import app.orionmd.digitalwallet.databinding.ActivityStatementsListBinding
import kotlinx.coroutines.launch

/** Every statement ever uploaded, newest first. Tapping one opens the original file (decrypted
 * to a throwaway cache copy) in whatever viewer the device has for its type; the delete button
 * removes the record and its stored file, but leaves any Finances entries already imported from
 * it in place - deleting a statement is not the same as undoing what it added. */
class StatementsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatementsListBinding
    private lateinit var repository: StatementRepository
    private lateinit var adapter: StatementsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatementsListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = StatementRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = StatementsAdapter(
            onClick = { statement ->
                DocumentStore.openExternally(this, statement.sourceFileName, statement.sourceMimeType, statement.sourceDisplayName)
            },
            onDelete = { statement -> confirmDelete(statement) }
        )
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        lifecycleScope.launch {
            repository.observeAll().collect { items ->
                adapter.submitList(items)
                binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmDelete(statement: StatementItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.statement_delete_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    DocumentStore.delete(this@StatementsListActivity, statement.sourceFileName)
                    repository.delete(statement)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
