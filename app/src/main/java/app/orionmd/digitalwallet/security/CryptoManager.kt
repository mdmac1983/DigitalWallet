package app.orionmd.digitalwallet.security

import android.util.Base64
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Holds the session's AES-256 master key (derived from the user's PIN by [PinManager]) in
 * memory only, and encrypts/decrypts every sensitive field and image the app stores. The key
 * lives only as long as the process is unlocked; there is no way to encrypt or decrypt
 * anything without first unlocking via the PIN screen.
 */
object CryptoManager {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    @Volatile
    private var sessionKey: SecretKeySpec? = null

    fun unlock(masterKeyBytes: ByteArray) {
        sessionKey = SecretKeySpec(masterKeyBytes, "AES")
    }

    fun lock() {
        sessionKey = null
    }

    fun isUnlocked(): Boolean = sessionKey != null

    private fun requireKey(): SecretKeySpec =
        sessionKey ?: throw IllegalStateException("CryptoManager is locked - unlock with the PIN first")

    /** Encrypts [plainText]; returns a Base64 string safe to store in the database. */
    fun encryptString(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val (iv, cipherBytes) = encryptBytes(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherBytes, Base64.NO_WRAP)
    }

    /** Reverses [encryptString]. Returns "" for blank/empty input rather than throwing. */
    fun decryptString(storedValue: String): String {
        if (storedValue.isEmpty()) return ""
        val combined = Base64.decode(storedValue, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
        return String(decryptBytes(iv, cipherBytes), Charsets.UTF_8)
    }

    /** Encrypts an arbitrary file's bytes to [outputFile], overwriting it if present. */
    fun encryptFile(inputBytes: ByteArray, outputFile: File) {
        val (iv, cipherBytes) = encryptBytes(inputBytes)
        outputFile.outputStream().use { out ->
            out.write(iv)
            out.write(cipherBytes)
        }
    }

    /** Decrypts a file previously written by [encryptFile] and returns the plaintext bytes. */
    fun decryptFile(encryptedFile: File): ByteArray {
        val combined = encryptedFile.readBytes()
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
        return decryptBytes(iv, cipherBytes)
    }

    private fun encryptBytes(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val key = requireKey()
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return iv to cipher.doFinal(plain)
    }

    private fun decryptBytes(iv: ByteArray, cipherBytes: ByteArray): ByteArray {
        val key = requireKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }
}
