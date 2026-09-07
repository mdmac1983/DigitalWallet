package app.orionmd.digitalwallet.ui.passwords

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.PasswordCategory
import app.orionmd.digitalwallet.data.PasswordItem
import app.orionmd.digitalwallet.databinding.ItemPasswordBinding
import app.orionmd.digitalwallet.ui.common.RowStyler
import app.orionmd.digitalwallet.ui.common.ViewMode

class PasswordAdapter(
    private val context: Context,
    private val onClick: (PasswordItem) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit = {}
) : RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {

    private var items: List<PasswordItem> = emptyList()
    private var viewMode: ViewMode = ViewMode.TILES
    /** Whether drag handles should show/respond - only true while sort mode is Manual. */
    private var dragEnabled: Boolean = true

    fun submitList(newItems: List<PasswordItem>) {
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
    fun currentItems(): List<PasswordItem> = items

    /** Reflects one step of a drag: swaps [fromPosition] and [toPosition] in the in-memory list
     * and animates the move. Nothing is persisted here - see [currentItems]. */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        items = items.toMutableList().apply { add(toPosition, removeAt(fromPosition)) }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPasswordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemPasswordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: PasswordItem) {
            binding.textAccountName.text = entry.accountName
            binding.textUsername.text = entry.username
            binding.textCategory.text = context.getString(labelFor(entry.category))
            binding.root.setOnClickListener { onClick(entry) }

            RowStyler.apply(binding.root, binding.imageIcon, 40, viewMode)

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

    private fun labelFor(category: PasswordCategory): Int = when (category) {
        PasswordCategory.EMAIL -> R.string.category_email
        PasswordCategory.SOCIAL_MEDIA -> R.string.category_social_media
        PasswordCategory.WEBSITE -> R.string.category_website
        PasswordCategory.BANKING -> R.string.category_banking
        PasswordCategory.INVESTMENT -> R.string.category_investment
        PasswordCategory.OTHER -> R.string.category_other
    }
}
