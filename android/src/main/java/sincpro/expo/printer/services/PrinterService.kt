package sincpro.expo.printer.services

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import sincpro.expo.printer.adapters.AndroidBluetooth
import sincpro.expo.printer.adapters.BixolonClient
import sincpro.expo.printer.domain.BluetoothDevice
import sincpro.expo.printer.domain.Connection
import sincpro.expo.printer.domain.ConnectionChangeReason
import sincpro.expo.printer.domain.ConnectionState
import sincpro.expo.printer.domain.PrintCommand
import sincpro.expo.printer.domain.PrintJob
import sincpro.expo.printer.domain.PrinterConfig
import sincpro.expo.printer.domain.PrinterErrorCode
import sincpro.expo.printer.domain.PrinterException
import sincpro.expo.printer.domain.PrinterInfo
import sincpro.expo.printer.domain.PrinterStatus
import sincpro.expo.printer.infrastructure.BitmapDecoder
import sincpro.expo.printer.infrastructure.PdfRasterizer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val LINK_CHECK_DELAY_MS = 500L
private const val DEFAULT_DPI = 203

/**
 * Owns the printer session. Every Bixolon SDK call runs on one dedicated thread ([enqueue]),
 * because the SDK is not thread-safe and overlapping connects used to leave orphan sockets that
 * kept the printer busy. The connection state is owned here and verified against the SDK link.
 */
class PrinterService(context: Context, private val onConnectionChange: (Connection) -> Unit) {
    private val sdkThread: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "SincproPrinterSdk") }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = BixolonClient(context)
    private val bluetooth = AndroidBluetooth(context)
    private val pdf = PdfRasterizer(context.cacheDir)

    @Volatile private var connection = Connection.IDLE

    @Volatile private var dpi = DEFAULT_DPI

    @Volatile var config = PrinterConfig()
        private set

    fun <T> enqueue(task: PrinterService.() -> T, done: (Result<T>) -> Unit) {
        sdkThread.execute { done(runCatching { task() }) }
    }

    private fun publish(next: Connection) {
        synchronized(this) {
            if (next == connection) return
            connection = next
        }
        onConnectionChange(next)
    }

    private fun onLinkLost(address: String?) {
        val current = connection
        if (current.state != ConnectionState.CONNECTED || !current.address.equals(address, ignoreCase = true)) return
        bluetooth.unwatchLink()
        publish(Connection(ConnectionState.DISCONNECTED, current.address, ConnectionChangeReason.LINK_LOST))
        sdkThread.execute { client.disconnect() }
    }

    /** Context: the ACL broadcast can race the SDK's own cleanup, so the link is checked a moment later. */
    private fun verifyLinkSoon(address: String) {
        mainHandler.postDelayed({ if (!client.isLinkUp) onLinkLost(address) }, LINK_CHECK_DELAY_MS)
    }

    private fun refresh(): Connection {
        val current = connection
        if (current.state == ConnectionState.CONNECTED && !client.isLinkUp) onLinkLost(current.address)
        return connection
    }

    private fun requireConnected() {
        if (refresh().state != ConnectionState.CONNECTED) {
            throw PrinterException(PrinterErrorCode.NOT_CONNECTED, "No printer is connected")
        }
    }

    /**
     * Context: runs on the SDK thread.
     *
     * 1. Reuse the session when already connected to the same printer.
     * 2. Drop any previous or half-open session, so no orphan socket keeps the printer busy.
     * 3. Connect, read the dpi and apply the stored configuration.
     * 4. Final: publish CONNECTED, or DISCONNECTED with the error on failure.
     */
    private fun connect(address: String, attempt: () -> Unit) {
        val current = refresh()
        if (current.state == ConnectionState.CONNECTED && current.address.equals(address, ignoreCase = true)) return

        bluetooth.unwatchLink()
        client.disconnect()
        publish(Connection(ConnectionState.CONNECTING, address, null))
        try {
            attempt()
            dpi = client.dpi()
            client.applyConfig(config)
            publish(Connection(ConnectionState.CONNECTED, address, ConnectionChangeReason.CONNECTED))
        } catch (error: PrinterException) {
            client.disconnect()
            publish(Connection(ConnectionState.DISCONNECTED, address, ConnectionChangeReason.CONNECT_FAILED))
            throw error
        }
    }

    fun pairedDevices(): List<BluetoothDevice> = bluetooth.pairedDevices()

    fun connection(): Connection = refresh()

    fun isConnected(): Boolean = refresh().state == ConnectionState.CONNECTED

    fun dpi(): Int = dpi

    fun pdfPageCount(base64: String): Int = pdf.pageCount(base64)

    fun connectBluetooth(address: String, timeoutMs: Int) =
        connect(address) {
            client.connectBluetooth(address, timeoutMs)
            bluetooth.watchLink(address) { verifyLinkSoon(address) }
        }

    fun connectWifi(ip: String, port: Int, timeoutMs: Int) = connect("$ip:$port") { client.connectWifi(ip, port, timeoutMs) }

    fun connectUsb(timeoutMs: Int) = connect("usb") { client.connectUsb(timeoutMs) }

    fun disconnect() {
        bluetooth.unwatchLink()
        client.disconnect()
        publish(Connection(ConnectionState.DISCONNECTED, connection.address, ConnectionChangeReason.USER_DISCONNECT))
    }

    fun status(): PrinterStatus {
        val state = refresh().state
        if (state != ConnectionState.CONNECTED) {
            return PrinterStatus(state, false, false, false, false, false, false, null)
        }
        val flags =
            client.readStatus()
                ?: return PrinterStatus(state, false, false, false, false, false, false, "Status read failed")
        return PrinterStatus(
            connectionState = state,
            hasPaper = !flags.paperEmpty,
            isCoverOpen = flags.coverOpen,
            isOverheated = flags.overheated,
            isCutterJammed = flags.cutterJammed,
            isRibbonEnd = flags.ribbonEnd,
            hasMediaError = flags.mediaError,
            errorMessage = null,
        )
    }

    fun info(): PrinterInfo {
        requireConnected()
        return client.readInfo()
    }

    fun setConfig(next: PrinterConfig) {
        config = next
        if (refresh().state == ConnectionState.CONNECTED) client.applyConfig(next)
    }

    private fun decodeImages(commands: List<PrintCommand>): Map<Int, Bitmap> =
        commands.withIndex().mapNotNull { (index, command) ->
            when (command) {
                is PrintCommand.Image ->
                    index to (
                        BitmapDecoder.decode(command.base64)
                            ?: throw PrinterException(PrinterErrorCode.INVALID_ARGUMENT, "The image could not be decoded")
                    )
                is PrintCommand.PdfPage ->
                    index to (
                        pdf.render(command.base64, command.page, command.width)
                            ?: throw PrinterException(PrinterErrorCode.INVALID_ARGUMENT, "PDF page ${command.page} could not be rendered")
                    )
                else -> null
            }
        }.toMap()

    fun print(job: PrintJob) {
        requireConnected()
        val bitmaps = decodeImages(job.commands)
        try {
            client.print(job, bitmaps)
        } catch (error: PrinterException) {
            refresh()
            throw error
        } finally {
            bitmaps.values.forEach(Bitmap::recycle)
        }
    }

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        bluetooth.unwatchLink()
        sdkThread.execute { client.release() }
        sdkThread.shutdown()
    }
}
