package app.orionmd.digitalwallet.ui.common

import android.content.Context
import app.orionmd.digitalwallet.R

/** How a list of entries (Cards, IDs, Passwords, Contacts, Sales) is displayed. Persisted
 * independently per screen via [ViewModePrefs] so changing one list's view doesn't affect the
 * others. */
enum class ViewMode {
    TILES, COMPACT, LIST;

    fun label(context: Context): String = when (this) {
        TILES -> context.getString(R.string.view_mode_tiles)
        COMPACT -> context.getString(R.string.view_mode_compact)
        LIST -> context.getString(R.string.view_mode_list)
    }
}

object ViewModePrefs {
    private const val PREFS_NAME = "view_mode_prefs"

    // One key per screen so each remembers its own view mode independently.
    const val KEY_CARDS = "cards"
    const val KEY_IDS = "ids"
    const val KEY_PASSWORDS = "passwords"
    const val KEY_CONTACTS = "contacts"
    const val KEY_SALES = "sales"

    fun get(context: Context, key: String): ViewMode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
        return raw?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.TILES
    }

    fun set(context: Context, key: String, mode: ViewMode) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, mode.name)
            .apply()
    }
}
