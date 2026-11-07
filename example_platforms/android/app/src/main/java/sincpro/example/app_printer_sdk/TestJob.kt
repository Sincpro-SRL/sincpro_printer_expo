package sincpro.example.app_printer_sdk

import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.suspendCancellableCoroutine
import sincpro.expo.printer.domain.Media
import sincpro.expo.printer.domain.MediaType
import sincpro.expo.printer.domain.PrintCommand
import sincpro.expo.printer.domain.PrintJob
import sincpro.expo.printer.domain.QrErrorCorrection
import sincpro.expo.printer.services.PrinterService

/** Runs a task on the service's SDK thread, exactly like the Expo module does. */
suspend fun <T> PrinterService.call(task: PrinterService.() -> T): T =
    suspendCancellableCoroutine { continuation -> enqueue(task) { result -> continuation.resumeWith(result) } }

enum class Font(val code: Int, val charWidth: Int, val charHeight: Int) {
    SMALL(49, 12, 20),
    MEDIUM(51, 19, 30),
    LARGE(53, 32, 40),
    XLARGE(54, 48, 76),
}

enum class Align { LEFT, CENTER, RIGHT }

enum class Symbology(val code: Int) { CODE39(0), CODE128(1), EAN13(7) }

/**
 * Minimal top-to-bottom layout for test jobs. The production layout lives in TypeScript
 * (src/domain/receipt.ts); this only places primitives so the native driver can be exercised.
 */
class TestJob(private val widthDots: Int = 576) {
    private val commands = mutableListOf<PrintCommand>()
    private var y = 20

    private fun x(align: Align, contentWidth: Int) =
        when (align) {
            Align.LEFT -> 10
            Align.CENTER -> maxOf(0, (widthDots - contentWidth) / 2)
            Align.RIGHT -> maxOf(0, widthDots - contentWidth - 10)
        }

    fun text(text: String, font: Font = Font.MEDIUM, bold: Boolean = false, align: Align = Align.LEFT) = apply {
        commands += PrintCommand.Text(x(align, text.length * font.charWidth), y, font.charHeight, text, font.code, bold)
        y += maxOf(30, font.charHeight)
    }

    fun qr(data: String, size: Int = 5, align: Align = Align.CENTER) = apply {
        val side = 25 * size
        commands += PrintCommand.Qr(x(align, side), y, side, data, size, QrErrorCorrection.M)
        y += side + 20
    }

    fun barcode(data: String, symbology: Symbology = Symbology.CODE128, barHeight: Int = 60, align: Align = Align.CENTER) =
        apply {
            val width = (11 * data.length + 35) * 2
            commands += PrintCommand.Barcode(x(align, width), y, barHeight + 30, data, symbology.code, 2, 6, barHeight, true)
            y += barHeight + 30
        }

    fun image(base64: String, align: Align = Align.CENTER) = apply {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = minOf(bounds.outWidth, widthDots)
        val height = bounds.outHeight * width / maxOf(1, bounds.outWidth)
        commands += PrintCommand.Image(x(align, width), y, height, base64, width)
        y += height + 10
    }

    fun pdf(base64: String, page: Int = 1) = apply {
        commands += PrintCommand.PdfPage(0, y, 0, base64, page, widthDots)
    }

    fun separator() = text("-".repeat((widthDots - 20) / Font.SMALL.charWidth), Font.SMALL)

    fun space(dots: Int = 20) = apply { y += dots }

    fun build(copies: Int = 1) = PrintJob(Media(widthDots, 0, 0, MediaType.CONTINUOUS), copies, commands.toList())
}
