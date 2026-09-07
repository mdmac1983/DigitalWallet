package app.orionmd.digitalwallet.ui.wallet

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.IdDocumentItem
import app.orionmd.digitalwallet.data.IdDocumentType
import app.orionmd.digitalwallet.data.ImageStore
import app.orionmd.digitalwallet.databinding.ItemIdBinding
import app.orionmd.digitalwallet.ui.common.RowStyler
import app.orionmd.digitalwallet.ui.common.ViewMode

class IdAdapter(
    private val context: Context,
    private val onClick: (IdDocumentItem) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit = {}
) : RecyclerView.Adapter<IdAdapter.ViewHolder>() {

    private var items: List<IdDocumentItem> = emptyList()
    private var viewMode: ViewMode = ViewMode.TILES

    fun submitList(newItems: List<IdDocumentItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setViewMode(mode: ViewMode) {
        viewMode = mode
        notifyDataSetChanged()
    }

    /** The list as currently ordered on screen, including any in-progress drag - used once a drag
     * finishes to persist the new order. */
    fun currentItems(): List<IdDocumentItem> = items

    /** Reflects one step of a drag: swaps [fromPosition] and [toPosition] in the in-memory list
     * and animates the move. Nothing is persisted here - see [currentItems]. */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        items = items.toMutableList().apply { add(toPosition, removeAt(fromPosition)) }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemIdBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(doc: IdDocumentItem) {
            binding.textDocType.text = context.getString(labelFor(doc.documentType))
            binding.textFullName.text = doc.fullName
            val digits = doc.documentNumber
            binding.textDocNumber.text = if (digits.length > 4) {
                "•••• ${digits.takeLast(4)}"
            } else digits

            val thumb = ImageStore.loadBitmap(context, doc.frontImagePath)
            if (thumb != null) {
                binding.imageThumb.setImageBitmap(thumb)
            } else {
                binding.imageThumb.setImageResource(R.drawable.ic_id_placeholder)
            }

            binding.root.setOnClickListener { onClick(doc) }

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

    private fun labelFor(type: IdDocumentType): Int = when (type) {
        IdDocumentType.DRIVERS_LICENSE -> R.string.doc_type_drivers_license
        IdDocumentType.STATE_ID -> R.string.doc_type_state_id
        IdDocumentType.SOCIAL_SECURITY_CARD -> R.string.doc_type_ssn_card
        IdDocumentType.PASSPORT -> R.string.doc_type_passport
    }
}
