package sincpro.expo.printer.domain

import android.graphics.Bitmap

/**
 * DOMAIN - Printer Adapter Contract
 *
 * Defines the interface for printer adapters (port in hexagonal architecture).
 * Each printer brand implements this interface.
 */
interface IPrinterAdapter {
    /**
     * Connect to the printer
     */
    suspend fun connect(
        address: String,
        port: Int = 9100,
    ): Result<Unit>

    /**
     * Disconnect from the printer
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * Get printer status
     */
    suspend fun getStatus(): Result<PrinterStatus>

    /**
     * Initialize printer
     */
    suspend fun initializePrinter(): Result<Unit>

    /**
     * Configure print media
     */
    suspend fun configureMedia(config: MediaConfig): Result<Unit>

    /**
     * Clear print buffer
     */
    suspend fun clearBuffer(): Result<Unit>

    /**
     * Begin print transaction
     */
    suspend fun beginTransaction(): Result<Unit>

    /**
     * End print transaction
     */
    suspend fun endTransaction(): Result<Unit>

    /**
     * Draw text at absolute position
     */
    suspend fun drawText(
        text: String,
        x: Int,
        y: Int,
        fontSize: Int,
        bold: Boolean,
        alignment: Int,
    ): Result<Unit>

    /**
     * Draw QR code
     */
    suspend fun drawQR(
        data: String,
        x: Int,
        y: Int,
        size: Int,
    ): Result<Unit>

    /**
     * Draw bitmap image
     */
    suspend fun drawBitmap(
        bitmap: Bitmap,
        x: Int,
        y: Int,
    ): Result<Unit>

    /**
     * Execute print
     */
    suspend fun print(copies: Int): Result<Unit>

    /**
     * Wait for print completion
     */
    suspend fun waitForCompletion(timeoutMs: Long): Result<Unit>
}

/**
 * Printer status information
 */
data class PrinterStatus(
    val isConnected: Boolean,
    val isPaperPresent: Boolean,
    val isError: Boolean,
    val errorMessage: String? = null,
)

/**
 * Printer events
 */
sealed class PrinterEvent {
    object OutputComplete : PrinterEvent()

    data class StateChange(
        val state: Int,
    ) : PrinterEvent()

    data class Error(
        val message: String,
    ) : PrinterEvent()
}
