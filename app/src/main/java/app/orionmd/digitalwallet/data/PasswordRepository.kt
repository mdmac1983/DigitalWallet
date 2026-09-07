package app.orionmd.digitalwallet.data

import android.content.Context
import app.orionmd.digitalwallet.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PasswordRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).passwordDao()

    fun observeAll(): Flow<List<PasswordItem>> = dao.getAll().map { list -> list.map { it.toItem() } }

    suspend fun getById(id: Long): PasswordItem? = dao.getById(id)?.toItem()

    suspend fun save(item: PasswordItem) {
        val now = System.currentTimeMillis()
        if (item.id == 0L) {
            val nextOrder = (dao.maxSortOrder() ?: -1) + 1
            dao.insert(item.toEntity(createdAt = now, updatedAt = now, sortOrder = nextOrder))
        } else {
            val existing = dao.getById(item.id)
            dao.update(
                item.toEntity(
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    sortOrder = existing?.sortOrder ?: 0
                )
            )
        }
    }

    suspend fun delete(item: PasswordItem) {
        dao.getById(item.id)?.let { dao.delete(it) }
    }

    /** Persists a new manual display order after the user drags entries into a new arrangement
     * (only relevant while the Passwords screen's sort mode is Manual). */
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.updateSortOrder(id, index) }
    }

    private fun PasswordEntity.toItem() = PasswordItem(
        id = id,
        category = category,
        accountName = CryptoManager.decryptString(accountNameEnc),
        username = CryptoManager.decryptString(usernameEnc),
        password = CryptoManager.decryptString(passwordEnc),
        url = CryptoManager.decryptString(urlEnc),
        notes = CryptoManager.decryptString(notesEnc)
    )

    private fun PasswordItem.toEntity(createdAt: Long, updatedAt: Long, sortOrder: Int) = PasswordEntity(
        id = id,
        category = category,
        accountNameEnc = CryptoManager.encryptString(accountName),
        usernameEnc = CryptoManager.encryptString(username),
        passwordEnc = CryptoManager.encryptString(password),
        urlEnc = CryptoManager.encryptString(url),
        notesEnc = CryptoManager.encryptString(notes),
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
