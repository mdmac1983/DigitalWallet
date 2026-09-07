package app.orionmd.digitalwallet.data

import android.content.Context
import app.orionmd.digitalwallet.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = AppDatabase.getInstance(context).contactDao()

    fun observeAll(): Flow<List<ContactItem>> = dao.getAll().map { list -> list.map { it.toItem() } }

    suspend fun getById(id: Long): ContactItem? = dao.getById(id)?.toItem()

    suspend fun save(item: ContactItem) {
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

    suspend fun delete(item: ContactItem) {
        ImageStore.delete(appContext, item.photoPath)
        dao.getById(item.id)?.let { dao.delete(it) }
    }

    /** Persists a new manual display order after the user drags entries into a new arrangement
     * (only relevant while the Contacts screen's sort mode is Manual). */
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.updateSortOrder(id, index) }
    }

    private fun ContactEntity.toItem() = ContactItem(
        id = id,
        name = CryptoManager.decryptString(nameEnc),
        phone = CryptoManager.decryptString(phoneEnc),
        email = CryptoManager.decryptString(emailEnc),
        address = CryptoManager.decryptString(addressEnc),
        company = CryptoManager.decryptString(companyEnc),
        notes = CryptoManager.decryptString(notesEnc),
        photoPath = photoPath,
        category = category
    )

    private fun ContactItem.toEntity(createdAt: Long, updatedAt: Long, sortOrder: Int) = ContactEntity(
        id = id,
        nameEnc = CryptoManager.encryptString(name),
        phoneEnc = CryptoManager.encryptString(phone),
        emailEnc = CryptoManager.encryptString(email),
        addressEnc = CryptoManager.encryptString(address),
        companyEnc = CryptoManager.encryptString(company),
        notesEnc = CryptoManager.encryptString(notes),
        photoPath = photoPath,
        category = category,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
