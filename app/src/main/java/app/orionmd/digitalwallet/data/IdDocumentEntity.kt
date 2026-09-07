package app.orionmd.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The four document types offered under Wallet > IDs. */
enum class IdDocumentType {
    DRIVERS_LICENSE,
    STATE_ID,
    SOCIAL_SECURITY_CARD,
    PASSPORT
}

/**
 * Raw database row for a Wallet "IDs" entry (Driver's License, State ID, Social Security Card,
 * Passport). All free-text fields are stored as AES-GCM ciphertext; see [IdDocumentRepository].
 */
@Entity(tableName = "id_documents")
data class IdDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentType: IdDocumentType,
    val fullNameEnc: String,
    val documentNumberEnc: String,
    val issueDateEnc: String,
    val expirationDateEnc: String,
    val issuingRegionEnc: String,
    val notesEnc: String,
    val frontImagePath: String?,
    val backImagePath: String?,
    /** Manual display order for the IDs list (lower shows first) - set on insert to put new
     * documents at the end, then only ever changed by dragging to reorder. Deliberately
     * independent of updatedAt, which changes on every edit and would otherwise scramble a
     * manual order. */
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
