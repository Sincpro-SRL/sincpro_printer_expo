package sincpro.expo.printer.service

import android.graphics.Bitmap
import android.util.Log
import sincpro.expo.printer.domain.Alignment
import sincpro.expo.printer.domain.FontSize
import sincpro.expo.printer.domain.IPrinterAdapter
import sincpro.expo.printer.domain.MediaConfig
import sincpro.expo.printer.domain.Receipt
import sincpro.expo.printer.domain.ReceiptLine
import sincpro.expo.printer.infrastructure.orchestration.EventBus
import sincpro.expo.printer.infrastructure.orchestration.PrintJobContext
import sincpro.expo.printer.infrastructure.orchestration.PrintJobOrchestrator

/**
 * SERVICE - Print Service (High Level)
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  SELF-CONTAINED SERVICE                                                 │
 * │                                                                         │
 * │  This service encapsulates ALL high-level printing operations:          │
 * │  • Print receipts (header + details + footer)                           │
 * │  • Print QR codes                                                       │
 * │  • Print text lines                                                     │
 * │  • Print images                                                         │
 * │                                                                         │
 * │  Default media: 80mm continuous paper (most common thermal printer)     │
 * │                                                                         │
 * │  Dependencies:                                                          │
 * │  • IPrinterAdapter (Adapter) - Printer-specific implementation          │
 * │  • PrintJobOrchestrator (Infrastructure) - Mutex/lifecycle              │
 * │  • EventBus (Infrastructure) - Event publishing                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * USAGE:
 *   // From JavaScript/TypeScript, clients create a Receipt:
 *   val receipt = Receipt(
 *       header = listOf(ReceiptLine.Text("My Store", FontSize.LARGE, Alignment.CENTER)),
 *       details = listOf(ReceiptLine.KeyValue("Item", "$10.00")),
 *       footer = listOf(ReceiptLine.QRCode("https://mystore.com"))
 *   )
 *   printService.printReceipt(receipt)
 */
