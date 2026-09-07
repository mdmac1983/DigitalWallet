package app.orionmd.digitalwallet.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around Google ML Kit's on-device text recognizer (same approach used by the
 * Top Notch Lock work-order app) - free, works fully offline once the model is present on the
 * device, and never uploads the image anywhere. Used only as a fallback for statement pages
 * that aren't real extractable-text PDF pages (a scanned page, or a photo of a paper statement).
 */
object OcrEngine {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** Returns the recognized text, or an empty string if OCR fails for any reason. */
    suspend fun recognize(bitmap: Bitmap): String = runCatching {
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
    }.getOrDefault("")
}
