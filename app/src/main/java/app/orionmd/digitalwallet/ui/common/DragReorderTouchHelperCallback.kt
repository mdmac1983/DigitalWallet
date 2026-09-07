package app.orionmd.digitalwallet.ui.common

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * Shared drag-to-reorder wiring for the Cards and IDs lists. Dragging is only ever started
 * explicitly (see each adapter's drag-handle touch listener, which calls
 * [androidx.recyclerview.widget.ItemTouchHelper.startDrag]) rather than via long-press anywhere
 * on the row, so it never conflicts with tapping a row to open it for editing.
 *
 * [onMove] is called for every position swap while a drag is in progress and should just update
 * the adapter's in-memory list (notifyItemMoved) - nothing is persisted yet. [onDragFinished] is
 * called once the user lifts their finger, and is where the caller should write the new order to
 * the database.
 */
class DragReorderTouchHelperCallback(
    private val onMove: (fromPosition: Int, toPosition: Int) -> Unit,
    private val onDragFinished: () -> Unit
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    override fun isLongPressDragEnabled(): Boolean = false

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        onMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Swiping is disabled above; nothing to do.
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onDragFinished()
    }
}
