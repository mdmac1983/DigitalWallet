package app.orionmd.digitalwallet.ui.passwords

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.PasswordCategory
import app.orionmd.digitalwallet.data.PasswordItem
import app.orionmd.digitalwallet.data.PasswordRepository
import app.orionmd.digitalwallet.databinding.ActivityPasswordEditBinding
import kotlinx.coroutines.launch

class PasswordEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PASSWORD_ID = "extra_password_id"
        private val CATEGORIES = listOf(
            PasswordCategory.EMAIL,
            PasswordCategory.SOCIAL_MEDIA,
            PasswordCategory.WEBSITE,
            PasswordCategory.BANKING,
            PasswordCategory.INVESTMENT,
            PasswordCategory.OTHER
        )
    }

    private lateinit var binding: ActivityPasswordEditBinding
    private lateinit var repository: PasswordRepository

    private var passwordId: Long = -1L
    private var existing: PasswordItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = PasswordRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        passwordId = intent.getLongExtra(EXTRA_PASSWORD_ID, -1L)
        binding.toolbar.title = getString(if (passwordId == -1L) R.string.new_password_title else R.string.edit_password_title)

        val labels = CATEGORIES.map { getString(labelFor(it)) }
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        binding.buttonSave.setOnClickListener { save() }

        if (passwordId != -1L) {
            lifecycleScope.launch {
                existing = repository.getById(passwordId)
                existing?.let { populate(it) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (passwordId != -1L) menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_delete -> { confirmDelete(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populate(item: PasswordItem) = with(binding) {
        spinnerCategory.setSelection(CATEGORIES.indexOf(item.category).coerceAtLeast(0))
        editAccountName.setText(item.accountName)
        editUsername.setText(item.username)
        editPassword.setText(item.password)
        editUrl.setText(item.url)
        editNotes.setText(item.notes)
    }

    private fun save() {
        val item = PasswordItem(
            id = if (passwordId == -1L) 0 else passwordId,
            category = CATEGORIES[binding.spinnerCategory.selectedItemPosition],
            accountName = binding.editAccountName.text?.toString().orEmpty(),
            username = binding.editUsername.text?.toString().orEmpty(),
            password = binding.editPassword.text?.toString().orEmpty(),
            url = binding.editUrl.text?.toString().orEmpty(),
            notes = binding.editNotes.text?.toString().orEmpty()
        )
        lifecycleScope.launch {
            repository.save(item)
            finish()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    existing?.let { repository.delete(it) }
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun labelFor(category: PasswordCategory): Int = when (category) {
        PasswordCategory.EMAIL -> R.string.category_email
        PasswordCategory.SOCIAL_MEDIA -> R.string.category_social_media
        PasswordCategory.WEBSITE -> R.string.category_website
        PasswordCategory.BANKING -> R.string.category_banking
        PasswordCategory.INVESTMENT -> R.string.category_investment
        PasswordCategory.OTHER -> R.string.category_other
    }
}
