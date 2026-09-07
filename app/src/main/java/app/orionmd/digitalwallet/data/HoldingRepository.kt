package app.orionmd.digitalwallet.data

import android.content.Context
import app.orionmd.digitalwallet.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HoldingRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).holdingDao()

    fun observeAll(): Flow<List<HoldingItem>> = dao.getAll().map { list -> list.map { it.toItem() } }

    suspend fun getById(id: Long): HoldingItem? = dao.getById(id)?.toItem()

    suspend fun save(item: HoldingItem) {
        val now = System.currentTimeMillis()
        if (item.id == 0L) {
            dao.insert(item.toEntity(createdAt = now, updatedAt = now))
        } else {
            val createdAt = dao.getById(item.id)?.createdAt ?: now
            dao.update(item.toEntity(createdAt = createdAt, updatedAt = now))
        }
    }

    /** Overwrites an existing row in place without bumping updatedAt - used by
     * [app.orionmd.digitalwallet.security.KeyRotation] to re-encrypt a holding's fields under a
     * new key without it looking like the user just touched it. */
    suspend fun update(item: HoldingItem) {
        dao.update(item.toEntity(createdAt = item.createdAt, updatedAt = item.updatedAt))
    }

    suspend fun delete(item: HoldingItem) {
        dao.getById(item.id)?.let { dao.delete(it) }
    }

    private fun HoldingEntity.toItem() = HoldingItem(
        id = id,
        holdingType = holdingType,
        symbol = CryptoManager.decryptString(symbolEnc),
        name = CryptoManager.decryptString(nameEnc),
        quantity = CryptoManager.decryptString(quantityEnc),
        costBasis = CryptoManager.decryptString(costBasisEnc),
        currentPrice = CryptoManager.decryptString(currentPriceEnc),
        notes = CryptoManager.decryptString(notesEnc),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun HoldingItem.toEntity(createdAt: Long, updatedAt: Long) = HoldingEntity(
        id = id,
        holdingType = holdingType,
        symbolEnc = CryptoManager.encryptString(symbol),
        nameEnc = CryptoManager.encryptString(name),
        quantityEnc = CryptoManager.encryptString(quantity),
        costBasisEnc = CryptoManager.encryptString(costBasis),
        currentPriceEnc = CryptoManager.encryptString(currentPrice),
        notesEnc = CryptoManager.encryptString(notes),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
