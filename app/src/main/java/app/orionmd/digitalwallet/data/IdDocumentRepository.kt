package app.orionmd.digitalwallet.data

import android.content.Context
import app.orionmd.digitalwallet.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IdDocumentRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = AppDatabase.getInstance(context).idDocumentDao()

    fun observeAll(): Flow<List<IdDocumentItem>> = dao.getAll().map { list -> list.map { it.toItem() } }

    suspend fun getById(id: Long): IdDocumentItem? = dao.getById(id)?.toItem()

    suspend fun save(item: IdDocumentItem) {
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

    suspend fun delete(item: IdDocumentItem) {
        ImageStore.delete(appContext, item.frontImagePath)
        ImageStore.delete(appContext, item.backImagePath)
        dao.getById(item.id)?.let { dao.delete(it) }
    }

    /** Persists a new manual display order after the user drags documents into a new arrangement -
     * [orderedIds] is every document's id in its new top-to-bottom order. */
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.updateSortOrder(id, index) }
    }

    private fun IdDocumentEntity.toItem() = IdDocumentItem(
        id = id,
        documentType = documentType,
        fullName = CryptoManager.decryptString(fullNameEnc),
        documentNumber = CryptoManager.decryptString(documentNumberEnc),
        issueDate = CryptoManager.decryptString(issueDateEnc),
        expirationDate = CryptoManager.decryptString(expirationDateEnc),
        issuingRegion = CryptoManager.decryptString(issuingRegionEnc),
        notes = CryptoManager.decryptString(notesEnc),
        frontImagePath = frontImagePath,
        backImagePath = backImagePath
    )

    private fun IdDocumentItem.toEntity(createdAt: Long, updatedAt: Long, sortOrder: Int) = IdDocumentEntity(
        id = id,
        documentType = documentType,
        fullNameEnc = CryptoManager.encryptString(fullName),
        documentNumberEnc = CryptoManager.encryptString(documentNumber),
        issueDateEnc = CryptoManager.encryptString(issueDate),
        expirationDateEnc = CryptoManager.encryptString(expirationDate),
        issuingRegionEnc = CryptoManager.encryptString(issuingRegion),
        notesEnc = CryptoManager.encryptString(notes),
        frontImagePath = frontImagePath,
        backImagePath = backImagePath,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
