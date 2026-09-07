package app.orionmd.digitalwallet.data

import android.content.Context
import app.orionmd.digitalwallet.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SaleRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).saleDao()

    fun observeAll(): Flow<List<SaleItem>> = dao.getAll().map { list -> list.map { it.toItem() } }

    suspend fun getById(id: Long): SaleItem? = dao.getById(id)?.toItem()

    suspend fun save(item: SaleItem) {
        val now = System.currentTimeMillis()
        if (item.id == 0L) {
            dao.insert(item.toEntity(createdAt = now, updatedAt = now))
        } else {
            val createdAt = dao.getById(item.id)?.createdAt ?: now
            dao.update(item.toEntity(createdAt = createdAt, updatedAt = now))
        }
    }

    /** Overwrites an existing row in place without bumping updatedAt - used by
     * [app.orionmd.digitalwallet.security.KeyRotation] to re-encrypt a sale's fields under a new
     * key without it looking like the user just touched it. */
    suspend fun update(item: SaleItem) {
        dao.update(item.toEntity(createdAt = item.createdAt, updatedAt = item.updatedAt))
    }

    suspend fun delete(item: SaleItem) {
        dao.getById(item.id)?.let { dao.delete(it) }
    }

    private fun SaleEntity.toItem() = SaleItem(
        id = id,
        date = CryptoManager.decryptString(dateEnc),
        clientId = CryptoManager.decryptString(clientIdEnc),
        service = CryptoManager.decryptString(serviceEnc),
        price = CryptoManager.decryptString(priceEnc),
        commissionFee = CryptoManager.decryptString(commissionFeeEnc),
        partnerFee = CryptoManager.decryptString(partnerFeeEnc),
        status = status,
        amountPaid = CryptoManager.decryptString(amountPaidEnc),
        notes = CryptoManager.decryptString(notesEnc),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun SaleItem.toEntity(createdAt: Long, updatedAt: Long) = SaleEntity(
        id = id,
        dateEnc = CryptoManager.encryptString(date),
        clientIdEnc = CryptoManager.encryptString(clientId),
        serviceEnc = CryptoManager.encryptString(service),
        priceEnc = CryptoManager.encryptString(price),
        commissionFeeEnc = CryptoManager.encryptString(commissionFee),
        partnerFeeEnc = CryptoManager.encryptString(partnerFee),
        status = status,
        amountPaidEnc = CryptoManager.encryptString(amountPaid),
        notesEnc = CryptoManager.encryptString(notes),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
