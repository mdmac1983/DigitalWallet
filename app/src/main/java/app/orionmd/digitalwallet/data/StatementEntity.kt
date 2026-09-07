package app.orionmd.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StatementType {
    BANK,
    CREDIT_CARD,
    INVESTMENT
}

/**
 * Raw database row for one uploaded monthly statement (a bank/credit-card/investment PDF or a
 * photographed paper statement) that fed transactions into the Finances tab. The original file
 * itself is kept encrypted on disk via [DocumentStore] under [sourceFileName] - this row is just
 * its metadata. See [StatementRepository] for the encrypt/decrypt boundary.
 */
@Entity(tableName = "statements")
data class StatementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: StatementType,
    val institutionEnc: String,
    val statementMonthEnc: String,
    val sourceFileName: String?,
    val sourceDisplayNameEnc: String,
    val sourceMimeType: String?,
    val createdAt: Long,
    /** What the statement itself says its balance was at the start/end of the period - typed in
     * by the user off their own statement, not computed from parsed transactions. Empty string
     * ("" round-trips through [app.orionmd.digitalwallet.security.CryptoManager] as "") means not
     * provided, since these are optional. */
    val beginningBalanceEnc: String = "",
    val endingBalanceEnc: String = ""
)
