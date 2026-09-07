package app.orionmd.digitalwallet.ui.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.ContactCategory
import app.orionmd.digitalwallet.data.ContactItem
import app.orionmd.digitalwallet.data.ImageStore
import app.orionmd.digitalwallet.databinding.ItemContactBinding
import app.orionmd.digitalwallet.ui.common.RowStyler
import app.orionmd.digitalwallet.ui.common.ViewMode

class ContactAdapter(
    private val context: Context,
    private val onClick: (ContactItem) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit = {}
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private var items: List<ContactItem> = emptyList()
    private var viewMode: ViewMode = ViewMode.TILES
    /** Whether drag handles should show/respond - only true while sort mode is Manual. */
    private var dragEnabled: Boolean = true

    fun submitList(newItems: List<ContactItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setViewMode(mode: ViewMode) {
        viewMode = mode
        notifyDataSetChanged()
    }

    fun setDragEnabled(enabled: Boolean) {
        dragEnabled = enabled
        notifyDataSetChanged()
    }

    /** The list as currently ordered on screen, including any in-progress drag - used once a drag
     * finishes to persist the new order. */
    fun currentItems(): List<ContactItem> = items

    /** Reflects one step of a drag: swaps [fromPosition] and [toPosition] in the in-memory list
     * and animates the move. Nothing is persisted here - see [currentItems]. */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        items = items.toMutableList().apply { add(toPosition, removeAt(fromPosition)) }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: ContactItem) {
            binding.textName.text = contact.name
            binding.textSubtitle.text = contact.phone.ifBlank { contact.email }
            binding.textCategory.text = context.getString(labelFor(contact.category))

            val photo = ImageStore.loadBitmap(context, contact.photoPath)
            if (photo != null) {
                binding.imagePhoto.setImageBitmap(photo)
            } else {
                binding.imagePhoto.setImageResource(R.drawable.ic_contact_placeholder)
            }

            binding.root.setOnClickListener { onClick(contact) }

            RowStyler.apply(binding.root, binding.imagePhoto, 48, viewMode)

            binding.dragHandle.visibility = if (dragEnabled) View.VISIBLE else View.GONE
            @Suppress("ClickableViewAccessibility")
            binding.dragHandle.setOnTouchListener { _, event ->
                if (dragEnabled && event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }

    private fun labelFor(category: ContactCategory): Int = when (category) {
        ContactCategory.FAMILY -> R.string.category_family
        ContactCategory.FRIEND -> R.string.category_friend
        ContactCategory.WORK -> R.string.category_work
        ContactCategory.BUSINESS -> R.string.category_business
        ContactCategory.OTHER -> R.string.category_other
    }
}
