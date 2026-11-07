package sincpro.expo.printer.adapters

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import com.bixolon.labelprinter.BixolonLabelPrinter
import sincpro.expo.printer.domain.Media
import sincpro.expo.printer.domain.MediaType
import sincpro.expo.printer.domain.Orientation
import sincpro.expo.printer.domain.PrintCommand
import sincpro.expo.printer.domain.PrintJob
import sincpro.expo.printer.domain.PrinterConfig
import sincpro.expo.printer.domain.PrinterErrorCode
import sincpro.expo.printer.domain.PrinterException
import sincpro.expo.printer.domain.PrinterInfo
import sincpro.expo.printer.domain.QrErrorCorrection
import sincpro.expo.printer.domain.Speed

private const val TAG = "SincproPrinter"

private const val INTERFACE_BLUETOOTH = 0
private const val INTERFACE_USB = 2
private const val UNUSED_PORT = 9100
private const val LAST_ERROR_UNSUPPORTED_MODEL = 1007
private const val CONTINUOUS_TAIL_DOTS = 100
private const val IMAGE_LEVEL = 50
private const val AUTO_CUTTER_EVERY_LABEL = 1

/** Raw first status byte (manual §3-24): each bit is one printer condition. */
data class StatusFlags(val byte: Int) {
    val paperEmpty get() = byte and 0x80 != 0
    val coverOpen get() = byte and 0x40 != 0
    val cutterJammed get() = byte and 0x20 != 0
    val overheated get() = byte and 0x10 != 0
    val mediaError get() = byte and 0x08 != 0
    val ribbonEnd get() = byte and 0x04 != 0
}

/**
 * Wraps BixolonLabelPrinter (Label SDK 2.1.2). The SDK is not thread-safe and every call blocks,
 * so callers must use it from a single thread; [PrinterService] guarantees that.
 *
 * Verified in the SDK bytecode: `connect` returns the model name on success and "" on any
 * failure, and its timeout is in milliseconds (0 means wait forever).
 */
class BixolonClient(context: Context) {
    private val sdkEvents = HandlerThread("SincproPrinterSdkEvents").apply { start() }

    // Context: the SDK requires a Handler; connection state is owned by PrinterService because the
    // SDK sends nothing when the printer drops, so these messages are only logged.
    private val handler =
        Handler(sdkEvents.looper) { message ->
            if (message.what == BixolonLabelPrinter.MESSAGE_STATE_CHANGE) {
                Log.d(TAG, "Bixolon SDK state=${message.arg1}")
            }
            true
        }

    private val printer = BixolonLabelPrinter(context, handler, sdkEvents.looper)

    val isLinkUp: Boolean
        get() = printer.isConnected()

    private fun connect(target: String, timeoutMs: Int, attempt: () -> String?): String {
        val startedAt = SystemClock.elapsedRealtime()
        val model = attempt()
        if (!model.isNullOrEmpty() && printer.isConnected()) return model

        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val lastError = printer.getLastError()
        throw when {
            lastError == LAST_ERROR_UNSUPPORTED_MODEL ->
                PrinterException(PrinterErrorCode.UNSUPPORTED_MODEL, "$target is not a supported Bixolon label printer")
            timeoutMs > 0 && elapsed >= timeoutMs ->
                PrinterException(PrinterErrorCode.CONNECT_TIMEOUT, "$target did not answer within ${timeoutMs}ms")
            else ->
                PrinterException(PrinterErrorCode.CONNECT_FAILED, "Could not connect to $target (SDK error $lastError)")
        }
    }

    fun connectBluetooth(address: String, timeoutMs: Int): String =
        connect("Bluetooth $address", timeoutMs) {
            printer.connect(address, INTERFACE_BLUETOOTH, UNUSED_PORT, timeoutMs)
        }

    fun connectWifi(ip: String, port: Int, timeoutMs: Int): String =
        connect("WiFi $ip:$port", timeoutMs) { printer.connect(ip, port, timeoutMs) }

    fun connectUsb(timeoutMs: Int): String =
        connect("USB printer", timeoutMs) { printer.connect("", INTERFACE_USB, UNUSED_PORT, timeoutMs) }

    fun disconnect() {
        runCatching { printer.disconnect() }.onFailure { Log.w(TAG, "Disconnect failed", it) }
    }

    fun release() {
        disconnect()
        sdkEvents.quitSafely()
    }

    fun readStatus(): StatusFlags? = printer.getStatus(false).firstOrNull()?.let { StatusFlags(it.toInt() and 0xFF) }

    private fun information(type: Int): String =
        printer.getPrinterInformation(type)
            .toString(Charsets.US_ASCII)
            .filter { it.code in 0x20..0x7E }
            .trim()

    /** The SDK reads the dpi from its device database; older builds reported a 2/3/6 code. */
    fun dpi(): Int =
        when (val raw = printer.getPrinterDpi()) {
            in 150..700 -> raw
            3 -> 300
            6 -> 600
            else -> 203
        }

    fun readInfo(): PrinterInfo =
        PrinterInfo(
            model = information(BixolonLabelPrinter.PRINTER_INFORMATION_MODEL_NAME),
            firmware = information(BixolonLabelPrinter.PRINTER_INFORMATION_FIRMWARE_VERSION),
            serial = information(BixolonLabelPrinter.PRINTER_INFORMATION_SERIAL_NUMBER),
            dpi = dpi(),
        )

    private fun speedCode(speed: Speed): Int =
        when (speed) {
            Speed.SLOW -> BixolonLabelPrinter.SPEED_25IPS
            Speed.MEDIUM -> BixolonLabelPrinter.SPEED_50IPS
            Speed.FAST -> BixolonLabelPrinter.SPEED_70IPS
            Speed.EXTRA_FAST -> BixolonLabelPrinter.SPEED_80IPS
        }

