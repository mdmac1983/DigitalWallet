package app.orionmd.digitalwallet.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Owns the app's PIN: setting it, verifying it, and deriving the AES master key used to
 * encrypt everything at rest. The PIN itself is never stored — only a salted, one-way
 * verification hash (SHA-256 of the derived key, not the key itself) is kept, so recovering
 * the stored hash does not hand over the encryption key.
 */
object PinManager {

    private const val PREFS_NAME = "wallet_pin_prefs"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_AUTH_HASH = "pin_auth_hash"
    private const val PBKDF2_ITERATIONS = 150_000
    private const val KEY_LENGTH_BITS = 256
    private const val AUTH_LABEL = "app.orionmd.digitalwallet.auth.v1"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isPinSet(context: Context): Boolean = prefs(context).contains(KEY_AUTH_HASH)

    /** Sets (or overwrites) the PIN and returns the freshly derived AES master key. */
    fun setPin(context: Context, pin: String): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val masterKeyBytes = deriveMasterKey(pin, salt)
        val authHash = authHashOf(masterKeyBytes)
        prefs(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_AUTH_HASH, Base64.encodeToString(authHash, Base64.NO_WRAP))
            .apply()
        return masterKeyBytes
    }

    /**
     * Verifies [pin] against the stored hash. Returns the derived AES master key on success,
     * or null if the PIN is wrong (or none has been set yet).
     */
    fun deriveMasterKeyIfValid(context: Context, pin: String): ByteArray? {
        val p = prefs(context)
        val saltB64 = p.getString(KEY_SALT, null) ?: return null
        val storedHashB64 = p.getString(KEY_AUTH_HASH, null) ?: return null
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val storedHash = Base64.decode(storedHashB64, Base64.NO_WRAP)

        val candidateKey = deriveMasterKey(pin, salt)
        val candidateHash = authHashOf(candidateKey)

        return if (MessageDigest.isEqual(candidateHash, storedHash)) candidateKey else null
    }

    fun changePin(context: Context, newPin: String): ByteArray = setPin(context, newPin)

    private fun deriveMasterKey(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun authHashOf(masterKey: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(masterKey)
        digest.update(AUTH_LABEL.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }
}
