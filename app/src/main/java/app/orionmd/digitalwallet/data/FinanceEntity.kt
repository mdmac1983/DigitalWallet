package app.orionmd.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FinanceType {
    INCOME,
    EXPENSE
}

/**
 * Raw database row for a Finances-tab entry. All free-text fields are stored as AES-GCM
 * ciphertext; see [FinanceRepository].
 */
@Entity(tableName = "finances")
data class FinanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: FinanceType,
    val amountEnc: String,
    val dateEnc: String,
    val categoryEnc: String,
    val sourceEnc: String,
    val notesEnc: String,
    /** Optional override for the word shown instead of "Income"/"Expense" - e.g. "Deposit",
     * "Withdrawal", "Debit", "Credit" - for accounts where those terms fit better. Blank means
     * fall back to the default Income/Expense wording. The [type] itself always keeps deciding
     * the sign and color, so nothing about the math changes based on this label. */
    val typeLabelEnc: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    /** Non-null when this entry was created by importing a statement (see [StatementEntity]);
     * null for manually-entered rows. Not a foreign key constraint - deleting a statement leaves
     * its already-imported entries in place, just with a dangling id. */
    val statementId: Long? = null
)
