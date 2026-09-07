package app.orionmd.digitalwallet.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import app.orionmd.digitalwallet.security.CryptoManager
import java.io.File
import java.util.UUID

/**
 * Encrypted-at-rest storage for arbitrary uploaded files - statement PDFs and photographed
 * statement images - the same trust model as [ImageStore] (private app storage only, AES-GCM
 * under the session key) but for any binary content rather than just decodable bitmaps, since a
 * statement's original file is worth keeping around for reference even after its transactions
 * have been parsed out.
 */
object DocumentStore {

    private fun documentsDir(context: Context): File =
        File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    /** Encrypts [rawBytes] and returns the filename it was stored under. */
    fun save(context: Context, rawBytes: ByteArray): String {
        val filename = "${UUID.randomUUID()}.enc"
        CryptoManager.encryptFile(rawBytes, File(documentsDir(context), filename))
        return filename
    }

    /** Decrypts the file stored under [filename] and returns its raw bytes, or null if missing. */
    fun loadDecryptedBytes(context: Context, filename: String?): ByteArray? {
        if (filename.isNullOrEmpty()) return null
        val file = File(documentsDir(context), filename)
        if (!file.exists()) return null
        return CryptoManager.decryptFile(file)
    }

    /** Re-encrypts [rawBytes] back into the existing [filename]. Used by [app.orionmd.digitalwallet.security.KeyRotation]. */
    fun saveAt(context: Context, filename: String, rawBytes: ByteArray) {
        CryptoManager.encryptFile(rawBytes, File(documentsDir(context), filename))
    }

    fun delete(context: Context, filename: String?) {
        if (filename.isNullOrEmpty()) return
        File(documentsDir(context), filename).delete()
    }

    /**
     * Decrypts [filename] into a throwaway cache file (named after [displayName] so a share
     * sheet / viewer shows a sensible name and extension) and returns a FileProvider [Uri] for
     * it, or null if the source file is missing. The cache copy is plaintext, which is fine -
     * app-private cache dirs are already only readable by this app.
     */
    fun uriForViewing(context: Context, filename: String?, displayName: String): Uri? {
        val bytes = loadDecryptedBytes(context, filename) ?: return null
        val safeName = displayName.ifBlank { "statement" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val viewDir = File(context.cacheDir, "view").apply { if (!exists()) mkdirs() }
        val outFile = File(viewDir, "${System.currentTimeMillis()}_$safeName")
        outFile.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "app.orionmd.digitalwallet.fileprovider", outFile)
    }

    /** Opens [filename] in an external viewer app (e.g. a PDF reader) via [Intent.ACTION_VIEW]. */
    fun openExternally(context: Context, filename: String?, mimeType: String?, displayName: String) {
        val uri = uriForViewing(context, filename, displayName) ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, displayName))
    }
}
