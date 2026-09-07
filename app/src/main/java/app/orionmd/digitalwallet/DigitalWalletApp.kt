package app.orionmd.digitalwallet

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class DigitalWalletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // PdfBox-Android needs its font/resource assets primed once before first use (reading a
        // statement PDF, or generating the consolidated monthly PDF). Cheap, idempotent.
        PDFBoxResourceLoader.init(applicationContext)
    }
}
