package app.orionmd.digitalwallet.ui.statements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.FinanceType
import app.orionmd.digitalwallet.databinding.ItemParsedTransactionBinding

class ParsedTransactionAdapter(
    private val items: MutableList<ParsedTransaction>,
    private val onEdit: (position: Int, item: ParsedTransaction) -> Unit
) : RecyclerView.Adapter<ParsedTransactionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParsedTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(position)

    override fun getItemCount() = items.size

    fun notifyItemUpdated(position: Int) = notifyItemChanged(position)

    inner class ViewHolder(private val binding: ItemParsedTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item = items[position]
            val sign = if (item.type == FinanceType.INCOME) "+" else "-"
            binding.textSummary.text = "$sign$${item.amount}" + if (item.category.isNotBlank()) " · ${item.category}" else ""
            binding.textDetail.text = listOf(item.date, item.description).filter { it.isNotBlank() }.joinToString(" • ")
                .ifBlank { itemView.context.getString(R.string.field_notes) }

            binding.checkboxInclude.setOnCheckedChangeListener(null)
            binding.checkboxInclude.isChecked = item.include
            binding.checkboxInclude.setOnCheckedChangeListener { _, checked -> items[position].include = checked }

            binding.buttonEdit.setOnClickListener { onEdit(position, item) }
        }
    }
}
