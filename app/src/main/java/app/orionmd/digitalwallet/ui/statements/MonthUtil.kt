package app.orionmd.digitalwallet.ui.statements

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Statement months are stored as a sortable "yyyy-MM" key and shown as "September 2026". */
object MonthUtil {

    private fun keyFormat() = SimpleDateFormat("yyyy-MM", Locale.US)
    private fun labelFormat() = SimpleDateFormat("MMMM yyyy", Locale.US)

    fun currentKey(): String = keyFormat().format(Date())

    fun label(key: String): String = runCatching {
        keyFormat().parse(key)?.let { labelFormat().format(it) }
    }.getOrNull() ?: key

    /** Most-recent-first (key, label) pairs for the last [months] months, current month first. */
    fun recentMonths(months: Int = 24): List<Pair<String, String>> {
        val calendar = Calendar.getInstance()
        return (0 until months).map {
            val key = keyFormat().format(calendar.time)
            val label = labelFormat().format(calendar.time)
            calendar.add(Calendar.MONTH, -1)
            key to label
        }
    }

    /** Best-effort attempt to read a "yyyy-MM" key out of a free-text date string a user typed
     * into a manual Finances entry (several common formats tried in turn). Returns null if none
     * match - that entry just won't be groupable by month in the consolidated PDF. */
    fun keyFromFreeText(dateText: String): String? {
        if (dateText.isBlank()) return null
        val patterns = listOf(
            "M/d/yyyy", "M/d/yy", "yyyy-MM-dd", "MMM d, yyyy", "MMMM d, yyyy", "MMM d yyyy", "MMMM d yyyy"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(dateText.trim())
            }.getOrNull()
            if (parsed != null) return keyFormat().format(parsed)
        }
        return null
    }
}
