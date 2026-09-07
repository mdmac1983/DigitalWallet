package app.orionmd.digitalwallet.data

import android.content.Context
import app.orionmd.digitalwallet.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Only place in the app that translates between plaintext [CardItem]s and encrypted
 * [CardEntity] rows. UI code should never touch [CardDao] directly.
 */
class CardRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = AppDatabase.getInstance(context).cardDao()

    fun observeAll(): Flow<List<CardItem>> = dao.getAll().map { list -> list.map { it.toItem() } }

    suspend fun getById(id: Long): CardItem? = dao.getById(id)?.toItem()

    suspend fun save(item: CardItem) {
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

    suspend fun delete(item: CardItem) {
        ImageStore.delete(appContext, item.frontImagePath)
        ImageStore.delete(appContext, item.backImagePath)
        dao.getById(item.id)?.let { dao.delete(it) }
    }

    /** Persists a new manual display order after the user drags cards into a new arrangement -
     * [orderedIds] is every card's id in its new top-to-bottom order. */
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.updateSortOrder(id, index) }
    }

    private fun CardEntity.toItem() = CardItem(
        id = id,
        issuer = CryptoManager.decryptString(issuerEnc),
        cardName = CryptoManager.decryptString(cardNameEnc),
        cardholderName = CryptoManager.decryptString(cardholderNameEnc),
        cardNumber = CryptoManager.decryptString(cardNumberEnc),
        expDate = CryptoManager.decryptString(expDateEnc),
        cvv = CryptoManager.decryptString(cvvEnc),
        websitePortal = CryptoManager.decryptString(websitePortalEnc),
        username = CryptoManager.decryptString(usernameEnc),
        password = CryptoManager.decryptString(passwordEnc),
        frontImagePath = frontImagePath,
        backImagePath = backImagePath,
        creditLimit = CryptoManager.decryptString(creditLimitEnc),
        billingAddress = CryptoManager.decryptString(billingAddressEnc),
        billingCity = CryptoManager.decryptString(billingCityEnc),
        billingState = CryptoManager.decryptString(billingStateEnc),
        billingZip = CryptoManager.decryptString(billingZipEnc)
    )

    private fun CardItem.toEntity(createdAt: Long, updatedAt: Long, sortOrder: Int) = CardEntity(
        id = id,
        issuerEnc = CryptoManager.encryptString(issuer),
        cardNameEnc = CryptoManager.encryptString(cardName),
        cardholderNameEnc = CryptoManager.encryptString(cardholderName),
        cardNumberEnc = CryptoManager.encryptString(cardNumber),
        expDateEnc = CryptoManager.encryptString(expDate),
        cvvEnc = CryptoManager.encryptString(cvv),
        websitePortalEnc = CryptoManager.encryptString(websitePortal),
        usernameEnc = CryptoManager.encryptString(username),
        passwordEnc = CryptoManager.encryptString(password),
        frontImagePath = frontImagePath,
        backImagePath = backImagePath,
        creditLimitEnc = CryptoManager.encryptString(creditLimit),
        billingAddressEnc = CryptoManager.encryptString(billingAddress),
        billingCityEnc = CryptoManager.encryptString(billingCity),
        billingStateEnc = CryptoManager.encryptString(billingState),
        billingZipEnc = CryptoManager.encryptString(billingZip),
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
