package app.orionmd.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HoldingType {
    STOCK,
    CRYPTO,
    /** Uninvested cash sitting in a brokerage, ready to deploy. Represented internally as
     * quantity=1 at currentPrice=<the cash amount>, so [totalValue] keeps working unmodified;
     * costBasis is always blank for cash, so [gainLoss] is never tracked for it. */
    CASH
}

/**
 * Raw database row for one manually-entered stock/crypto holding. There's no live price lookup
 * (see [HoldingRepository]/[HoldingItem]) - the user types in quantity and current price/value
 * themselves and updates it whenever they check in, so this stays fully offline like the rest of
 * the app. costBasisEnc is optional (blank if the user doesn't want to track gain/loss).
 */
@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdingType: HoldingType,
    val symbolEnc: String,
    val nameEnc: String,
    val quantityEnc: String,
    val costBasisEnc: String,
    val currentPriceEnc: String,
    val notesEnc: String,
    val createdAt: Long,
    val updatedAt: Long
)
