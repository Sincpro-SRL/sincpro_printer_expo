package sincpro.expo.printer.adapter.bixolon

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.bixolon.labelprinter.BixolonLabelPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import sincpro.expo.printer.domain.IPrinterAdapter
import sincpro.expo.printer.domain.MediaConfig
import sincpro.expo.printer.domain.PrinterEvent
import sincpro.expo.printer.domain.PrinterStatus

/**
 * ADAPTER - Bixolon Printer Adapter
 *
 * Wraps the Bixolon Label Printer SDK.
 *
 * Responsibilities:
 * - Map BixolonLabelPrinter (SDK) → IPrinterAdapter (domain)
 * - Translate SDK events → PrinterEvent (domain)
 *
 * NO mutex here (that's infrastructure)
 * NO queue logic here (that's infrastructure)
 * Only translates/maps SDK ↔ domain
 */
class BixolonPrinterAdapter(
    private val context: Context,
) : IPrinterAdapter {
    private var bixolonPrinter: BixolonLabelPrinter? = null
    private val eventChannel = Channel<PrinterEvent>(CONFLATED)

    /**
     * Handler for SDK events
     */
    private val messageHandler =
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    BixolonLabelPrinter.MESSAGE_STATE_CHANGE -> {
                        Log.d(this::class.simpleName, "State changed: ${msg.arg1}")
                        CoroutineScope(Dispatchers.IO).launch {
                            eventChannel.send(PrinterEvent.StateChange(msg.arg1))
                        }
                    }

                    BixolonLabelPrinter.MESSAGE_COMPLETE_PROCESS -> {
                        Log.d(this::class.simpleName, "✅ Output complete")
                        CoroutineScope(Dispatchers.IO).launch {
                            eventChannel.send(PrinterEvent.OutputComplete)
                        }
                    }
                }
            }
        }

    override suspend fun connect(
        address: String,
        port: Int,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (bixolonPrinter == null) {
                    bixolonPrinter = BixolonLabelPrinter(context, messageHandler, null)
                }

                val result = bixolonPrinter?.connect(address, port) ?: false

                if (result) {
                    Log.d(this::class.simpleName, "✅ Connected to $address:$port")
                    Result.success(Unit)
                } else {
                    Log.e(this::class.simpleName, "❌ Connection failed")
                    Result.failure(Exception("Connection failed"))
                }
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Connection error", e)
                Result.failure(e)
            }
        }

    override suspend fun disconnect(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                bixolonPrinter?.disconnect()
                bixolonPrinter = null
                Log.d(this::class.simpleName, "✅ Disconnected")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Disconnect error", e)
                Result.failure(e)
            }
        }

    override suspend fun getStatus(): Result<PrinterStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                val status =
                    PrinterStatus(
                        isConnected = printer.isConnected,
                        isPaperPresent = true, // TODO: Get from SDK
                        isError = false,
                    )

                Result.success(status)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Get status error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun initializePrinter(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                printer.initializePrinter()
                Log.d(this::class.simpleName, "✅ Printer initialized")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Initialize error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun configureMedia(config: MediaConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                // 🔥 CRITICAL FIX: Configure media width and height
                // This was missing and caused gaps in printing
                printer.setWidth(config.widthDots)
                printer.setLength(
                    config.heightDots,
                    config.gapDots,
                    0,
                    config.mediaType.sdkValue,
                )

                Log.d(
                    this::class.simpleName,
                    "✅ Media configured: ${config.widthDots}x${config.heightDots} dots, type=${config.mediaType}",
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Configure media error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun clearBuffer(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                printer.clearBuffer()
                Log.d(this::class.simpleName, "✅ Buffer cleared")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Clear buffer error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun beginTransaction(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                printer.setCharacterset(BixolonLabelPrinter.CHARSET_MULTILINGUAL_CODE)

                Log.d(this::class.simpleName, "✅ Transaction started")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Begin transaction error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun endTransaction(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(this::class.simpleName, "✅ Transaction ended")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ End transaction error", e)
                Result.failure(e)
            }
        }

    override suspend fun drawText(
        text: String,
        x: Int,
        y: Int,
        fontSize: Int,
        bold: Boolean,
        alignment: Int,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                val fontStyle =
                    if (bold) {
                        BixolonLabelPrinter.STYLE_BOLD
                    } else {
                        BixolonLabelPrinter.STYLE_NORMAL
                    }

                printer.drawText(
                    text,
                    x,
                    y,
                    BixolonLabelPrinter.FONT_MONACO,
                    fontSize,
                    fontSize,
                    fontStyle,
                    false,
                )

                Log.d(this::class.simpleName, "✅ Text drawn at ($x, $y): '$text'")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Draw text error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun drawQR(
        data: String,
        x: Int,
        y: Int,
        size: Int,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                printer.draw2DQRCode(
                    data,
                    x,
                    y,
                    BixolonLabelPrinter.QR_MODEL2,
                    size,
                    BixolonLabelPrinter.ROTATION_NONE,
                )

                Log.d(this::class.simpleName, "✅ QR drawn at ($x, $y): '$data'")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Draw QR error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun drawBitmap(
        bitmap: Bitmap,
        x: Int,
        y: Int,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                printer.drawBitmap(
                    bitmap,
                    x,
                    y,
                    BixolonLabelPrinter.BITMAP_WIDTH,
                    350,
                    BixolonLabelPrinter.BITMAP_DITHER_NONE,
                    BixolonLabelPrinter.ROTATION_NONE,
                )

                Log.d(this::class.simpleName, "✅ Bitmap drawn at ($x, $y)")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Draw bitmap error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun print(copies: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val printer =
                    bixolonPrinter
                        ?: return@withContext Result.failure(Exception("Printer not initialized"))

                printer.print(copies, 1)
                Log.d(this::class.simpleName, "✅ Print command sent ($copies copies)")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Print error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun waitForCompletion(timeoutMs: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(timeoutMs) {
                    for (event in eventChannel) {
                        if (event is PrinterEvent.OutputComplete) {
                            Log.d(this::class.simpleName, "✅ Print completed")
                            return@withTimeout Result.success(Unit)
                        }
                    }
                }
                Result.failure(Exception("Timeout waiting for completion"))
            } catch (e: TimeoutCancellationException) {
                Log.e(this::class.simpleName, "❌ Timeout waiting for completion")
                Result.failure(Exception("Timeout waiting for completion"))
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Wait for completion error", e)
                Result.failure(e)
            }
        }
    }
}
