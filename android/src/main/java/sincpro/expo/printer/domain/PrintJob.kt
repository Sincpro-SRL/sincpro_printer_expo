package sincpro.expo.printer.domain

import java.util.UUID

/**
 * DOMAIN - Print Job
 *
 * Represents a print job with its status and result.
 */
data class PrintJob(
    val id: String = UUID.randomUUID().toString(),
    val receipt: Receipt? = null,
    val lines: List<ReceiptLine>? = null,
    val mediaConfig: MediaConfig = MediaConfig.continuous104mm(),
    val copies: Int = 1,
    val status: PrintJobStatus = PrintJobStatus.PENDING,
)

/**
 * Print job status
 */
enum class PrintJobStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
}

/**
 * Print job result
 */
sealed class PrintJobResult {
    data class Success(
        val jobId: String,
        val message: String = "Print completed successfully",
    ) : PrintJobResult()

    data class Failure(
        val jobId: String,
        val error: String,
        val exception: Throwable? = null,
    ) : PrintJobResult()

    data class Timeout(
        val jobId: String,
        val timeoutMs: Long,
    ) : PrintJobResult()
}
