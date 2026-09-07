package app.orionmd.digitalwallet.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.orionmd.digitalwallet.security.CryptoManager
import java.io.File
import java.util.UUID

/**
 * Every uploaded card/ID/contact photo is encrypted on disk under the app's private storage
 * (never external storage, never a content-provider-visible location) using the same session
 * key as the database fields. Files are named by random UUID; the filename is the only thing
 * stored in plaintext in the database, since it carries no information about the image itself.
 */
object ImageStore {

    private fun imagesDir(context: Context): File =
        File(context.filesDir, "images").apply { if (!exists()) mkdirs() }

    /** Encrypts [rawBytes] (e.g. a JPEG from the camera or gallery) and returns its filename. */
    fun save(context: Context, rawBytes: ByteArray): String {
        val filename = "${UUID.randomUUID()}.enc"
        CryptoManager.encryptFile(rawBytes, File(imagesDir(context), filename))
        return filename
    }

    /** Decrypts the image stored under [filename] and decodes it to a [Bitmap], or null if missing. */
    fun loadBitmap(context: Context, filename: String?): Bitmap? {
        val bytes = loadDecryptedBytes(context, filename) ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /** Decrypts the image stored under [filename] and returns its raw bytes, or null if missing. */
    fun loadDecryptedBytes(context: Context, filename: String?): ByteArray? {
        if (filename.isNullOrEmpty()) return null
        val file = File(imagesDir(context), filename)
        if (!file.exists()) return null
        return CryptoManager.decryptFile(file)
    }

    /**
     * Re-encrypts [rawBytes] back into the existing [filename], overwriting it. Used only by
     * [app.orionmd.digitalwallet.security.KeyRotation] when the active encryption key changes
     * (a PIN change) and every image needs to move from the old key to the new one without
     * changing the filename any database row points at.
     */
    fun saveAt(context: Context, filename: String, rawBytes: ByteArray) {
        CryptoManager.encryptFile(rawBytes, File(imagesDir(context), filename))
    }

    fun delete(context: Context, filename: String?) {
        if (filename.isNullOrEmpty()) return
        File(imagesDir(context), filename).delete()
    }
}
