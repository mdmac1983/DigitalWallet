package app.orionmd.digitalwallet.ui.finances

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.HoldingItem
import app.orionmd.digitalwallet.data.HoldingType
import app.orionmd.digitalwallet.data.gainLoss
import app.orionmd.digitalwallet.data.quantityValue
import app.orionmd.digitalwallet.data.totalValue
import app.orionmd.digitalwallet.databinding.ItemHoldingBinding
import java.util.Locale

class HoldingAdapter(
    private val context: Context,
    private val onClick: (HoldingItem) -> Unit
) : RecyclerView.Adapter<HoldingAdapter.ViewHolder>() {

    private var items: List<HoldingItem> = emptyList()

    fun submitList(newItems: List<HoldingItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHoldingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemHoldingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(holding: HoldingItem) {
            binding.textSymbol.text = holding.symbol
            binding.textTypeBadge.text = context.getString(
                when (holding.holdingType) {
                    HoldingType.CRYPTO -> R.string.holding_type_crypto
                    HoldingType.CASH -> R.string.holding_type_cash
                    HoldingType.STOCK -> R.string.holding_type_stock
                }
            )

            val qty = formatNumber(holding.quantityValue())
            binding.textHoldingMeta.text = when {
                holding.holdingType == HoldingType.CASH -> context.getString(R.string.holding_cash_meta)
                holding.name.isNotBlank() -> "${holding.name} • $qty"
                else -> qty
            }

            val value = holding.totalValue()
            binding.textValue.text = if (value != null) formatCurrency(value) else context.getString(R.string.value_not_set)

            val gainLoss = holding.gainLoss()
            if (gainLoss != null) {
                val color = ContextCompat.getColor(
                    context, if (gainLoss >= 0) R.color.wallet_income else R.color.wallet_expense
                )
                binding.textGainLoss.setTextColor(color)
                binding.textGainLoss.text = (if (gainLoss >= 0) "+" else "-") + formatCurrency(kotlin.math.abs(gainLoss))
            } else {
                binding.textGainLoss.setTextColor(ContextCompat.getColor(context, R.color.wallet_divider))
                binding.textGainLoss.text = context.getString(R.string.gain_loss_not_tracked)
            }

            binding.root.setOnClickListener { onClick(holding) }
        }
    }

    companion object {
        fun formatCurrency(value: Double): String = String.format(Locale.US, "$%,.2f", value)

        /** Quantities aren't always whole numbers (fractional shares, crypto) but shouldn't show
         * a pile of trailing zeros either. */
        fun formatNumber(value: Double): String {
            return if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                String.format(Locale.US, "%,.4f", value).trimEnd('0').trimEnd('.')
            }
        }
    }
}
