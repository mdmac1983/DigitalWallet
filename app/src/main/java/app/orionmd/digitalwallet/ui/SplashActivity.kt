package app.orionmd.digitalwallet.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.databinding.ActivitySplashBinding
import app.orionmd.digitalwallet.ui.pin.PinUnlockActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_DELAY_MS = 1100L

/** The app's actual launcher activity: shows the brand logo briefly, then hands off to
 * [PinUnlockActivity], which does the real routing (PIN setup vs. unlock vs. straight through if
 * already unlocked this process). Purely cosmetic - holds no app state of its own. */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            startActivity(Intent(this@SplashActivity, PinUnlockActivity::class.java))
            finish()
        }
    }
}
