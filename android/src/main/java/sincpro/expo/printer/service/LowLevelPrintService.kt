package sincpro.expo.printer.service

import android.graphics.Bitmap
import android.util.Log
import sincpro.expo.printer.domain.Alignment
import sincpro.expo.printer.domain.FontSize
import sincpro.expo.printer.domain.IPrinterAdapter
import sincpro.expo.printer.domain.MediaConfig
import sincpro.expo.printer.infrastructure.orchestration.EventBus
import sincpro.expo.printer.infrastructure.orchestration.PrintJobContext
import sincpro.expo.printer.infrastructure.orchestration.PrintJobOrchestrator

/**
 * SERVICE - Low Level Print Service
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  SELF-CONTAINED SERVICE                                                 │
 * │                                                                         │
 * │  This service exposes PRIMITIVE print operations for advanced users     │
 * │  who need fine-grained control over the print output.                   │
 * │                                                                         │
 * │  Operations:                                                            │
 * │  • Draw text at absolute X,Y position                                   │
 * │  • Draw QR codes                                                        │
 * │  • Draw barcodes (Code128, Code39, EAN13, etc.)                         │
 * │  • Draw bitmaps/images                                                  │
 * │  • Draw lines and boxes                                                 │
 * │                                                                         │
 * │  Default media: 80mm continuous paper                                   │
 * │                                                                         │
 * │  Dependencies:                                                          │
 * │  • IPrinterAdapter (Adapter) - Printer-specific implementation          │
 * │  • PrintJobOrchestrator (Infrastructure) - Mutex/lifecycle              │
 * │  • EventBus (Infrastructure) - Event publishing                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * USAGE (Context Manager Pattern):
 *   lowLevelService.withPrintSession {
 *       // Direct control over every element
 *       drawText("Hello", x = 50, y = 50, fontSize = 30)
 *       drawQR("https://example.com", x = 100, y = 100, size = 5)
 *       drawBarcode("123456789", x = 50, y = 200, type = BarcodeType.CODE128)
 *       print()
 *   }
 */
class LowLevelPrintService(
    // Adapter - Printer-specific implementation
    private val printerAdapter: IPrinterAdapter,
    // Infrastructure - Job orchestration (mutex, lifecycle)
    private val orchestrator: PrintJobOrchestrator,
    // Infrastructure - Event publishing
    private val eventBus: EventBus,
) {
    // CONTEXT MANAGER PATTERN

    /**
     * Execute a print session with proper lifecycle management.
     *
     * Inside the block, you have access to all primitive operations:
     * - drawText(), drawQR(), drawBarcode(), drawBitmap()
     * - drawLine(), drawBox()
     * - print()
     *
     * The session automatically handles:
     * - Mutex lock (only 1 job at a time)
     * - Printer initialization
     * - Media configuration
     * - Buffer clearing
     * - Transaction management
     *
     * @param mediaConfig Media configuration (default: 80mm continuous)
     * @param block Code to execute within the print session
     * @return Result from block execution
     *
     * EXAMPLE:
     *   lowLevelService.withPrintSession(MediaConfig.continuous80mm()) {
     *       drawText("Title", 50, 50, 40, bold = true)
     *       drawText("Line 2", 50, 100, 24)
     *       drawQR("DATA123", 100, 150, 5)
     *       print()
     *   }
     */
    suspend fun <T> withPrintSession(
        mediaConfig: MediaConfig = MediaConfig.continuous80mm(),
        block: suspend PrintJobContext.() -> T,
    ): Result<T> {
        Log.d(this::class.simpleName, "🚀 Starting low-level print session")
        return orchestrator.executeJob(mediaConfig = mediaConfig, block = block)
    }

    // DIRECT OPERATIONS (Without session - use with caution)

    /**
     * Draw text at absolute position.
     *
     * NOTE: Prefer using withPrintSession() for proper lifecycle management.
     * This method is exposed for edge cases where manual control is needed.
     *
     * @param text Text content
     * @param x Horizontal position in dots
     * @param y Vertical position in dots
     * @param fontSize Font size in dots
     * @param bold Bold text
     * @param alignment Text alignment (0=left, 1=center, 2=right)
     */
    suspend fun drawTextDirect(
        text: String,
        x: Int,
        y: Int,
        fontSize: Int = 24,
        bold: Boolean = false,
        alignment: Int = 0,
    ): Result<Unit> {
        Log.d(this::class.simpleName, "📝 Direct draw text: '$text' at ($x, $y)")
        return printerAdapter.drawText(text, x, y, fontSize, bold, alignment)
    }

    /**
     * Draw QR code at absolute position.
     *
     * @param data Data to encode
     * @param x Horizontal position in dots
     * @param y Vertical position in dots
     * @param size QR size (1-10, affects resolution)
     */
    suspend fun drawQRDirect(
        data: String,
        x: Int,
        y: Int,
        size: Int = 5,
    ): Result<Unit> {
        Log.d(this::class.simpleName, "📱 Direct draw QR: '$data' at ($x, $y)")
        return printerAdapter.drawQR(data, x, y, size)
    }

    /**
     * Draw bitmap at absolute position.
     *
     * @param bitmap Bitmap image
     * @param x Horizontal position in dots
     * @param y Vertical position in dots
     */
    suspend fun drawBitmapDirect(
        bitmap: Bitmap,
        x: Int,
        y: Int,
    ): Result<Unit> {
        Log.d(this::class.simpleName, "🖼️ Direct draw bitmap: ${bitmap.width}x${bitmap.height} at ($x, $y)")
        return printerAdapter.drawBitmap(bitmap, x, y)
    }

    // UTILITY METHODS

    /**
     * Get printer status.
     */
    suspend fun getPrinterStatus(): Result<PrinterStatus> {
        Log.d(this::class.simpleName, "📊 Getting printer status")
        return printerAdapter.getStatus()
    }

    /**
     * Clear print buffer.
     */
    suspend fun clearBuffer(): Result<Unit> {
        Log.d(this::class.simpleName, "🧹 Clearing print buffer")
        return printerAdapter.clearBuffer()
    }

    // CONSTANTS

    companion object {
        /**
         * Common font sizes in dots (203 DPI)
         */
        object FontSizes {
            const val TINY = 16
            const val SMALL = 20
            const val MEDIUM = 24
            const val LARGE = 32
            const val XLARGE = 40
            const val TITLE = 48
        }

        /**
         * Common media widths in dots
         */
        object MediaWidths {
            const val MM_58 = 464 // 58mm paper
            const val MM_72 = 576 // 72mm paper
            const val MM_80 = 640 // 80mm paper (DEFAULT)
            const val MM_104 = 832 // 104mm paper
        }
    }
}
