package com.sincpro.printer.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.bixolon.labelprinter.BixolonLabelPrinter
import com.sincpro.printer.domain.Alignment
import com.sincpro.printer.domain.BarcodeType
import com.sincpro.printer.domain.ConnectionConfig
import com.sincpro.printer.domain.ConnectionState
import com.sincpro.printer.domain.ConnectionType
import com.sincpro.printer.domain.FontSize
import com.sincpro.printer.domain.IPrinter
import com.sincpro.printer.domain.MediaConfig
import com.sincpro.printer.domain.MediaType
import com.sincpro.printer.domain.Orientation
import com.sincpro.printer.domain.PrintElement
import com.sincpro.printer.domain.PrinterConfig
import com.sincpro.printer.domain.PrinterInfo
import com.sincpro.printer.domain.PrinterStatus
import com.sincpro.printer.domain.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BixolonPrinterAdapter(private val context: Context) : IPrinter {

    private var printer: BixolonLabelPrinter? = null
    private var connectionState = ConnectionState.DISCONNECTED
    private var currentDpi: Int = 203
    private val printMutex = Mutex()

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BixolonLabelPrinter.MESSAGE_STATE_CHANGE -> {
                    connectionState = when (msg.arg1) {
                        BixolonLabelPrinter.STATE_CONNECTED -> ConnectionState.CONNECTED
                        BixolonLabelPrinter.STATE_CONNECTING -> ConnectionState.CONNECTING
                        else -> ConnectionState.DISCONNECTED
                    }
                }
            }
        }
    }

    // ==================== Conexión ====================

    override suspend fun connect(config: ConnectionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (printer == null) {
                printer = BixolonLabelPrinter(context, handler, Looper.getMainLooper())
            }
            connectionState = ConnectionState.CONNECTING
            val p = printer!!
            val result = when (config.type) {
                ConnectionType.BLUETOOTH -> p.connect(config.address)
                ConnectionType.WIFI -> p.connect(config.address, config.port, config.timeoutMs.toInt())
                ConnectionType.USB -> p.connect()
            }
            if (result != null && !result.contains("fail", ignoreCase = true)) {
                connectionState = ConnectionState.CONNECTED
                Result.success(Unit)
            } else {
                connectionState = ConnectionState.ERROR
                Result.failure(Exception("Connection failed: $result"))
            }
        } catch (e: Exception) {
            connectionState = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            printer?.disconnect()
            connectionState = ConnectionState.DISCONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isConnected() = connectionState == ConnectionState.CONNECTED

    // ==================== Estado ====================

    override suspend fun getStatus(): Result<PrinterStatus> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.success(PrinterStatus.disconnected())

            val statusBytes = p.getStatus(false)
            if (statusBytes == null || statusBytes.isEmpty()) {
                return@withContext Result.success(PrinterStatus.error("Failed to read status"))
            }

            val byte1 = statusBytes.getOrNull(0)?.toInt() ?: 0

            val paperEmpty = (byte1 and 0x80) != 0
            val coverOpen = (byte1 and 0x40) != 0
            val overheat = (byte1 and 0x10) != 0
            val hasError = paperEmpty || coverOpen || overheat

            Result.success(
                PrinterStatus(
                    connectionState = connectionState,
                    hasPaper = !paperEmpty,
                    isCoverOpen = coverOpen,
                    isOverheated = overheat,
                    hasError = hasError,
                    errorMessage = if (hasError) buildErrorMessage(paperEmpty, coverOpen, overheat) else null
                )
            )
        } catch (e: Exception) {
            Result.success(PrinterStatus.error("Status read failed: ${e.message}"))
        }
    }

    override suspend fun getInfo(): Result<PrinterInfo> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
            val model = p.getPrinterInformation(BixolonLabelPrinter.PRINTER_INFORMATION_MODEL_NAME)
                ?.toString(Charsets.UTF_8)?.trim() ?: "Unknown"
            val firmware = p.getPrinterInformation(BixolonLabelPrinter.PRINTER_INFORMATION_FIRMWARE_VERSION)
                ?.toString(Charsets.UTF_8)?.trim() ?: "Unknown"
            val serial = p.getPrinterInformation(BixolonLabelPrinter.PRINTER_INFORMATION_SERIAL_NUMBER)
                ?.toString(Charsets.UTF_8)?.trim() ?: "Unknown"
            currentDpi = p.getPrinterDpi()
            Result.success(PrinterInfo(model, firmware, serial, currentDpi))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDpi(): Int = currentDpi

    // ==================== Configuración ====================

    override suspend fun configure(config: PrinterConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
            p.setMargin(config.marginLeft, config.marginTop)
            p.setDensity(config.density.level)
            p.setSpeed(config.speed.ips)
            p.setOrientation(config.orientation.toSdk())
            p.setAutoCutter(config.autoCutter.enabled, if (config.autoCutter.fullCut) 1 else 0)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Impresión ====================

    override suspend fun print(
        elements: List<PrintElement>,
        media: MediaConfig,
        copies: Int
    ): Result<Unit> = printMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val p = printer ?: return@withContext Result.failure(Exception("Not connected"))

                // 1. Limpiar buffer antes de nueva impresión (evita residuos)
                p.clearBuffer()

                // 2. Iniciar transacción (optimización de red)
                p.beginTransactionPrint()

                // 3. Configurar ancho
                p.setWidth(media.widthDots)

                // 4. Calcular altura del contenido y configurar length
                val contentHeight = calculateContentHeight(elements)
                val heightWithMargin = contentHeight + 100  // Margen extra para feed final
                
                when (media.type) {
                    MediaType.CONTINUOUS -> {
                        // Para papel continuo: usar altura calculada del contenido
                        p.setLength(heightWithMargin, 0, BixolonLabelPrinter.MEDIA_TYPE_CONTINUOUS, 0)
                    }
                    MediaType.GAP -> {
                        p.setLength(media.heightDots, media.gapDots, BixolonLabelPrinter.MEDIA_TYPE_GAP, 0)
                    }
                    MediaType.BLACK_MARK -> {
                        p.setLength(media.heightDots, media.gapDots, BixolonLabelPrinter.MEDIA_TYPE_BLACK_MARK, 0)
                    }
                }

                // 5. Renderizar elementos al buffer
                elements.forEach { element ->
                    renderElement(p, element)
                }

                // 6. Ejecutar impresión
                p.print(copies, 1)

                // 7. Finalizar transacción
                p.endTransactionPrint()

                Result.success(Unit)
            } catch (e: Exception) {
                runCatching { 
                    printer?.endTransactionPrint()
                    printer?.clearBuffer()  // Limpiar buffer en caso de error
                }
                Result.failure(e)
            }
        }
    }

    /**
     * Calculate total height needed for all elements
     */
    private fun calculateContentHeight(elements: List<PrintElement>): Int {
        if (elements.isEmpty()) return 100
        
        var maxY = 0
        elements.forEach { element ->
            val elementBottom = when (element) {
                is PrintElement.Text -> element.y + 30  // Altura aproximada de texto
                is PrintElement.QR -> element.y + (element.size * 20) + 20
                is PrintElement.Barcode -> element.y + element.height + 30
                is PrintElement.Image -> element.y + element.bitmap.height + 10
                is PrintElement.Space -> element.dots
            }
            if (elementBottom > maxY) maxY = elementBottom
        }
        
        return maxY
    }

    // ==================== Utilidades ====================

    override suspend fun feed(dots: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            printer?.setOffset(dots)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            printer?.setAutoCutter(true, 1)
            printer?.print(1, 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Render privado ====================

    private fun renderElement(p: BixolonLabelPrinter, element: PrintElement) {
        when (element) {
            is PrintElement.Text -> p.drawText(
                element.content,
                element.x,
                element.y,
                element.style.fontSize.toSdk(),
                1, 1, 0,
                BixolonLabelPrinter.ROTATION_NONE,
                false,
                element.style.bold,
                element.style.alignment.toSdk()
            )

            is PrintElement.QR -> p.drawQrCode(
                element.data,
                element.x,
                element.y,
                BixolonLabelPrinter.QR_CODE_MODEL2,
                BixolonLabelPrinter.ECC_LEVEL_15,
                element.size,
                BixolonLabelPrinter.ROTATION_NONE
            )

            is PrintElement.Barcode -> p.draw1dBarcode(
                element.data,
                element.x,
                element.y,
                element.type.toSdk(),
                element.width,
                6,
                element.height,
                BixolonLabelPrinter.ROTATION_NONE,
                BixolonLabelPrinter.HRI_BELOW_BARCODE,
                0
            )

            is PrintElement.Image -> p.drawBitmap(
                element.bitmap,
                element.x,
                element.y,
                element.bitmap.width,
                50,
                true
            )

            is PrintElement.Space -> { /* Handled by Y positioning */ }
        }
    }

    // ==================== Mappers ====================

    private fun FontSize.toSdk() = when (this) {
        FontSize.SMALL -> BixolonLabelPrinter.FONT_SIZE_8
        FontSize.MEDIUM -> BixolonLabelPrinter.FONT_SIZE_12
        FontSize.LARGE -> BixolonLabelPrinter.FONT_SIZE_20
        FontSize.XLARGE -> BixolonLabelPrinter.FONT_SIZE_30
    }

    private fun Alignment.toSdk() = when (this) {
        Alignment.LEFT -> BixolonLabelPrinter.TEXT_ALIGNMENT_LEFT
        Alignment.CENTER -> BixolonLabelPrinter.TEXT_ALIGNMENT_NONE
        Alignment.RIGHT -> BixolonLabelPrinter.TEXT_ALIGNMENT_RIGHT
    }

    private fun BarcodeType.toSdk() = when (this) {
        BarcodeType.CODE128 -> BixolonLabelPrinter.BARCODE_CODE128
        BarcodeType.CODE39 -> BixolonLabelPrinter.BARCODE_CODE39
        BarcodeType.EAN13 -> BixolonLabelPrinter.BARCODE_EAN13
        BarcodeType.EAN8 -> BixolonLabelPrinter.BARCODE_EAN8
        BarcodeType.UPCA -> BixolonLabelPrinter.BARCODE_UPC_A
        BarcodeType.UPCE -> BixolonLabelPrinter.BARCODE_UPC_E
        BarcodeType.CODE93 -> BixolonLabelPrinter.BARCODE_CODE93
        BarcodeType.CODABAR -> BixolonLabelPrinter.BARCODE_CODABAR
    }

    private fun Orientation.toSdk() = when (this) {
        Orientation.TOP_TO_BOTTOM -> BixolonLabelPrinter.ORIENTATION_TOP_TO_BOTTOM
        Orientation.BOTTOM_TO_TOP -> BixolonLabelPrinter.ORIENTATION_BOTTOM_TO_TOP
    }

    private fun buildErrorMessage(paperEmpty: Boolean, coverOpen: Boolean, overheat: Boolean): String {
        val errors = mutableListOf<String>()
        if (paperEmpty) errors.add("Paper empty")
        if (coverOpen) errors.add("Cover open")
        if (overheat) errors.add("Overheated")
        return errors.joinToString(", ")
    }
}
