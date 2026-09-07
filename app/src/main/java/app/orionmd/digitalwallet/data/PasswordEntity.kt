package app.orionmd.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Categories shown in the Passwords tab. */
enum class PasswordCategory {
    EMAIL,
    SOCIAL_MEDIA,
    WEBSITE,
    BANKING,
    INVESTMENT,
    OTHER
}

/**
 * Raw database row for a Passwords-tab entry. All free-text fields are stored as AES-GCM
 * ciphertext; see [PasswordRepository].
 */
@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: PasswordCategory,
    val accountNameEnc: String,
    val usernameEnc: String,
    val passwordEnc: String,
    val urlEnc: String,
    val notesEnc: String,
    /** Manual display order (lower shows first) - see [CardEntity.sortOrder] for why this is
     * independent of updatedAt. Only meaningful when the Passwords screen's sort mode is Manual. */
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
