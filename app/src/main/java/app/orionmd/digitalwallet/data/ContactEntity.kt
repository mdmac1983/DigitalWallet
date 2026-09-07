package app.orionmd.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Categories shown in the Contacts tab - lets contacts be sorted "by type" the same way
 * Passwords already can be. */
enum class ContactCategory {
    FAMILY,
    FRIEND,
    WORK,
    BUSINESS,
    OTHER
}

/**
 * Raw database row for a Contacts-tab entry. All free-text fields are stored as AES-GCM
 * ciphertext; see [ContactRepository].
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEnc: String,
    val phoneEnc: String,
    val emailEnc: String,
    val addressEnc: String,
    val companyEnc: String,
    val notesEnc: String,
    val photoPath: String?,
    val category: ContactCategory = ContactCategory.OTHER,
    /** Manual display order (lower shows first) - see [CardEntity.sortOrder] for why this is
     * independent of updatedAt. Only meaningful when the Contacts screen's sort mode is Manual. */
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
