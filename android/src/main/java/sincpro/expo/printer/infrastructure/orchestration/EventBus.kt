package sincpro.expo.printer.infrastructure.orchestration

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * INFRASTRUCTURE - Event Bus
 *
 * Publish/subscribe mechanism for printer events.
 * Uses Kotlin Flow for reactive event handling.
 *
 * This is infrastructure because it's a cross-cutting concern.
 */
class EventBus {
    private val _events = MutableSharedFlow<PrinterEvent>(replay = 0)
    val events: SharedFlow<PrinterEvent> = _events.asSharedFlow()

    /**
     * Publish an event
     */
    suspend fun publish(event: PrinterEvent) {
        _events.emit(event)
    }

    /**
     * Publish an event (non-suspend version for convenience)
     */
    fun publishSync(event: PrinterEvent) {
        _events.tryEmit(event)
    }
}

/**
 * Printer events - All events in the printer lifecycle
 *
 * Grouped by category for better organization.
 */
sealed class PrinterEvent {
    // BLUETOOTH DISCOVERY EVENTS
    object DiscoveryStarted : PrinterEvent()

    object DiscoveryStopped : PrinterEvent()

    data class DeviceFound(
        val name: String,
        val address: String,
    ) : PrinterEvent()

    // CONNECTION EVENTS
    data class Connecting(
        val address: String,
    ) : PrinterEvent()

    data class Connected(
        val address: String,
    ) : PrinterEvent()

    data class Disconnected(
        val address: String,
    ) : PrinterEvent()

    data class ConnectionFailed(
        val address: String,
        val error: String,
    ) : PrinterEvent()

    // PRINT JOB EVENTS
    data class JobStarted(
        val jobId: String,
    ) : PrinterEvent()

    data class JobCompleted(
        val jobId: String,
    ) : PrinterEvent()

    data class JobFailed(
        val jobId: String,
        val error: String,
    ) : PrinterEvent()

    data class JobProgress(
        val jobId: String,
        val progress: Int,
    ) : PrinterEvent()
}
