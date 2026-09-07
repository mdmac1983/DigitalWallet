package app.orionmd.digitalwallet.ui.sales

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.SaleItem
import app.orionmd.digitalwallet.data.SaleStatus
import app.orionmd.digitalwallet.data.balanceDue
import app.orionmd.digitalwallet.data.netFee
import app.orionmd.digitalwallet.databinding.ItemSaleBinding
import app.orionmd.digitalwallet.ui.common.RowStyler
import app.orionmd.digitalwallet.ui.common.ViewMode
import java.util.Locale

class SaleAdapter(
    private val context: Context,
    private val onClick: (SaleItem) -> Unit
) : RecyclerView.Adapter<SaleAdapter.ViewHolder>() {

    private var items: List<SaleItem> = emptyList()
    /** Display-only entry numbers keyed by sale id - see [SalesFragment] for how this is built:
     * always by creation order, independent of the list's current (most-recently-updated-first)
     * sort order, so a sale's number never changes just because it (or another entry) was edited. */
    private var numbering: Map<Long, Int> = emptyMap()
    private var viewMode: ViewMode = ViewMode.TILES

    fun submitList(newItems: List<SaleItem>, numbering: Map<Long, Int> = emptyMap()) {
        items = newItems
        this.numbering = numbering
        notifyDataSetChanged()
    }

    fun setViewMode(mode: ViewMode) {
        viewMode = mode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemSaleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(sale: SaleItem) {
            val number = numbering[sale.id]
            binding.textEntryNumber.text = if (number != null) {
                context.getString(R.string.sale_entry_number_format, "%02d".format(number))
            } else {
                ""
            }
            binding.textClientId.text = sale.clientId
            binding.textServiceDate.text = listOf(sale.service, sale.date).filter { it.isNotBlank() }.joinToString(" • ")
            binding.textNetFee.text = formatCurrency(sale.netFee())

            val statusColor = ContextCompat.getColor(
                context,
                when (sale.status) {
                    SaleStatus.PAID -> R.color.wallet_income
                    SaleStatus.PARTIAL -> R.color.wallet_accent
                    SaleStatus.UNPAID -> R.color.wallet_expense
                }
            )
            binding.textStatus.setTextColor(statusColor)
            binding.textStatus.text = context.getString(
                when (sale.status) {
                    SaleStatus.PAID -> R.string.sale_status_paid
                    SaleStatus.PARTIAL -> R.string.sale_status_partial
                    SaleStatus.UNPAID -> R.string.sale_status_unpaid
                }
            )

            val balanceDue = sale.balanceDue()
            if (sale.status == SaleStatus.PARTIAL && balanceDue > 0.0) {
                binding.textBalanceDue.visibility = android.view.View.VISIBLE
                binding.textBalanceDue.text = context.getString(R.string.sale_balance_due_format, formatCurrency(balanceDue))
            } else {
                binding.textBalanceDue.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(sale) }

            RowStyler.apply(binding.root, null, 0, viewMode)
        }
    }

    companion object {
        fun formatCurrency(value: Double): String = String.format(Locale.US, "$%,.2f", value)
    }
}
