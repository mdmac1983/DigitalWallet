package app.orionmd.digitalwallet.ui.common

import android.content.Context
import app.orionmd.digitalwallet.R

/** How Passwords and Contacts can be ordered.
 *
 * ALPHABETICAL and TYPE are computed after decryption, client-side (the fields they sort by -
 * account name / contact name / category - are stored as AES-GCM ciphertext, so SQL can't order
 * by their real value). MANUAL uses the persisted `sortOrder` column and is the only mode where
 * dragging a row to reorder it does anything - see each screen's ItemTouchHelper. */
enum class SortMode {
    ALPHABETICAL, TYPE, MANUAL;

    fun label(context: Context): String = when (this) {
        ALPHABETICAL -> context.getString(R.string.sort_mode_alphabetical)
        TYPE -> context.getString(R.string.sort_mode_type)
        MANUAL -> context.getString(R.string.sort_mode_manual)
    }
}

object SortModePrefs {
    private const val PREFS_NAME = "sort_mode_prefs"

    const val KEY_PASSWORDS = "passwords"
    const val KEY_CONTACTS = "contacts"

    /** Defaults to Manual - right after upgrading, a screen's manual order is backfilled to match
     * its old most-recently-updated-first order (see AppDatabase's MIGRATION_9_10), so nothing
     * appears to change until the user actually switches sort mode or drags something. */
    fun get(context: Context, key: String): SortMode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
        return raw?.let { runCatching { SortMode.valueOf(it) }.getOrNull() } ?: SortMode.MANUAL
    }

    fun set(context: Context, key: String, mode: SortMode) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, mode.name)
            .apply()
    }
}
