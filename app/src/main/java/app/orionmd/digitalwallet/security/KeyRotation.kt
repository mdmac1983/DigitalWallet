package app.orionmd.digitalwallet.security

import android.content.Context
import app.orionmd.digitalwallet.data.CardRepository
import app.orionmd.digitalwallet.data.ContactRepository
import app.orionmd.digitalwallet.data.DocumentStore
import app.orionmd.digitalwallet.data.FinanceRepository
import app.orionmd.digitalwallet.data.HoldingRepository
import app.orionmd.digitalwallet.data.IdDocumentRepository
import app.orionmd.digitalwallet.data.ImageStore
import app.orionmd.digitalwallet.data.PasswordRepository
import app.orionmd.digitalwallet.data.SaleRepository
import app.orionmd.digitalwallet.data.StatementRepository
import kotlinx.coroutines.flow.first

/**
 * Changing the PIN also changes the AES key derived from it (see [PinManager]), so every
 * already-encrypted field and image has to be decrypted under the old key and re-encrypted
 * under the new one - otherwise changing the PIN would silently lock the user out of all their
 * own data. Call this from a background dispatcher; it does blocking file and database I/O.
 */
object KeyRotation {

    /** Returns true on success, false if [oldPin] does not match the currently-set PIN. */
    suspend fun changePin(context: Context, oldPin: String, newPin: String): Boolean {
        val oldKey = PinManager.deriveMasterKeyIfValid(context, oldPin) ?: return false
        CryptoManager.unlock(oldKey)

        val cardRepo = CardRepository(context)
        val idRepo = IdDocumentRepository(context)
        val passwordRepo = PasswordRepository(context)
        val contactRepo = ContactRepository(context)
        val financeRepo = FinanceRepository(context)
        val statementRepo = StatementRepository(context)
        val holdingRepo = HoldingRepository(context)
        val saleRepo = SaleRepository(context)

        // Phase 1: decrypt every row and every image's/document's bytes while the OLD key is
        // still active.
        val cards = cardRepo.observeAll().first()
        val ids = idRepo.observeAll().first()
        val passwords = passwordRepo.observeAll().first()
        val contacts = contactRepo.observeAll().first()
        val finances = financeRepo.observeAll().first()
        val statements = statementRepo.observeAll().first()
        val holdings = holdingRepo.observeAll().first()
        val sales = saleRepo.observeAll().first()

        val decryptedImages = HashMap<String, ByteArray>()
        fun stash(path: String?) {
            if (path.isNullOrEmpty() || decryptedImages.containsKey(path)) return
            ImageStore.loadDecryptedBytes(context, path)?.let { decryptedImages[path] = it }
        }
        cards.forEach { stash(it.frontImagePath); stash(it.backImagePath) }
        ids.forEach { stash(it.frontImagePath); stash(it.backImagePath) }
        contacts.forEach { stash(it.photoPath) }

        val decryptedDocuments = HashMap<String, ByteArray>()
        statements.forEach { statement ->
            val path = statement.sourceFileName
            if (!path.isNullOrEmpty() && !decryptedDocuments.containsKey(path)) {
                DocumentStore.loadDecryptedBytes(context, path)?.let { decryptedDocuments[path] = it }
            }
        }

        // Phase 2: commit the new PIN and switch the active key.
        val newKey = PinManager.changePin(context, newPin)
        CryptoManager.unlock(newKey)

        // Phase 3: re-encrypt everything under the new key, writing back to the same
        // filenames/row ids so nothing else in the app needs to change.
        decryptedImages.forEach { (path, bytes) -> ImageStore.saveAt(context, path, bytes) }
        decryptedDocuments.forEach { (path, bytes) -> DocumentStore.saveAt(context, path, bytes) }
        cards.forEach { cardRepo.save(it) }
        ids.forEach { idRepo.save(it) }
        passwords.forEach { passwordRepo.save(it) }
        contacts.forEach { contactRepo.save(it) }
        finances.forEach { financeRepo.save(it) }
        statements.forEach { statementRepo.update(it) }
        holdings.forEach { holdingRepo.update(it) }
        sales.forEach { saleRepo.update(it) }

        return true
    }
}
