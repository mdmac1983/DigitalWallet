package app.orionmd.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Raw database row for a Wallet "Cards" entry. Every text field below other than the id,
 * timestamps and image filenames is stored as AES-GCM ciphertext (see [CryptoManager]) - this
 * class only models the table shape, never plaintext. Encryption/decryption happens in
 * [CardRepository], which is the only thing UI code should talk to.
 */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issuerEnc: String,
    val cardNameEnc: String,
    val cardholderNameEnc: String,
    val cardNumberEnc: String,
    val expDateEnc: String,
    val cvvEnc: String,
    val websitePortalEnc: String,
    val usernameEnc: String,
    val passwordEnc: String,
    val frontImagePath: String?,
    val backImagePath: String?,
    val creditLimitEnc: String = "",
    val billingAddressEnc: String = "",
    val billingCityEnc: String = "",
    val billingStateEnc: String = "",
    val billingZipEnc: String = "",
    /** Manual display order for the Cards list (lower shows first) - set on insert to put new
     * cards at the end, then only ever changed by dragging to reorder. Deliberately independent
     * of updatedAt, which changes on every edit and would otherwise scramble a manual order. */
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
