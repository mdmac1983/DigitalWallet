package app.orionmd.digitalwallet.ui.statements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.StatementItem
import app.orionmd.digitalwallet.data.StatementType
import app.orionmd.digitalwallet.databinding.ItemStatementBinding

class StatementsAdapter(
    private val onClick: (StatementItem) -> Unit,
    private val onDelete: (StatementItem) -> Unit
) : RecyclerView.Adapter<StatementsAdapter.ViewHolder>() {

    private var items: List<StatementItem> = emptyList()

    fun submitList(newItems: List<StatementItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemStatementBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StatementItem) {
            binding.textInstitution.text = item.institution
            val typeLabel = when (item.type) {
                StatementType.BANK -> "Bank"
                StatementType.CREDIT_CARD -> "Credit Card"
                StatementType.INVESTMENT -> "Investment"
            }
            binding.textMeta.text = "$typeLabel · ${MonthUtil.label(item.statementMonth)}"

            if (item.beginningBalance.isNotBlank() || item.endingBalance.isNotBlank()) {
                val context = binding.root.context
                binding.textBalance.text = context.getString(
                    R.string.statement_balance_format,
                    formatBalance(item.beginningBalance),
                    formatBalance(item.endingBalance)
                )
                binding.textBalance.visibility = View.VISIBLE
            } else {
                binding.textBalance.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(item) }
            binding.buttonDelete.setOnClickListener { onDelete(item) }
        }

        private fun formatBalance(raw: String): String {
            if (raw.isBlank()) return "?"
            val value = raw.toDoubleOrNull() ?: return raw
            return String.format(java.util.Locale.US, "$%,.2f", value)
        }
    }
}
