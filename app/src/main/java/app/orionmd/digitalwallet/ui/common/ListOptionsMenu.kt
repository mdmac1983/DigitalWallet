package app.orionmd.digitalwallet.ui.common

import android.view.View
import androidx.appcompat.widget.PopupMenu

/** Small popup menus for choosing a [ViewMode] or [SortMode] from a toolbar icon button. */
object ListOptionsMenu {

    fun showViewMode(anchor: View, current: ViewMode, onSelected: (ViewMode) -> Unit) {
        val popup = PopupMenu(anchor.context, anchor)
        ViewMode.values().forEachIndexed { index, mode ->
            popup.menu.add(0, index, index, mode.label(anchor.context))
        }
        popup.menu.setGroupCheckable(0, true, true)
        popup.menu.getItem(current.ordinal).isChecked = true
        popup.setOnMenuItemClickListener { item ->
            onSelected(ViewMode.values()[item.itemId])
            true
        }
        popup.show()
    }

    fun showSortMode(anchor: View, current: SortMode, onSelected: (SortMode) -> Unit) {
        val popup = PopupMenu(anchor.context, anchor)
        SortMode.values().forEachIndexed { index, mode ->
            popup.menu.add(0, index, index, mode.label(anchor.context))
        }
        popup.menu.setGroupCheckable(0, true, true)
        popup.menu.getItem(current.ordinal).isChecked = true
        popup.setOnMenuItemClickListener { item ->
            onSelected(SortMode.values()[item.itemId])
            true
        }
        popup.show()
    }
}