class PrintService(
    // Adapter - Printer-specific implementation
    private val printerAdapter: IPrinterAdapter,
    // Infrastructure - Job orchestration (mutex, lifecycle)
    private val orchestrator: PrintJobOrchestrator,
    // Infrastructure - Event publishing
    private val eventBus: EventBus,
) {
    // HIGH-LEVEL PRINT OPERATIONS

    /**
     * Print a complete receipt (header + details + footer).
     *
     * @param receipt Receipt with header, details, and footer lines
     * @param mediaConfig Media configuration (default: 80mm continuous)
     * @param copies Number of copies to print
     * @return Result indicating success or failure
     */
    suspend fun printReceipt(
        receipt: Receipt,
        mediaConfig: MediaConfig = MediaConfig.continuous80mm(),
        copies: Int = 1,
    ): Result<Unit> {
        Log.d(
            this::class.simpleName,
            "🧾 Printing receipt with ${receipt.header.size} header, ${receipt.details.size} details, ${receipt.footer.size} footer lines",
        )

        return withPrintSession(mediaConfig) {
            var currentY = LAYOUT.TOP_MARGIN

            // Render header
            for (line in receipt.header) {
                currentY = renderLine(line, currentY)
            }

            // Space between header and details
            currentY += LAYOUT.SECTION_SPACING

            // Render details
            for (line in receipt.details) {
                currentY = renderLine(line, currentY)
            }

            // Space between details and footer
            currentY += LAYOUT.SECTION_SPACING

            // Render footer
            for (line in receipt.footer) {
                currentY = renderLine(line, currentY)
            }

            // Execute print
            print(copies)
            Log.d(this::class.simpleName, "✅ Receipt printed successfully")
        }
    }

    /**
     * Print a list of receipt lines.
     *
     * @param lines List of receipt lines to print
     * @param mediaConfig Media configuration (default: 80mm continuous)
     * @param copies Number of copies to print
     * @return Result indicating success or failure
     */
    suspend fun printLines(
        lines: List<ReceiptLine>,
        mediaConfig: MediaConfig = MediaConfig.continuous80mm(),
        copies: Int = 1,
    ): Result<Unit> {
        Log.d(this::class.simpleName, "📄 Printing ${lines.size} lines")

        return withPrintSession(mediaConfig) {
            var currentY = LAYOUT.TOP_MARGIN

            for (line in lines) {
                currentY = renderLine(line, currentY)
            }

            print(copies)
            Log.d(this::class.simpleName, "✅ Lines printed successfully")
        }
    }

    /**
     * Print a single QR code (convenience method).
     *
     * @param data Data to encode in QR code
     * @param size QR code size (1-10, default 5)
     * @param alignment QR code alignment (default: CENTER)
     * @param mediaConfig Media configuration (default: 80mm continuous)
     * @return Result indicating success or failure
     */
    suspend fun printQRCode(
        data: String,
        size: Int = 5,
        alignment: Alignment = Alignment.CENTER,
        mediaConfig: MediaConfig = MediaConfig.continuous80mm(),
    ): Result<Unit> {
        Log.d(this::class.simpleName, "📱 Printing QR code: $data")

        val qrLine = ReceiptLine.QRCode(data, size, alignment)
        return printLines(listOf(qrLine), mediaConfig)
    }

    /**
     * Print a single text line (convenience method).
     *
     * @param text Text content
     * @param fontSize Font size (default: MEDIUM)
     * @param alignment Text alignment (default: LEFT)
     * @param bold Bold text (default: false)
     * @param mediaConfig Media configuration (default: 80mm continuous)
     * @return Result indicating success or failure
     */
    suspend fun printText(
        text: String,
        fontSize: FontSize = FontSize.MEDIUM,
        alignment: Alignment = Alignment.LEFT,
        bold: Boolean = false,
        mediaConfig: MediaConfig = MediaConfig.continuous80mm(),
    ): Result<Unit> {
        Log.d(this::class.simpleName, "📝 Printing text: $text")

        val textLine = ReceiptLine.Text(text, fontSize, alignment, bold)
        return printLines(listOf(textLine), mediaConfig)
    }

    /**
     * Print a bitmap image (convenience method).
     *
     * @param bitmap Bitmap image to print
     * @param alignment Image alignment (default: CENTER)
     * @param mediaConfig Media configuration (default: 80mm continuous)
     * @return Result indicating success or failure
     */
    suspend fun printImage(
        bitmap: Bitmap,
        alignment: Alignment = Alignment.CENTER,
        mediaConfig: MediaConfig = MediaConfig.continuous80mm(),
    ): Result<Unit> {
        Log.d(this::class.simpleName, "🖼️ Printing image: ${bitmap.width}x${bitmap.height}")

        val imageLine = ReceiptLine.Image(bitmap, alignment)
        return printLines(listOf(imageLine), mediaConfig)
    }

    // CONTEXT MANAGER PATTERN

    /**
     * Execute a print session with proper lifecycle management.
     *
     * This is the core pattern - all high-level methods use this internally.
     *
     * @param mediaConfig Media configuration
     * @param block Code to execute within the print session
     * @return Result from block execution
     */
    private suspend fun <T> withPrintSession(
        mediaConfig: MediaConfig = MediaConfig.continuous80mm(),
        block: suspend PrintJobContext.() -> T,
    ): Result<T> = orchestrator.executeJob(mediaConfig = mediaConfig, block = block)

    // LINE RENDERING (Internal)

    /**
     * Render a receipt line and return the new Y position.
     */
    private suspend fun PrintJobContext.renderLine(
        line: ReceiptLine,
        currentY: Int,
    ): Int =
        when (line) {
            is ReceiptLine.Text -> renderText(line, currentY)
            is ReceiptLine.KeyValue -> renderKeyValue(line, currentY)
            is ReceiptLine.QRCode -> renderQRCode(line, currentY)
            is ReceiptLine.Separator -> renderSeparator(line, currentY)
            is ReceiptLine.Space -> currentY + (line.lines * LAYOUT.LINE_HEIGHT)
            is ReceiptLine.Image -> renderImage(line, currentY)
        }

    /**
     * Render text line
     */
    private suspend fun PrintJobContext.renderText(
        line: ReceiptLine.Text,
        currentY: Int,
    ): Int {
        val mediaWidth = getMediaWidth()

        val x =
            when (line.alignment) {
                Alignment.LEFT -> LAYOUT.LEFT_MARGIN
                Alignment.CENTER -> mediaWidth / 2 - (line.content.length * line.fontSize.dots / 4)
                Alignment.RIGHT -> mediaWidth - LAYOUT.RIGHT_MARGIN - (line.content.length * line.fontSize.dots / 2)
            }

        drawText(
            text = line.content,
            x = x.coerceAtLeast(LAYOUT.LEFT_MARGIN),
            y = currentY,
            fontSize = line.fontSize.dots,
            bold = line.bold,
            alignment = 0,
        )

        return currentY + line.fontSize.dots + LAYOUT.LINE_SPACING
    }

    /**
     * Render key-value pair
     */
    private suspend fun PrintJobContext.renderKeyValue(
        line: ReceiptLine.KeyValue,
        currentY: Int,
    ): Int {
        val mediaWidth = getMediaWidth()

        // Key on the left
        drawText(
            text = line.key,
            x = LAYOUT.LEFT_MARGIN,
            y = currentY,
            fontSize = line.fontSize.dots,
            bold = line.bold,
            alignment = 0,
        )

        // Value on the right
        val valueWidth = line.value.length * line.fontSize.dots / 2
        drawText(
            text = line.value,
            x = mediaWidth - LAYOUT.RIGHT_MARGIN - valueWidth,
            y = currentY,
            fontSize = line.fontSize.dots,
            bold = line.bold,
            alignment = 0,
        )

        return currentY + line.fontSize.dots + LAYOUT.LINE_SPACING
    }

    /**
     * Render QR code
     */
    private suspend fun PrintJobContext.renderQRCode(
        line: ReceiptLine.QRCode,
        currentY: Int,
    ): Int {
        val mediaWidth = getMediaWidth()

        val qrWidth = line.size * LAYOUT.QR_SIZE_MULTIPLIER
        val x =
            when (line.alignment) {
                Alignment.LEFT -> LAYOUT.LEFT_MARGIN
                Alignment.CENTER -> (mediaWidth - qrWidth) / 2
                Alignment.RIGHT -> mediaWidth - LAYOUT.RIGHT_MARGIN - qrWidth
            }

        drawQR(
            data = line.data,
            x = x.coerceAtLeast(LAYOUT.LEFT_MARGIN),
            y = currentY,
            size = line.size,
        )

        return currentY + qrWidth + LAYOUT.ELEMENT_SPACING
    }

    /**
     * Render separator line
     */
    private suspend fun PrintJobContext.renderSeparator(
        line: ReceiptLine.Separator,
        currentY: Int,
    ): Int {
        val separator = line.char.repeat(line.length)

        drawText(
            text = separator,
            x = LAYOUT.LEFT_MARGIN,
            y = currentY,
            fontSize = LAYOUT.SEPARATOR_FONT_SIZE,
            bold = false,
            alignment = 0,
        )

        return currentY + LAYOUT.LINE_HEIGHT
    }

    /**
     * Render image
     */
    private suspend fun PrintJobContext.renderImage(
        line: ReceiptLine.Image,
        currentY: Int,
    ): Int {
        val mediaWidth = getMediaWidth()

        val x =
            when (line.alignment) {
                Alignment.LEFT -> LAYOUT.LEFT_MARGIN
                Alignment.CENTER -> (mediaWidth - line.bitmap.width) / 2
                Alignment.RIGHT -> mediaWidth - LAYOUT.RIGHT_MARGIN - line.bitmap.width
            }

        drawBitmap(
            bitmap = line.bitmap,
            x = x.coerceAtLeast(LAYOUT.LEFT_MARGIN),
            y = currentY,
        )

        return currentY + line.bitmap.height + LAYOUT.ELEMENT_SPACING
    }

    // LAYOUT CONSTANTS

    /**
     * Layout constants for receipt rendering.
     * All values in dots (203 DPI standard for thermal printers).
     */
    private object LAYOUT {
        const val TOP_MARGIN = 50
        const val LEFT_MARGIN = 30 // Reduced for 80mm paper
        const val RIGHT_MARGIN = 30 // Reduced for 80mm paper
        const val LINE_HEIGHT = 30
        const val LINE_SPACING = 8
        const val SECTION_SPACING = 16
        const val ELEMENT_SPACING = 16
        const val SEPARATOR_FONT_SIZE = 20
        const val QR_SIZE_MULTIPLIER = 20
    }
}
