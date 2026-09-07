# Add project specific ProGuard rules here.
-keep class app.orionmd.digitalwallet.data.** { *; }
-keepattributes *Annotation*

# pdfbox-android's JPXFilter optionally supports JPEG2000-encoded images via an external
# "JP2" decoder/encoder library that isn't bundled (and isn't needed - none of this app's own
# generated PDFs, nor typical bank/statement PDFs, use JPEG2000 image encoding). Safe to silence.
-dontwarn com.gemalto.jp2.**
