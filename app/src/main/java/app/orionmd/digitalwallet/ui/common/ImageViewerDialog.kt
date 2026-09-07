package app.orionmd.digitalwallet.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.ImageView

/**
 * Full-screen "tap the thumbnail to see it clearly" viewer for a card/ID/contact photo. Just a
 * plain full-bleed ImageView on a black background - dismissed by tapping it or the back button.
 * Nothing here is written to disk; it only ever shows a [Bitmap] already decrypted in memory.
 */
object ImageViewerDialog {
    fun show(context: Context, bitmap: Bitmap) {
        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageBitmap(bitmap)
        }
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(imageView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
