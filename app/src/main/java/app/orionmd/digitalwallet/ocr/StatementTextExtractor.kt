package app.orionmd.digitalwallet.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * Pulls readable text out of an uploaded statement, whichever form it came in:
 *
 * - A digital PDF (the normal case - downloaded straight from a bank/card/investment site) has
 *   real selectable text, so [PDFTextStripper] reads it directly per page - fast and exact.
 * - Any page that comes back with almost no text is treated as a scanned image (some statements
 *   are just a scan) and rasterized with the platform's [PdfRenderer], then run through
 *   [OcrEngine].
 * - A plain photo/image upload always goes straight through [OcrEngine].
 *
 * Either way this only produces *candidate* text for [app.orionmd.digitalwallet.ui.statements.StatementParser]
 * to search for transaction-shaped lines - nothing here is trusted blindly.
 */
object StatementTextExtractor {

    private const val MIN_CHARS_TO_TRUST_PDF_TEXT = 20
    private const val RENDER_SCALE = 2

    suspend fun extractText(context: Context, fileBytes: ByteArray, mimeType: String?): String {
        return if (mimeType == "application/pdf") {
            extractFromPdf(context, fileBytes)
        } else {
            val bitmap = runCatching { BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size) }.getOrNull()
                ?: return ""
            OcrEngine.recognize(bitmap)
        }
    }

    private suspend fun extractFromPdf(context: Context, fileBytes: ByteArray): String {
        val tempFile = File.createTempFile("stmt_", ".pdf", context.cacheDir)
        return try {
            tempFile.writeBytes(fileBytes)
            PDFBoxResourceLoader.init(context.applicationContext)
            val builder = StringBuilder()
            PDDocument.load(tempFile).use { document ->
                val pageCount = document.numberOfPages
                for (pageIndex in 0 until pageCount) {
                    val pageText = runCatching {
                        PDFTextStripper().apply {
                            startPage = pageIndex + 1
                            endPage = pageIndex + 1
                        }.getText(document)
                    }.getOrDefault("")

                    if (pageText.trim().length >= MIN_CHARS_TO_TRUST_PDF_TEXT) {
                        builder.append(pageText).append('\n')
                    } else {
                        renderPdfPage(tempFile, pageIndex)?.let { bitmap ->
                            builder.append(OcrEngine.recognize(bitmap)).append('\n')
                        }
                    }
                }
            }
            builder.toString()
        } finally {
            tempFile.delete()
        }
    }

    /** Rasterizes one page of [pdfFile] via the platform's built-in PDF renderer - separate from
     * PdfBox-Android, which is used above only for text and (elsewhere) for writing PDFs. */
    private fun renderPdfPage(pdfFile: File, pageIndex: Int): Bitmap? = runCatching {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (pageIndex >= renderer.pageCount) return null
                renderer.openPage(pageIndex).use { page ->
                    val bitmap = Bitmap.createBitmap(
                        page.width * RENDER_SCALE, page.height * RENDER_SCALE, Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }.getOrNull()
}
