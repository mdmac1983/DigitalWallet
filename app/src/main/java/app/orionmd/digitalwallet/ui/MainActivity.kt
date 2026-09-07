package app.orionmd.digitalwallet.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.databinding.ActivityMainBinding
import app.orionmd.digitalwallet.security.CryptoManager
import app.orionmd.digitalwallet.ui.contacts.ContactsFragment
import app.orionmd.digitalwallet.ui.finances.FinancesFragment
import app.orionmd.digitalwallet.ui.passwords.PasswordsFragment
import app.orionmd.digitalwallet.ui.pin.PinUnlockActivity
import app.orionmd.digitalwallet.ui.sales.SalesFragment
import app.orionmd.digitalwallet.ui.settings.SettingsActivity
import app.orionmd.digitalwallet.ui.wallet.WalletFragment

/**
 * Hosts the five main tabs (Wallet, Passwords, Contacts, Finances at a Glance, Sales) behind the
 * bottom navigation bar. Only reachable once [CryptoManager] has been unlocked via the PIN screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safety net: never show any data if somehow reached without unlocking first
        // (e.g. a stale task being resumed from the recents list after a process kill).
        if (!CryptoManager.isUnlocked()) {
            startActivity(Intent(this, PinUnlockActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            showFragment(WalletFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_wallet -> { showFragment(WalletFragment()); true }
                R.id.nav_passwords -> { showFragment(PasswordsFragment()); true }
                R.id.nav_contacts -> { showFragment(ContactsFragment()); true }
                R.id.nav_finances -> { showFragment(FinancesFragment()); true }
                R.id.nav_sales -> { showFragment(SalesFragment()); true }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
