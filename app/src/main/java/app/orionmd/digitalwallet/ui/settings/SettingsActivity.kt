package app.orionmd.digitalwallet.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.BuildConfig
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.BackupManager
import app.orionmd.digitalwallet.databinding.ActivitySettingsBinding
import app.orionmd.digitalwallet.databinding.DialogChangePinBinding
import app.orionmd.digitalwallet.security.KeyRotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val importBackupPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { confirmImportBackup(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.textVersion.text = getString(
            R.string.settings_version_value_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        binding.textChangelog.text = loadChangelog()

        binding.rowChangePin.setOnClickListener { showChangePinDialog() }
        binding.rowBackupExport.setOnClickListener { exportBackup() }
        binding.rowBackupImport.setOnClickListener { importBackupPicker.launch("application/zip") }
        binding.rowBuyMeACoffee.setOnClickListener {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.buy_me_a_coffee_url))))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadChangelog(): String = runCatching {
        assets.open("changelog.txt").use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    }.getOrDefault("")

    private fun showChangePinDialog() {
        val dialogBinding = DialogChangePinBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.change_pin_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.settings_change_pin, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val currentPin = dialogBinding.editCurrentPin.text?.toString().orEmpty()
                val newPin = dialogBinding.editNewPin.text?.toString().orEmpty()
                val confirmPin = dialogBinding.editNewPinConfirm.text?.toString().orEmpty()

                when {
                    newPin.length < 4 -> showDialogError(dialogBinding, R.string.pin_error_too_short)
                    newPin != confirmPin -> showDialogError(dialogBinding, R.string.pin_error_mismatch)
                    else -> performChangePin(dialog, dialogBinding, currentPin, newPin)
                }
            }
        }
        dialog.show()
    }

    private fun showDialogError(dialogBinding: DialogChangePinBinding, resId: Int) {
        dialogBinding.textError.setText(resId)
        dialogBinding.textError.visibility = android.view.View.VISIBLE
    }

    private fun performChangePin(
        dialog: AlertDialog,
        dialogBinding: DialogChangePinBinding,
        currentPin: String,
        newPin: String
    ) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        Toast.makeText(this, R.string.pin_change_in_progress, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                KeyRotation.changePin(this@SettingsActivity, currentPin, newPin)
            }
            if (success) {
                Toast.makeText(this@SettingsActivity, R.string.pin_change_success, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                showDialogError(dialogBinding, R.string.pin_error_current_incorrect)
            }
        }
    }

    private fun exportBackup() {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching { BackupManager.export(this@SettingsActivity) }.getOrNull()
            }
            if (file == null) {
                Toast.makeText(this@SettingsActivity, R.string.backup_export_failed, Toast.LENGTH_LONG).show()
                return@launch
            }
            Toast.makeText(this@SettingsActivity, R.string.backup_export_success, Toast.LENGTH_SHORT).show()
            val uri = FileProvider.getUriForFile(this@SettingsActivity, "app.orionmd.digitalwallet.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.settings_backup_export)))
        }
    }

    private fun confirmImportBackup(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(R.string.backup_import_warning_title)
            .setMessage(R.string.backup_import_warning_message)
            .setPositiveButton(R.string.settings_backup_import) { _, _ -> performImportBackup(uri) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performImportBackup(uri: Uri) {
        Toast.makeText(this, R.string.pin_change_in_progress, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) { BackupManager.import(this@SettingsActivity, uri) }
            if (success) {
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle(R.string.backup_import_success)
                    .setCancelable(false)
                    .setPositiveButton(R.string.action_restart_now) { _, _ -> restartApp() }
                    .show()
            } else {
                Toast.makeText(this@SettingsActivity, R.string.backup_import_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun restartApp() {
        val restartIntent = packageManager.getLaunchIntentForPackage(packageName)
        restartIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    }
}
