package sincpro.expo.printer.infrastructure.orchestration

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import sincpro.expo.printer.domain.IPrinterAdapter
import sincpro.expo.printer.domain.MediaConfig
import java.util.UUID

/**
 * INFRASTRUCTURE - Print Job Orchestrator
 *
 * Responsibilities:
 * - Mutex (only 1 job at a time)
 * - Job queue management
 * - Lifecycle management (setup/teardown)
 * - Event publishing
 *
 * This is infrastructure because it handles non-functional concerns.
 */
class PrintJobOrchestrator(
    private val adapter: IPrinterAdapter,
    private val eventBus: EventBus,
) {
    private val jobMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Execute a print job with guarantees:
     * - Only 1 job at a time (mutex)
     * - Proper lifecycle (setup/teardown)
     * - Events published
     */
    suspend fun <T> executeJob(
        jobId: String = UUID.randomUUID().toString(),
        mediaConfig: MediaConfig,
        block: suspend PrintJobContext.() -> T,
    ): Result<T> =
        jobMutex.withLock {
            Log.d(this::class.simpleName, "🔒 Job $jobId acquired lock")

            try {
                eventBus.publish(PrinterEvent.JobStarted(jobId))

                val context = PrintJobContext(adapter, mediaConfig, jobId)

                // Setup: initialize, configure media, clear buffer, begin transaction
                context.setup()

                // Execute user code
                val result = block(context)

                // Teardown: end transaction
                context.teardown()

                eventBus.publish(PrinterEvent.JobCompleted(jobId))
                Log.d(this::class.simpleName, "✅ Job $jobId completed")

                Result.success(result)
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "❌ Job $jobId failed", e)
                eventBus.publish(PrinterEvent.JobFailed(jobId, e.message ?: "Unknown error"))
                Result.failure(e)
            }
        }

    /**
     * Shutdown the orchestrator
     */
    fun shutdown() {
        scope.cancel()
        Log.d(this::class.simpleName, "Orchestrator shutdown")
    }
}

/**
 * Print job execution context
 *
 * Provides operations available within a print job.
 */
class PrintJobContext(
    private val adapter: IPrinterAdapter,
    private val mediaConfig: MediaConfig,
    val jobId: String,
) {
    /**
     * Setup: initialize, configure, clear, begin transaction
     */
    suspend fun setup() {
        adapter.initializePrinter().getOrThrow()
        adapter.configureMedia(mediaConfig).getOrThrow()
        adapter.clearBuffer().getOrThrow()
        adapter.beginTransaction().getOrThrow()
    }

    /**
     * Teardown: end transaction
     */
    suspend fun teardown() {
        adapter.endTransaction().getOrThrow()
    }

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
    ) {
        adapter.drawText(text, x, y, fontSize, bold, alignment).getOrThrow()
    }

    /**
     * Draw QR code
     */
    suspend fun drawQR(
        data: String,
        x: Int,
        y: Int,
        size: Int,
    ) {
        adapter.drawQR(data, x, y, size).getOrThrow()
    }

    /**
     * Draw bitmap image
     */
    suspend fun drawBitmap(
        bitmap: android.graphics.Bitmap,
        x: Int,
        y: Int,
    ) {
        adapter.drawBitmap(bitmap, x, y).getOrThrow()
    }

    /**
     * Execute print and wait for completion
     */
    suspend fun print(copies: Int = 1) {
        adapter.print(copies).getOrThrow()
        adapter.waitForCompletion(DEFAULT_TIMEOUT_MS).getOrThrow()
    }

    /**
     * Get configured media width
     */
    fun getMediaWidth(): Int = mediaConfig.widthDots

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 30_000L
    }
}
