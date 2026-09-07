package app.orionmd.digitalwallet.ui.common

import android.view.View
import android.view.ViewGroup
import com.google.android.material.card.MaterialCardView

/**
 * Applies one of the three list view modes (see [ViewMode]) to a single row, so Tiles/Compact/List
 * look and behave consistently across every list in the app (Cards, IDs, Passwords, Contacts,
 * Sales) without needing a separate layout file per mode:
 *
 * - Tiles: the original look - a rounded, elevated card with generous margins and a full-size
 *   thumbnail.
 * - Compact: smaller margins/elevation/corner-radius and a smaller thumbnail, so more rows fit on
 *   screen while still reading as individual cards.
 * - List: a flat, edge-to-edge row with no elevation or rounding and a small thumbnail - closest to
 *   a plain system list.
 *
 * [card] is the row's outer MaterialCardView (every item_*.xml layout in the app already uses one
 * as its root). [thumb] is the row's thumbnail/photo/icon ImageView, or null for a row with none
 * (e.g. Sales). [thumbBaseSizeDp] is that thumbnail's normal (Tiles-mode) size in dp, since it
 * differs per list - Cards/IDs use 56dp, Contacts 48dp, Passwords 40dp.
 */
object RowStyler {

    fun apply(card: MaterialCardView, thumb: View?, thumbBaseSizeDp: Int, mode: ViewMode) {
        val density = card.resources.displayMetrics.density
        fun dp(value: Float) = value * density

        val elevationDp: Float
        val radiusDp: Float
        val marginDp: Float
        val thumbScale: Float
        when (mode) {
            ViewMode.TILES -> {
                elevationDp = 3f; radiusDp = 14f; marginDp = 12f; thumbScale = 1f
            }
            ViewMode.COMPACT -> {
                elevationDp = 1f; radiusDp = 10f; marginDp = 6f; thumbScale = 0.72f
            }
            ViewMode.LIST -> {
                elevationDp = 0f; radiusDp = 0f; marginDp = 0f; thumbScale = 0.55f
            }
        }

        card.cardElevation = dp(elevationDp)
        card.radius = dp(radiusDp)
        (card.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            lp.marginStart = dp(marginDp).toInt()
            lp.marginEnd = dp(marginDp).toInt()
            lp.topMargin = dp(marginDp / 2f).toInt()
            lp.bottomMargin = if (mode == ViewMode.LIST) dp(1f).toInt() else dp(marginDp / 2f).toInt()
            card.layoutParams = lp
        }

        thumb?.let {
            val size = dp(thumbBaseSizeDp * thumbScale).toInt()
            if (it.layoutParams.width != size || it.layoutParams.height != size) {
                it.layoutParams = it.layoutParams.apply { width = size; height = size }
            }
        }
    }
}