    /**
     * Context: best effort, like 1.x — a setting the model rejects is logged and the rest still
     * apply, so a connection never fails because of configuration.
     */
    fun applyConfig(config: PrinterConfig) {
        val orientation =
            when (config.orientation) {
                Orientation.TOP_TO_BOTTOM -> BixolonLabelPrinter.ORIENTATION_TOP_TO_BOTTOM
                Orientation.BOTTOM_TO_TOP -> BixolonLabelPrinter.ORIENTATION_BOTTOM_TO_TOP
            }
        val results =
            listOfNotNull(
                "setMargin" to printer.setMargin(config.marginLeft, config.marginTop),
                "setDensity" to printer.setDensity(config.density.level),
                config.speed?.let { "setSpeed" to printer.setSpeed(speedCode(it)) },
                "setOrientation" to printer.setOrientation(orientation),
                "setAutoCutter" to printer.setAutoCutter(config.autoCutter, AUTO_CUTTER_EVERY_LABEL),
            )
        results.filter { (_, code) -> code != 0 }.forEach { (operation, code) ->
            Log.w(TAG, "$operation rejected by the printer (code $code)")
        }
    }

    private fun ensureOk(operation: String, code: Int) {
        if (code != 0) throw PrinterException(PrinterErrorCode.PRINT_FAILED, "$operation failed (SDK code $code)")
    }

    private fun eccCode(level: QrErrorCorrection): Int =
        when (level) {
            QrErrorCorrection.L -> BixolonLabelPrinter.ECC_LEVEL_7
            QrErrorCorrection.M -> BixolonLabelPrinter.ECC_LEVEL_15
            QrErrorCorrection.Q -> BixolonLabelPrinter.ECC_LEVEL_25
            QrErrorCorrection.H -> BixolonLabelPrinter.ECC_LEVEL_30
        }

    private fun setPageLength(media: Media, contentBottom: Int) {
        val code =
            when (media.type) {
                MediaType.CONTINUOUS ->
                    printer.setLength(contentBottom + CONTINUOUS_TAIL_DOTS, 0, BixolonLabelPrinter.MEDIA_TYPE_CONTINUOUS, 0)
                MediaType.GAP ->
                    printer.setLength(media.heightDots, media.gapDots, BixolonLabelPrinter.MEDIA_TYPE_GAP, 0)
                MediaType.BLACK_MARK ->
                    printer.setLength(media.heightDots, media.gapDots, BixolonLabelPrinter.MEDIA_TYPE_BLACK_MARK, 0)
            }
        ensureOk("setLength", code)
    }

    private fun draw(command: PrintCommand, bitmap: Bitmap?) {
        val code =
            when (command) {
                is PrintCommand.Text ->
                    printer.drawText(
                        command.text, command.x, command.y, command.font, 1, 1, 0,
                        BixolonLabelPrinter.ROTATION_NONE, false, command.bold, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE,
                    )
                is PrintCommand.Qr ->
                    printer.drawQrCode(
                        command.data, command.x, command.y, BixolonLabelPrinter.QR_CODE_MODEL2,
                        eccCode(command.errorCorrection), command.size, BixolonLabelPrinter.ROTATION_NONE,
                    )
                is PrintCommand.Barcode ->
                    printer.draw1dBarcode(
                        command.data, command.x, command.y, command.symbology, command.narrowBarWidth,
                        command.wideBarWidth, command.barHeight, BixolonLabelPrinter.ROTATION_NONE,
                        if (command.showText) BixolonLabelPrinter.HRI_BELOW_FONT_SIZE_1 else BixolonLabelPrinter.HRI_NOT_PRINTED,
                        0,
                    )
                is PrintCommand.Image ->
                    printer.drawBitmap(requireNotNull(bitmap), command.x, command.y, command.width, IMAGE_LEVEL, true)
                is PrintCommand.PdfPage ->
                    printer.drawBitmap(requireNotNull(bitmap), command.x, command.y, bitmap.width, IMAGE_LEVEL, true)
            }
        ensureOk(command::class.simpleName ?: "draw", code)
    }

    /** The SDK scales images to the command width keeping the aspect ratio; PDF pages arrive pre-scaled. */
    private fun drawnHeight(command: PrintCommand, bitmap: Bitmap?): Int =
        when {
            bitmap == null -> command.height
            command is PrintCommand.Image -> bitmap.height * command.width / bitmap.width
            else -> bitmap.height
        }

    /**
     * Context: same transaction sequence as 1.x; images and PDF pages arrive already decoded,
     * keyed by command index.
     *
     * 1. Reset the buffer and open the transaction.
     * 2. Set width and page length (continuous pages fit the drawn content).
     * 3. Draw every command, failing on the first rejected one.
     * 4. Final: print the copies and close the transaction.
     */
    fun print(job: PrintJob, bitmaps: Map<Int, Bitmap>) {
        val contentBottom =
            job.commands.withIndex().maxOfOrNull { (index, command) ->
                command.y + drawnHeight(command, bitmaps[index])
            } ?: 0

        printer.clearBuffer()
        printer.beginTransactionPrint()
        try {
            ensureOk("setWidth", printer.setWidth(job.media.widthDots))
            setPageLength(job.media, contentBottom)
            job.commands.forEachIndexed { index, command -> draw(command, bitmaps[index]) }
            ensureOk("print", printer.print(job.copies, 1))
        } finally {
            if (printer.endTransactionPrint() < 0) Log.w(TAG, "Printer did not confirm the end of the transaction")
        }
    }
}
