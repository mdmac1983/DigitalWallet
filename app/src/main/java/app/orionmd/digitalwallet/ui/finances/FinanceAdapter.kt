package app.orionmd.digitalwallet.ui.finances

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.FinanceItem
import app.orionmd.digitalwallet.data.FinanceType
import app.orionmd.digitalwallet.data.displayTypeLabel
import app.orionmd.digitalwallet.databinding.ItemFinanceBinding

class FinanceAdapter(
    private val context: Context,
    private val onClick: (FinanceItem) -> Unit
) : RecyclerView.Adapter<FinanceAdapter.ViewHolder>() {

    private var items: List<FinanceItem> = emptyList()

    fun submitList(newItems: List<FinanceItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFinanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemFinanceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: FinanceItem) {
            binding.textCategory.text = entry.category.ifBlank {
                entry.displayTypeLabel(context.getString(R.string.type_income), context.getString(R.string.type_expense))
            }
            binding.textSourceDate.text = listOf(entry.source, entry.date).filter { it.isNotBlank() }.joinToString(" • ")

            val isIncome = entry.type == FinanceType.INCOME
            val color = ContextCompat.getColor(context, if (isIncome) R.color.wallet_income else R.color.wallet_expense)
            binding.typeIndicator.setBackgroundColor(color)
            binding.textAmount.setTextColor(color)
            binding.textAmount.text = (if (isIncome) "+" else "-") + "$" + entry.amount.removePrefix("-").removePrefix("+")

            binding.root.setOnClickListener { onClick(entry) }
        }
    }
}
