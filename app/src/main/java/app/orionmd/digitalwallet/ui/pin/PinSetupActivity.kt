package app.orionmd.digitalwallet.ui.pin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.orionmd.digitalwallet.databinding.ActivityPinSetupBinding
import app.orionmd.digitalwallet.security.CryptoManager
import app.orionmd.digitalwallet.security.PinManager
import app.orionmd.digitalwallet.ui.MainActivity

/**
 * Shown once, the very first time the app launches: forces the user to create a PIN before
 * any other screen is reachable. Requires entering the PIN twice so a typo doesn't lock the
 * user out of their own encrypted data.
 */
class PinSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonCreatePin.setOnClickListener { attemptCreatePin() }
    }

    private fun attemptCreatePin() {
        val pin = binding.editPin.text?.toString().orEmpty()
        val confirmPin = binding.editPinConfirm.text?.toString().orEmpty()

        if (pin.length < 4) {
            binding.textError.text = getString(app.orionmd.digitalwallet.R.string.pin_error_too_short)
            binding.textError.visibility = android.view.View.VISIBLE
            return
        }
        if (pin != confirmPin) {
            binding.textError.text = getString(app.orionmd.digitalwallet.R.string.pin_error_mismatch)
            binding.textError.visibility = android.view.View.VISIBLE
            return
        }

        val masterKey = PinManager.setPin(this, pin)
        CryptoManager.unlock(masterKey)

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
