package com.sincpro.printer.adapter

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.bixolon.labelprinter.BixolonLabelPrinter
import com.sincpro.printer.domain.Alignment
import com.sincpro.printer.domain.BarcodeType
import com.sincpro.printer.domain.ConnectionConfig
import com.sincpro.printer.domain.ConnectionState
import com.sincpro.printer.domain.ConnectionType
import com.sincpro.printer.domain.CutterType
import com.sincpro.printer.domain.FontSize
import com.sincpro.printer.domain.IPrinter
import com.sincpro.printer.domain.MediaConfig
import com.sincpro.printer.domain.MediaType
import com.sincpro.printer.domain.Orientation
import com.sincpro.printer.domain.PrinterConfig
import com.sincpro.printer.domain.PrinterInfo
import com.sincpro.printer.domain.PrinterStatus
import com.sincpro.printer.domain.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ADAPTER - Bixolon Printer Adapter
 * 
 * Implements IPrinter interface using Bixolon SDK
 * Maps domain models to Bixolon SDK constants
 */
class BixolonPrinterAdapter(private val context: Context) : IPrinter {

    private var printer: BixolonLabelPrinter? = null
    private var connected = false
    private var currentDpi: Int = 203

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == BixolonLabelPrinter.MESSAGE_STATE_CHANGE) {
                connected = msg.arg1 == BixolonLabelPrinter.STATE_CONNECTED
            }
        }
    }

    
    private fun FontSize.toBixolon() = when (this) {
        FontSize.SMALL -> BixolonLabelPrinter.FONT_SIZE_8
        FontSize.MEDIUM -> BixolonLabelPrinter.FONT_SIZE_12
        FontSize.LARGE -> BixolonLabelPrinter.FONT_SIZE_20
        FontSize.XLARGE -> BixolonLabelPrinter.FONT_SIZE_30
    }

    private fun Alignment.toBixolonTextAlignment() = when (this) {
        Alignment.LEFT -> BixolonLabelPrinter.TEXT_ALIGNMENT_LEFT
        Alignment.RIGHT -> BixolonLabelPrinter.TEXT_ALIGNMENT_RIGHT
        Alignment.CENTER -> BixolonLabelPrinter.TEXT_ALIGNMENT_NONE
    }

    private fun BarcodeType.toBixolon() = when (this) {
        BarcodeType.CODE128 -> BixolonLabelPrinter.BARCODE_CODE128
        BarcodeType.CODE39 -> BixolonLabelPrinter.BARCODE_CODE39
        BarcodeType.EAN13 -> BixolonLabelPrinter.BARCODE_EAN13
        BarcodeType.EAN8 -> BixolonLabelPrinter.BARCODE_EAN8
        BarcodeType.UPCA -> BixolonLabelPrinter.BARCODE_UPC_A
        BarcodeType.UPCE -> BixolonLabelPrinter.BARCODE_UPC_E
        BarcodeType.CODE93 -> BixolonLabelPrinter.BARCODE_CODE93
        BarcodeType.CODABAR -> BixolonLabelPrinter.BARCODE_CODABAR
    }

    private fun Orientation.toBixolonOrientation() = when (this) {
        Orientation.TOP_TO_BOTTOM -> BixolonLabelPrinter.ORIENTATION_TOP_TO_BOTTOM
        Orientation.LEFT_TO_RIGHT -> BixolonLabelPrinter.ORIENTATION_TOP_TO_BOTTOM
        Orientation.BOTTOM_TO_TOP -> BixolonLabelPrinter.ORIENTATION_BOTTOM_TO_TOP
        Orientation.RIGHT_TO_LEFT -> BixolonLabelPrinter.ORIENTATION_BOTTOM_TO_TOP
    }


    override suspend fun connect(config: ConnectionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (printer == null) {
                printer = BixolonLabelPrinter(context, handler, Looper.getMainLooper())
            }
            val p = printer!!
            val result = when (config.type) {
                ConnectionType.BLUETOOTH -> p.connect(config.address)
                ConnectionType.WIFI -> p.connect(config.address, config.port, config.timeoutMs.toInt())
                ConnectionType.USB -> p.connect()
            }
            if (result != null && !result.contains("fail", ignoreCase = true)) {
                connected = true
                Result.success(Unit)
            } else {
                Result.failure(Exception("Connection failed: $result"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            printer?.disconnect()
            connected = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isConnected() = connected

    override suspend fun getStatus(): Result<PrinterStatus> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.success(PrinterStatus.disconnected())
            
            val statusBytes = p.getStatus(false)
            if (statusBytes == null || statusBytes.isEmpty()) {
                return@withContext Result.success(PrinterStatus.error("Failed to read status"))
            }
            
            val byte1 = statusBytes.getOrNull(0)?.toInt() ?: 0
            val byte2 = statusBytes.getOrNull(1)?.toInt() ?: 0
            
            val paperEmpty = (byte1 and 0x80) != 0
            val coverOpen = (byte1 and 0x40) != 0
            val cutterJammed = (byte1 and 0x20) != 0
            val tphOverheat = (byte1 and 0x10) != 0
            val ribbonError = (byte1 and 0x04) != 0
            
            val bufferBuilding = (byte2 and 0x80) != 0
            val printing = (byte2 and 0x40) != 0
            val paused = (byte2 and 0x20) != 0
            
            val hasError = paperEmpty || coverOpen || cutterJammed || tphOverheat || ribbonError
            val errorMessage = buildErrorMessage(paperEmpty, coverOpen, cutterJammed, tphOverheat, ribbonError)
            
            Result.success(PrinterStatus(
                connectionState = ConnectionState.CONNECTED,
                hasPaper = !paperEmpty,
                isCoverOpen = coverOpen,
                isOverheated = tphOverheat,
                isCutterJammed = cutterJammed,
                isRibbonError = ribbonError,
                isPrinting = printing,
                isPaused = paused,
                isBufferBuilding = bufferBuilding,
                hasError = hasError,
                errorMessage = errorMessage
            ))
        } catch (e: Exception) {
            Result.success(PrinterStatus.error("Status read failed: ${e.message}"))
        }
    }
    
    private fun buildErrorMessage(
        paperEmpty: Boolean,
        coverOpen: Boolean,
        cutterJammed: Boolean,
        overheat: Boolean,
        ribbonError: Boolean
    ): String? {
        val errors = mutableListOf<String>()
        if (paperEmpty) errors.add("Paper empty")
        if (coverOpen) errors.add("Cover open")
        if (cutterJammed) errors.add("Cutter jammed")
        if (overheat) errors.add("Overheated")
        if (ribbonError) errors.add("Ribbon error")
        return if (errors.isEmpty()) null else errors.joinToString(", ")
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
            Result.success(PrinterInfo(model, firmware, serial, currentDpi, true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDpi(): Int = currentDpi


    override suspend fun configure(config: PrinterConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
            p.setMargin(config.marginLeft, config.marginTop)
            p.setDensity(config.density.level)
            p.setSpeed(config.speed.ips)
            p.setOrientation(config.orientation.toBixolonOrientation())
            if (config.autoCutter.enabled) {
                val cutType = if (config.autoCutter.type == CutterType.FULL) 1 else 0
                p.setAutoCutter(true, cutType)
            } else {
                p.setAutoCutter(false, 0)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun feedPaper(dots: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
            // setOffset moves paper forward (positive) or backward (negative)
            p.setOffset(dots)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cutPaper(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
            p.setAutoCutter(true, 1)
            p.print(1, 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun beginTransaction(media: MediaConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
            p.beginTransactionPrint()
            
            // Set width for paper feed calculation (required for proper paper advance)
            p.setWidth(media.widthDots)
            
            // Only call setLength() for LABEL mode (GAP/BLACK_MARK)
            // CONTINUOUS mode should NOT call setLength() (as per Bixolon sample)
            if (media.type != MediaType.CONTINUOUS) {
                val mediaCode = when (media.type) {
                    MediaType.GAP -> BixolonLabelPrinter.MEDIA_TYPE_GAP
                    MediaType.BLACK_MARK -> BixolonLabelPrinter.MEDIA_TYPE_BLACK_MARK
                    else -> BixolonLabelPrinter.MEDIA_TYPE_GAP
                }
                p.setLength(media.heightDots, media.gapDots, mediaCode, 0)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun endTransaction(copies: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
            p.print(copies, 1)
            p.endTransactionPrint()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun drawText(text: String, x: Int, y: Int, style: TextStyle): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
                p.drawText(
                    text,
                    x,
                    y,
                    style.fontSize.toBixolon(),
                    1,
                    1,
                    0,
                    BixolonLabelPrinter.ROTATION_NONE,
                    false,
                    style.bold,
                    style.alignment.toBixolonTextAlignment()
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun drawQR(data: String, x: Int, y: Int, size: Int): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
                p.drawQrCode(
                    data, x, y,
                    BixolonLabelPrinter.QR_CODE_MODEL2,
                    BixolonLabelPrinter.ECC_LEVEL_15,
                    size,
                    BixolonLabelPrinter.ROTATION_NONE
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun drawBarcode(
        data: String, x: Int, y: Int,
        type: BarcodeType, width: Int, height: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
            p.draw1dBarcode(
                data, x, y,
                type.toBixolon(),
                width, 6,
                height,
                BixolonLabelPrinter.ROTATION_NONE,
                BixolonLabelPrinter.HRI_BELOW_BARCODE,
                0
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun drawBitmap(bitmap: Bitmap, x: Int, y: Int): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
                p.drawBitmap(bitmap, x, y, bitmap.width, 50, true)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
