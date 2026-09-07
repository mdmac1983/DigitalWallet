package app.orionmd.digitalwallet.ui.wallet

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.CardItem
import app.orionmd.digitalwallet.data.CardRepository
import app.orionmd.digitalwallet.data.ImageStore
import app.orionmd.digitalwallet.databinding.ActivityCardEditBinding
import app.orionmd.digitalwallet.ui.common.ImagePickerHelper
import app.orionmd.digitalwallet.ui.common.ImageViewerDialog
import kotlinx.coroutines.launch

class CardEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CARD_ID = "extra_card_id"
    }

    private lateinit var binding: ActivityCardEditBinding
    private lateinit var repository: CardRepository

    private var cardId: Long = -1L
    private var existing: CardItem? = null

    private var pendingFrontBitmap: Bitmap? = null
    private var pendingBackBitmap: Bitmap? = null

    /** Whatever's currently shown in the front/back photo slots - existing or freshly picked -
     * kept around purely so tapping the thumbnail can show it enlarged. */
    private var displayedFrontBitmap: Bitmap? = null
    private var displayedBackBitmap: Bitmap? = null

    private lateinit var frontPicker: ImagePickerHelper
    private lateinit var backPicker: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = CardRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        cardId = intent.getLongExtra(EXTRA_CARD_ID, -1L)
        binding.toolbar.title = getString(if (cardId == -1L) R.string.new_card_title else R.string.edit_card_title)

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

        binding.layoutCardNumber.setEndIconOnClickListener { copyCardNumberToClipboard() }

        binding.buttonSave.setOnClickListener { save() }

        if (cardId != -1L) {
            lifecycleScope.launch {
                existing = repository.getById(cardId)
                existing?.let { populate(it) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (cardId != -1L) menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_delete -> { confirmDelete(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populate(item: CardItem) = with(binding) {
        editIssuer.setText(item.issuer)
        editCardName.setText(item.cardName)
        editCardholderName.setText(item.cardholderName)
        editCardNumber.setText(item.cardNumber)
        editExpDate.setText(item.expDate)
        editCvv.setText(item.cvv)
        editWebsitePortal.setText(item.websitePortal)
        editUsername.setText(item.username)
        editPassword.setText(item.password)
        editCreditLimit.setText(item.creditLimit)
        editBillingAddress.setText(item.billingAddress)
        editBillingCity.setText(item.billingCity)
        editBillingState.setText(item.billingState)
        editBillingZip.setText(item.billingZip)

        ImageStore.loadBitmap(this@CardEditActivity, item.frontImagePath)?.let {
            displayedFrontBitmap = it
            ImagePickerHelper.applyPhoto(photos.imageFront, it)
        }
        ImageStore.loadBitmap(this@CardEditActivity, item.backImagePath)?.let {
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

        val item = CardItem(
            id = if (cardId == -1L) 0 else cardId,
            issuer = binding.editIssuer.text?.toString().orEmpty(),
            cardName = binding.editCardName.text?.toString().orEmpty(),
            cardholderName = binding.editCardholderName.text?.toString().orEmpty(),
            cardNumber = binding.editCardNumber.text?.toString().orEmpty(),
            expDate = binding.editExpDate.text?.toString().orEmpty(),
            cvv = binding.editCvv.text?.toString().orEmpty(),
            websitePortal = binding.editWebsitePortal.text?.toString().orEmpty(),
            username = binding.editUsername.text?.toString().orEmpty(),
            password = binding.editPassword.text?.toString().orEmpty(),
            frontImagePath = frontPath,
            backImagePath = backPath,
            creditLimit = binding.editCreditLimit.text?.toString().orEmpty(),
            billingAddress = binding.editBillingAddress.text?.toString().orEmpty(),
            billingCity = binding.editBillingCity.text?.toString().orEmpty(),
            billingState = binding.editBillingState.text?.toString().orEmpty(),
            billingZip = binding.editBillingZip.text?.toString().orEmpty()
        )

        lifecycleScope.launch {
            repository.save(item)
            finish()
        }
    }

    private fun copyCardNumberToClipboard() {
        val number = binding.editCardNumber.text?.toString().orEmpty()
        if (number.isBlank()) return
        val clipboard = getSystemService(ClipboardManager::class.java)
        // Marked sensitive so Android 13+ suppresses the "copied" system preview popup for it,
        // and some launchers auto-clear sensitive clipboard entries sooner.
        val clip = ClipData.newPlainText(getString(R.string.field_card_number), number).apply {
            description.extras = android.os.PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_card_number, Toast.LENGTH_SHORT).show()
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
}
