package app.orionmd.digitalwallet.ui.pin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.databinding.ActivityPinUnlockBinding
import app.orionmd.digitalwallet.security.CryptoManager
import app.orionmd.digitalwallet.security.PinManager
import app.orionmd.digitalwallet.ui.MainActivity

/**
 * Reached right after the splash screen ([app.orionmd.digitalwallet.ui.SplashActivity]) on every
 * cold start: if no PIN has been set yet, it hands off to [PinSetupActivity]; otherwise it demands
 * the PIN before unlocking [CryptoManager] and moving on to [MainActivity]. If the process is
 * already unlocked (e.g. returning from a background state within the same process lifetime) it
 * skips straight through.
 */
class PinUnlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinUnlockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PinManager.isPinSet(this)) {
            startActivity(Intent(this, PinSetupActivity::class.java))
            finish()
            return
        }

        if (CryptoManager.isUnlocked()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityPinUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonUnlock.setOnClickListener { attemptUnlock() }
        binding.editPin.setOnEditorActionListener { _, _, _ ->
            attemptUnlock()
            true
        }
    }

    private fun attemptUnlock() {
        val pin = binding.editPin.text?.toString().orEmpty()
        val masterKey = PinManager.deriveMasterKeyIfValid(this, pin)

        if (masterKey == null) {
            binding.textError.visibility = View.VISIBLE
            binding.editPin.text?.clear()
            return
        }

        CryptoManager.unlock(masterKey)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
