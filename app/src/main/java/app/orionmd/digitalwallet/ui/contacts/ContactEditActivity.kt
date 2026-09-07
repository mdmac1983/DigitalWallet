package app.orionmd.digitalwallet.ui.contacts

import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.ContactCategory
import app.orionmd.digitalwallet.data.ContactItem
import app.orionmd.digitalwallet.data.ContactRepository
import app.orionmd.digitalwallet.data.ImageStore
import app.orionmd.digitalwallet.databinding.ActivityContactEditBinding
import app.orionmd.digitalwallet.ui.common.ImagePickerHelper
import app.orionmd.digitalwallet.ui.common.ImageViewerDialog
import kotlinx.coroutines.launch

class ContactEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONTACT_ID = "extra_contact_id"
        private val CATEGORIES = listOf(
            ContactCategory.FAMILY,
            ContactCategory.FRIEND,
            ContactCategory.WORK,
            ContactCategory.BUSINESS,
            ContactCategory.OTHER
        )
    }

    private lateinit var binding: ActivityContactEditBinding
    private lateinit var repository: ContactRepository

    private var contactId: Long = -1L
    private var existing: ContactItem? = null
    private var pendingPhotoBitmap: Bitmap? = null
    private var displayedPhotoBitmap: Bitmap? = null
    private lateinit var photoPicker: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = ContactRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        binding.toolbar.title = getString(if (contactId == -1L) R.string.new_contact_title else R.string.edit_contact_title)

        val labels = CATEGORIES.map { getString(labelFor(it)) }
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        photoPicker = ImagePickerHelper(this, this) { bitmap ->
            pendingPhotoBitmap = bitmap
            displayedPhotoBitmap = bitmap
            ImagePickerHelper.applyPhoto(binding.imagePhoto, bitmap)
        }
        binding.imagePhoto.setOnClickListener {
            displayedPhotoBitmap?.let { ImageViewerDialog.show(this, it) }
                ?: ImagePickerHelper.showImageSourceDialog(this, { photoPicker.launchCamera() }, { photoPicker.launchGallery() })
        }
        binding.buttonEditPhoto.setOnClickListener {
            ImagePickerHelper.showImageSourceDialog(this, { photoPicker.launchCamera() }, { photoPicker.launchGallery() })
        }

        binding.buttonSave.setOnClickListener { save() }

        if (contactId != -1L) {
            lifecycleScope.launch {
                existing = repository.getById(contactId)
                existing?.let { populate(it) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (contactId != -1L) menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_delete -> { confirmDelete(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populate(item: ContactItem) = with(binding) {
        spinnerCategory.setSelection(CATEGORIES.indexOf(item.category).coerceAtLeast(0))
        editName.setText(item.name)
        editPhone.setText(item.phone)
        editEmail.setText(item.email)
        editAddress.setText(item.address)
        editCompany.setText(item.company)
        editNotes.setText(item.notes)
        ImageStore.loadBitmap(this@ContactEditActivity, item.photoPath)?.let {
            displayedPhotoBitmap = it
            ImagePickerHelper.applyPhoto(imagePhoto, it)
        }
    }

    private fun save() {
        var photoPath = existing?.photoPath
        pendingPhotoBitmap?.let { bmp ->
            ImageStore.delete(this, photoPath)
            photoPath = ImageStore.save(this, ImagePickerHelper.toJpegBytes(bmp))
        }

        val item = ContactItem(
            id = if (contactId == -1L) 0 else contactId,
            name = binding.editName.text?.toString().orEmpty(),
            phone = binding.editPhone.text?.toString().orEmpty(),
            email = binding.editEmail.text?.toString().orEmpty(),
            address = binding.editAddress.text?.toString().orEmpty(),
            company = binding.editCompany.text?.toString().orEmpty(),
            notes = binding.editNotes.text?.toString().orEmpty(),
            photoPath = photoPath,
            category = CATEGORIES[binding.spinnerCategory.selectedItemPosition]
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

    private fun labelFor(category: ContactCategory): Int = when (category) {
        ContactCategory.FAMILY -> R.string.category_family
        ContactCategory.FRIEND -> R.string.category_friend
        ContactCategory.WORK -> R.string.category_work
        ContactCategory.BUSINESS -> R.string.category_business
        ContactCategory.OTHER -> R.string.category_other
    }
}
