package app.orionmd.digitalwallet.ui.statements

import app.orionmd.digitalwallet.data.FinanceType

/**
 * A best-effort guess at one transaction line found in an uploaded statement. Nothing derived
 * from these ever reaches the database directly - [ReviewParsedActivity] always shows them to
 * the user for confirmation/editing first.
 */
data class ParsedTransaction(
    var include: Boolean = true,
    var type: FinanceType,
    var amount: String,
    var date: String,
    var description: String,
    var category: String = ""
)

/**
 * Hands a just-parsed candidate list from [StatementImportActivity] to [ReviewParsedActivity]
 * without the ceremony of Parcelable - both run in the same process, and this data never needs
 * to survive a process death (if the process dies, re-importing the file is cheap).
 */
object ParsedTransactionBuffer {
    var pending: List<ParsedTransaction> = emptyList()
    /** Row id of the [app.orionmd.digitalwallet.data.StatementItem] these came from, so accepted
     * entries can be tagged with it for provenance/the monthly PDF's detail section. */
    var statementId: Long = -1L
    /** Pre-fills each accepted entry's "source" field (e.g. the institution name picked on import). */
    var defaultSource: String = ""
}
