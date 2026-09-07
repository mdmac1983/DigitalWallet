package app.orionmd.digitalwallet.data

import android.content.Context
import app.orionmd.digitalwallet.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).financeDao()

    fun observeAll(): Flow<List<FinanceItem>> = dao.getAll().map { list -> list.map { it.toItem() } }

    suspend fun getById(id: Long): FinanceItem? = dao.getById(id)?.toItem()

    suspend fun save(item: FinanceItem) {
        val now = System.currentTimeMillis()
        if (item.id == 0L) {
            dao.insert(item.toEntity(createdAt = now, updatedAt = now))
        } else {
            val existing = dao.getById(item.id)
            dao.update(item.toEntity(createdAt = existing?.createdAt ?: now, updatedAt = now))
        }
    }

    suspend fun delete(item: FinanceItem) {
        dao.getById(item.id)?.let { dao.delete(it) }
    }

    private fun FinanceEntity.toItem() = FinanceItem(
        id = id,
        type = type,
        amount = CryptoManager.decryptString(amountEnc),
        date = CryptoManager.decryptString(dateEnc),
        category = CryptoManager.decryptString(categoryEnc),
        source = CryptoManager.decryptString(sourceEnc),
        notes = CryptoManager.decryptString(notesEnc),
        typeLabel = CryptoManager.decryptString(typeLabelEnc),
        statementId = statementId
    )

    private fun FinanceItem.toEntity(createdAt: Long, updatedAt: Long) = FinanceEntity(
        id = id,
        type = type,
        amountEnc = CryptoManager.encryptString(amount),
        dateEnc = CryptoManager.encryptString(date),
        categoryEnc = CryptoManager.encryptString(category),
        sourceEnc = CryptoManager.encryptString(source),
        notesEnc = CryptoManager.encryptString(notes),
        typeLabelEnc = CryptoManager.encryptString(typeLabel),
        createdAt = createdAt,
        updatedAt = updatedAt,
        statementId = statementId
    )
}
