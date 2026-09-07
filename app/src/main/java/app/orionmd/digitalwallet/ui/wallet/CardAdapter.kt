package app.orionmd.digitalwallet.ui.wallet

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.data.CardItem
import app.orionmd.digitalwallet.data.ImageStore
import app.orionmd.digitalwallet.databinding.ItemCardBinding
import app.orionmd.digitalwallet.ui.common.RowStyler
import app.orionmd.digitalwallet.ui.common.ViewMode

class CardAdapter(
    private val context: Context,
    private val onClick: (CardItem) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit = {}
) : RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    private var items: List<CardItem> = emptyList()
    private var viewMode: ViewMode = ViewMode.TILES

    fun submitList(newItems: List<CardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setViewMode(mode: ViewMode) {
        viewMode = mode
        notifyDataSetChanged()
    }

    /** The list as currently ordered on screen, including any in-progress drag - used once a drag
     * finishes to persist the new order. */
    fun currentItems(): List<CardItem> = items

    /** Reflects one step of a drag: swaps [fromPosition] and [toPosition] in the in-memory list
     * and animates the move. Nothing is persisted here - see [currentItems]. */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        items = items.toMutableList().apply { add(toPosition, removeAt(fromPosition)) }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: CardItem) {
            binding.textIssuer.text = card.issuer.ifBlank { card.cardName }
            binding.textCardName.text = card.cardName
            binding.textCardholder.text = card.cardholderName
            val digits = card.cardNumber.filter { it.isDigit() }
            binding.textLastFour.text = if (digits.length >= 4) {
                "•••• ${digits.takeLast(4)}"
            } else ""

            val thumb = ImageStore.loadBitmap(context, card.frontImagePath)
            if (thumb != null) {
                binding.imageThumb.setImageBitmap(thumb)
            } else {
                binding.imageThumb.setImageResource(app.orionmd.digitalwallet.R.drawable.ic_card_placeholder)
            }

            binding.root.setOnClickListener { onClick(card) }

            RowStyler.apply(binding.root, binding.imageThumb, 56, viewMode)

            @Suppress("ClickableViewAccessibility")
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }
}
