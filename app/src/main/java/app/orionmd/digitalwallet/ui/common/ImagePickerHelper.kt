package app.orionmd.digitalwallet.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import app.orionmd.digitalwallet.databinding.DialogImageSourceBinding
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Wraps the two ways a card/ID/contact photo can be captured - the camera or the gallery -
 * behind a single callback so edit screens don't each re-implement URI/FileProvider plumbing.
 * The bitmap handed back is never written to disk in the clear; callers pass it straight to
 * [app.orionmd.digitalwallet.data.ImageStore] for encryption.
 */
class ImagePickerHelper(
    private val context: Context,
    caller: ActivityResultCaller,
    private val onImageReady: (Bitmap) -> Unit
) {
    private var pendingCameraUri: Uri? = null

    private val takePicture = caller.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                loadBitmapFromUri(uri)?.let(onImageReady)
            }
        }
    }

    private val pickFromGallery = caller.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadBitmapFromUri(it)?.let(onImageReady) }
    }

    fun launchCamera() {
        val photoFile = File.createTempFile("capture_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(context, "app.orionmd.digitalwallet.fileprovider", photoFile)
        pendingCameraUri = uri
        takePicture.launch(uri)
    }

    fun launchGallery() {
        pickFromGallery.launch("image/*")
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()

    companion object {
        /**
         * Displays an actual uploaded/captured photo in [imageView]. The placeholder "+" icon in
         * these ImageViews is tinted gray (`app:tint`) in XML, but a view's tint list applies to
         * *whatever* drawable it's showing - including a real photo set later via
         * [ImageView.setImageBitmap] - which otherwise recolors the whole photo into a flat gray
         * box. The placeholder padding is meant for the small "+" glyph too, not a full photo.
         * Always use this (never `imageView.setImageBitmap(bitmap)` directly) to show a real photo.
         */
        fun applyPhoto(imageView: ImageView, bitmap: Bitmap) {
            imageView.imageTintList = null
            imageView.setPadding(0, 0, 0, 0)
            imageView.setImageBitmap(bitmap)
        }

        /** Compresses a bitmap to JPEG bytes suitable for [app.orionmd.digitalwallet.data.ImageStore.save]. */
        fun toJpegBytes(bitmap: Bitmap, quality: Int = 85): ByteArray {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            return out.toByteArray()
        }

        /** Shows a simple "Camera / Gallery" chooser and invokes the matching callback. */
        fun showImageSourceDialog(context: Context, onCamera: () -> Unit, onGallery: () -> Unit) {
            val binding = DialogImageSourceBinding.inflate(LayoutInflater.from(context))
            val dialog = AlertDialog.Builder(context)
                .setView(binding.root)
                .create()
            binding.optionCamera.setOnClickListener {
                dialog.dismiss()
                onCamera()
            }
            binding.optionGallery.setOnClickListener {
                dialog.dismiss()
                onGallery()
            }
            dialog.show()
        }
    }
}
