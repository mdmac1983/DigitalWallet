package app.orionmd.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SaleStatus {
    UNPAID,
    PARTIAL,
    PAID
}

/**
 * Raw database row for a Sales-tab entry (a service sold to a client). Net fee isn't stored - it's
 * always price - commissionFee - partnerFee, computed from the current values (see
 * [netFee]/[SaleRepository]) so it can never drift out of sync with its own inputs. `status`
 * isn't sensitive like the free-text fields, so it's a plain column rather than *Enc; the actual
 * amount paid (only meaningful for PARTIAL) is free text a person could recognize, so it is
 * encrypted like the rest.
 */
@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEnc: String,
    val clientIdEnc: String,
    val serviceEnc: String,
    val priceEnc: String,
    val commissionFeeEnc: String,
    val partnerFeeEnc: String,
    val status: SaleStatus,
    val amountPaidEnc: String = "",
    val notesEnc: String,
    val createdAt: Long,
    val updatedAt: Long
)
