package app.orionmd.digitalwallet.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Export/import of a full local backup: the encrypted Room database plus the encrypted images/
 * and documents/ directories (see [ImageStore]/[DocumentStore]), zipped together. Nothing here
 * ever touches plaintext - every byte written to or read from the zip is exactly what's already
 * on disk, still under AES-GCM with the session key derived from the PIN. That's also why a
 * restored backup only decrypts correctly when unlocked with the SAME PIN that was active at
 * export time: the key itself is derived from the PIN and never stored anywhere on its own, so
 * restoring under a different PIN produces a database Room can open but that won't decrypt.
 */
object BackupManager {

    private const val DB_ENTRY_PREFIX = "databases/"
    private const val IMAGES_ENTRY_PREFIX = "files/images/"
    private const val DOCUMENTS_ENTRY_PREFIX = "files/documents/"

    /** Builds the backup zip and returns it. Runs its own file/DB I/O - call from a background
     * dispatcher. */
    suspend fun export(context: Context): File {
        // Flush the write-ahead log into the main db file, then let go of Room's open connection
        // entirely so the file on disk isn't being written to while we read it below.
        AppDatabase.getInstance(context).query("PRAGMA wal_checkpoint(FULL)", null).use { }
        AppDatabase.closeInstance()

        val outDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        outDir.listFiles()?.forEach { it.delete() } // don't accumulate old backups forever
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val outFile = File(outDir, "digital-wallet-backup-$stamp.zip")

        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
            dbFile.parentFile
                ?.listFiles { f -> f.name.startsWith(AppDatabase.DB_NAME) }
                ?.forEach { addFileToZip(zip, it, "$DB_ENTRY_PREFIX${it.name}") }

            addDirToZip(zip, File(context.filesDir, "images"), IMAGES_ENTRY_PREFIX)
            addDirToZip(zip, File(context.filesDir, "documents"), DOCUMENTS_ENTRY_PREFIX)
        }
        return outFile
    }

    /** Replaces the live database and every stored image/document with what's in [zipUri].
     * Returns true on success. Callers must get explicit user confirmation first - this performs
     * none of its own - and must restart the app process immediately after a successful import,
     * since everything already in memory (the unlocked session key, any cached rows) refers to
     * data that no longer exists on disk. */
    suspend fun import(context: Context, zipUri: Uri): Boolean = runCatching {
        AppDatabase.closeInstance()

        val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
        val dbDir = dbFile.parentFile?.apply { if (!exists()) mkdirs() }
        val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
        val documentsDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

        // Clear what's already there first, so a smaller/older backup (no -wal file, fewer
        // images) doesn't leave this device's own stray leftovers mixed in with the restored set.
        dbDir?.listFiles { f -> f.name.startsWith(AppDatabase.DB_NAME) }?.forEach { it.delete() }
        imagesDir.listFiles()?.forEach { it.delete() }
        documentsDir.listFiles()?.forEach { it.delete() }

        var sawDbEntry = false
        context.contentResolver.openInputStream(zipUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val target = when {
                        name.startsWith(DB_ENTRY_PREFIX) -> {
                            sawDbEntry = true
                            File(dbDir, name.removePrefix(DB_ENTRY_PREFIX))
                        }
                        name.startsWith(IMAGES_ENTRY_PREFIX) -> File(imagesDir, name.removePrefix(IMAGES_ENTRY_PREFIX))
                        name.startsWith(DOCUMENTS_ENTRY_PREFIX) -> File(documentsDir, name.removePrefix(DOCUMENTS_ENTRY_PREFIX))
                        else -> null
                    }
                    if (target != null && !entry.isDirectory && !name.endsWith("/")) {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { out -> zip.copyTo(out) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: return@runCatching false

        sawDbEntry
    }.getOrDefault(false)

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun addDirToZip(zip: ZipOutputStream, dir: File, entryPrefix: String) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { f ->
            if (f.isFile) addFileToZip(zip, f, "$entryPrefix${f.name}")
        }
    }
}
