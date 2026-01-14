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
import com.sincpro.printer.domain.Receipt
import com.sincpro.printer.domain.ReceiptLine
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
            val p = printer ?: return@withContext Result.success(
                PrinterStatus(false, false, true, "Not connected")
            )
            val status = p.getStatus(false)
            val hasPaper = status?.getOrNull(0)?.toInt()?.and(0x04) == 0
            val hasError = status?.getOrNull(0)?.toInt()?.and(0x80) != 0
            Result.success(PrinterStatus(connected, hasPaper, hasError, null))
        } catch (e: Exception) {
            Result.success(PrinterStatus(connected, true, false, null))
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
            p.setBackFeedOption(true, dots)
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


    override suspend fun print(receipt: Receipt, media: MediaConfig): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                beginTransaction(media).getOrThrow()
                var y = 50
                val allLines = receipt.header + receipt.body + receipt.footer
                for (line in allLines) {
                    y = renderLine(line, y, media.widthDots)
                }
                endTransaction(1).getOrThrow()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun renderLine(line: ReceiptLine, y: Int, width: Int): Int {
        return when (line) {
            is ReceiptLine.Text -> {
                val x = calculateX(line.alignment, width, line.content.length * 10)
                drawText(line.content, x, y, TextStyle(line.fontSize, line.bold, line.alignment))
                y + 30
            }
            is ReceiptLine.KeyValue -> {
                drawText(line.key, 10, y, TextStyle(line.fontSize, line.bold))
                drawText(line.value, width - 10 - (line.value.length * 10), y, TextStyle(line.fontSize, line.bold))
                y + 30
            }
            is ReceiptLine.QR -> {
                val qrWidth = line.size * 20
                val x = calculateX(line.alignment, width, qrWidth)
                drawQR(line.data, x, y, line.size)
                y + qrWidth + 20
            }
            is ReceiptLine.Barcode -> {
                val barcodeWidth = line.data.length * 10
                val x = calculateX(line.alignment, width, barcodeWidth)
                drawBarcode(line.data, x, y, line.type, line.width, line.height)
                y + line.height + 30
            }
            is ReceiptLine.Separator -> {
                val sep = line.char.toString().repeat(line.length)
                drawText(sep, 10, y, TextStyle(FontSize.SMALL))
                y + 20
            }
            is ReceiptLine.Space -> y + (line.lines * 20)
            is ReceiptLine.Image -> {
                val x = calculateX(line.alignment, width, line.bitmap.width)
                drawBitmap(line.bitmap, x, y)
                y + line.bitmap.height + 10
            }
        }
    }

    private fun calculateX(alignment: Alignment, width: Int, contentWidth: Int): Int {
        return when (alignment) {
            Alignment.LEFT -> 10
            Alignment.CENTER -> (width - contentWidth) / 2
            Alignment.RIGHT -> width - contentWidth - 10
        }
    }
}
