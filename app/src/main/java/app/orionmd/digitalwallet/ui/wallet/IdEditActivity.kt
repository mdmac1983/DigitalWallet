package app.orionmd.digitalwallet.ui.wallet

import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.IdDocumentItem
import app.orionmd.digitalwallet.data.IdDocumentRepository
import app.orionmd.digitalwallet.data.IdDocumentType
import app.orionmd.digitalwallet.data.ImageStore
import app.orionmd.digitalwallet.databinding.ActivityIdEditBinding
import app.orionmd.digitalwallet.ui.common.ImagePickerHelper
import app.orionmd.digitalwallet.ui.common.ImageViewerDialog
import kotlinx.coroutines.launch

class IdEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_DOCUMENT_ID = "extra_id_document_id"
        private val DOCUMENT_TYPES = listOf(
            IdDocumentType.DRIVERS_LICENSE,
            IdDocumentType.STATE_ID,
            IdDocumentType.SOCIAL_SECURITY_CARD,
            IdDocumentType.PASSPORT
        )
    }

    private lateinit var binding: ActivityIdEditBinding
    private lateinit var repository: IdDocumentRepository

    private var documentId: Long = -1L
    private var existing: IdDocumentItem? = null

    private var pendingFrontBitmap: Bitmap? = null
    private var pendingBackBitmap: Bitmap? = null

    private var displayedFrontBitmap: Bitmap? = null
    private var displayedBackBitmap: Bitmap? = null

    private lateinit var frontPicker: ImagePickerHelper
    private lateinit var backPicker: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = IdDocumentRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        documentId = intent.getLongExtra(EXTRA_ID_DOCUMENT_ID, -1L)
        binding.toolbar.title = getString(if (documentId == -1L) R.string.new_id_title else R.string.edit_id_title)

        val typeLabels = DOCUMENT_TYPES.map { getString(labelFor(it)) }
        binding.spinnerDocType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeLabels)

        frontPicker = ImagePickerHelper(this, this) { bitmap ->
            pendingFrontBitmap = bitmap
            displayedFrontBitmap = bitmap
            ImagePickerHelper.applyPhoto(binding.photos.imageFront, bitmap)
        }
        backPicker = ImagePickerHelper(this, this) { bitmap ->
            pendingBackBitmap = bitmap
            displayedBackBitmap = bitmap
            ImagePickerHelper.applyPhoto(binding.photos.imageBack, bitmap)
        }
        binding.photos.imageFront.setOnClickListener {
            displayedFrontBitmap?.let { ImageViewerDialog.show(this, it) }
                ?: ImagePickerHelper.showImageSourceDialog(this, { frontPicker.launchCamera() }, { frontPicker.launchGallery() })
        }
        binding.photos.imageBack.setOnClickListener {
            displayedBackBitmap?.let { ImageViewerDialog.show(this, it) }
                ?: ImagePickerHelper.showImageSourceDialog(this, { backPicker.launchCamera() }, { backPicker.launchGallery() })
        }
        binding.photos.buttonEditFront.setOnClickListener {
            ImagePickerHelper.showImageSourceDialog(this, { frontPicker.launchCamera() }, { frontPicker.launchGallery() })
        }
        binding.photos.buttonEditBack.setOnClickListener {
            ImagePickerHelper.showImageSourceDialog(this, { backPicker.launchCamera() }, { backPicker.launchGallery() })
        }

        binding.buttonSave.setOnClickListener { save() }

        if (documentId != -1L) {
            lifecycleScope.launch {
                existing = repository.getById(documentId)
                existing?.let { populate(it) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (documentId != -1L) menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_delete -> { confirmDelete(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populate(item: IdDocumentItem) = with(binding) {
        spinnerDocType.setSelection(DOCUMENT_TYPES.indexOf(item.documentType).coerceAtLeast(0))
        editFullName.setText(item.fullName)
        editDocumentNumber.setText(item.documentNumber)
        editIssueDate.setText(item.issueDate)
        editExpirationDate.setText(item.expirationDate)
        editIssuingRegion.setText(item.issuingRegion)
        editNotes.setText(item.notes)

        ImageStore.loadBitmap(this@IdEditActivity, item.frontImagePath)?.let {
            displayedFrontBitmap = it
            ImagePickerHelper.applyPhoto(photos.imageFront, it)
        }
        ImageStore.loadBitmap(this@IdEditActivity, item.backImagePath)?.let {
            displayedBackBitmap = it
            ImagePickerHelper.applyPhoto(photos.imageBack, it)
        }
    }

    private fun save() {
        var frontPath = existing?.frontImagePath
        var backPath = existing?.backImagePath

        pendingFrontBitmap?.let { bmp ->
            ImageStore.delete(this, frontPath)
            frontPath = ImageStore.save(this, ImagePickerHelper.toJpegBytes(bmp))
        }
        pendingBackBitmap?.let { bmp ->
            ImageStore.delete(this, backPath)
            backPath = ImageStore.save(this, ImagePickerHelper.toJpegBytes(bmp))
        }

        val item = IdDocumentItem(
            id = if (documentId == -1L) 0 else documentId,
            documentType = DOCUMENT_TYPES[binding.spinnerDocType.selectedItemPosition],
            fullName = binding.editFullName.text?.toString().orEmpty(),
            documentNumber = binding.editDocumentNumber.text?.toString().orEmpty(),
            issueDate = binding.editIssueDate.text?.toString().orEmpty(),
            expirationDate = binding.editExpirationDate.text?.toString().orEmpty(),
            issuingRegion = binding.editIssuingRegion.text?.toString().orEmpty(),
            notes = binding.editNotes.text?.toString().orEmpty(),
            frontImagePath = frontPath,
            backImagePath = backPath
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

    private fun labelFor(type: IdDocumentType): Int = when (type) {
        IdDocumentType.DRIVERS_LICENSE -> R.string.doc_type_drivers_license
        IdDocumentType.STATE_ID -> R.string.doc_type_state_id
        IdDocumentType.SOCIAL_SECURITY_CARD -> R.string.doc_type_ssn_card
        IdDocumentType.PASSPORT -> R.string.doc_type_passport
    }
}
