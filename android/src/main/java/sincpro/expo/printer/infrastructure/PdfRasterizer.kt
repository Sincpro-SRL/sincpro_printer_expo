package sincpro.expo.printer.infrastructure

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

class PdfRasterizer(private val cacheDir: File) {
    private fun <T> withRenderer(base64: String, block: (PdfRenderer) -> T): T {
        val file = File.createTempFile("sincpro_printer_", ".pdf", cacheDir)
        try {
            file.writeBytes(BitmapDecoder.decodeBytes(base64))
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer -> return block(renderer) }
            }
        } finally {
            file.delete()
        }
    }

    fun pageCount(base64: String): Int = runCatching { withRenderer(base64) { it.pageCount } }.getOrDefault(0)

    /** Renders a 1-based page scaled to [targetWidth] dots on a white background; null if it doesn't exist. */
    fun render(base64: String, page: Int, targetWidth: Int): Bitmap? =
        runCatching {
            withRenderer(base64) { renderer ->
                if (page < 1 || page > renderer.pageCount) return@withRenderer null
                renderer.openPage(page - 1).use { pdfPage ->
                    val height = (pdfPage.height * targetWidth.toFloat() / pdfPage.width).toInt()
                    Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE)
                        pdfPage.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    }
                }
            }
        }.getOrNull()
}
