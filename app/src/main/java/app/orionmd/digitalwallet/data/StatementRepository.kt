package app.orionmd.digitalwallet.data

import android.content.Context
import app.orionmd.digitalwallet.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StatementRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).statementDao()

    fun observeAll(): Flow<List<StatementItem>> = dao.getAll().map { list -> list.map { it.toItem() } }

    suspend fun getById(id: Long): StatementItem? = dao.getById(id)?.toItem()

    /** Returns the new row's id so the caller can tag imported [FinanceItem]s with it. */
    suspend fun insert(item: StatementItem): Long =
        dao.insert(item.toEntity(createdAt = System.currentTimeMillis()))

    /** Overwrites an existing row in place (same id, same original createdAt) - used by
     * [app.orionmd.digitalwallet.security.KeyRotation] to re-encrypt a statement's fields under
     * a new key without disturbing anything that points at its id. */
    suspend fun update(item: StatementItem) {
        dao.update(item.toEntity(createdAt = item.createdAt))
    }

    suspend fun delete(item: StatementItem) {
        dao.getById(item.id)?.let { dao.delete(it) }
    }

    private fun StatementEntity.toItem() = StatementItem(
        id = id,
        type = type,
        institution = CryptoManager.decryptString(institutionEnc),
        statementMonth = CryptoManager.decryptString(statementMonthEnc),
        sourceFileName = sourceFileName,
        sourceDisplayName = CryptoManager.decryptString(sourceDisplayNameEnc),
        sourceMimeType = sourceMimeType,
        createdAt = createdAt,
        beginningBalance = CryptoManager.decryptString(beginningBalanceEnc),
        endingBalance = CryptoManager.decryptString(endingBalanceEnc)
    )

    private fun StatementItem.toEntity(createdAt: Long) = StatementEntity(
        id = id,
        type = type,
        institutionEnc = CryptoManager.encryptString(institution),
        statementMonthEnc = CryptoManager.encryptString(statementMonth),
        sourceFileName = sourceFileName,
        sourceDisplayNameEnc = CryptoManager.encryptString(sourceDisplayName),
        sourceMimeType = sourceMimeType,
        createdAt = createdAt,
        beginningBalanceEnc = CryptoManager.encryptString(beginningBalance),
        endingBalanceEnc = CryptoManager.encryptString(endingBalance)
    )
}
