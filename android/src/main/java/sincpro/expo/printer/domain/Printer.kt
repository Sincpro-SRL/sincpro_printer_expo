package sincpro.expo.printer.domain

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

enum class ConnectionChangeReason(val value: String) {
    CONNECTED("connected"),
    USER_DISCONNECT("user_disconnect"),
    LINK_LOST("link_lost"),
    CONNECT_FAILED("connect_failed"),
}

data class Connection(
    val state: ConnectionState,
    val address: String?,
    val reason: ConnectionChangeReason?,
) {
    companion object {
        val IDLE = Connection(ConnectionState.DISCONNECTED, null, null)
    }
}

enum class BluetoothTransport(val value: String) { CLASSIC("classic"), BLE("ble"), UNKNOWN("unknown") }

data class BluetoothDevice(val name: String, val address: String, val transport: BluetoothTransport)

data class PrinterStatus(
    val connectionState: ConnectionState,
    val hasPaper: Boolean,
    val isCoverOpen: Boolean,
    val isOverheated: Boolean,
    val isCutterJammed: Boolean,
    val isRibbonEnd: Boolean,
    val hasMediaError: Boolean,
    val errorMessage: String?,
) {
    val hasError: Boolean
        get() = !hasPaper || isCoverOpen || isOverheated || isCutterJammed || isRibbonEnd || hasMediaError ||
            errorMessage != null
}

data class PrinterInfo(val model: String, val firmware: String, val serial: String, val dpi: Int)

enum class Density(val value: String, val level: Int) {
    LIGHT("light", 5),
    MEDIUM("medium", 10),
    DARK("dark", 15),
    EXTRA_DARK("extra_dark", 20),
}

enum class Speed(val value: String) { SLOW("slow"), MEDIUM("medium"), FAST("fast"), EXTRA_FAST("extra_fast") }

enum class Orientation(val value: String) { TOP_TO_BOTTOM("top_to_bottom"), BOTTOM_TO_TOP("bottom_to_top") }

/**
 * Context: `speed` is null unless the app sets it. 1.x never managed to change the speed (the SDK
 * rejected its values), so leaving it unset keeps each printer on its own configured speed.
 */
data class PrinterConfig(
    val marginLeft: Int = 0,
    val marginTop: Int = 0,
    val density: Density = Density.MEDIUM,
    val speed: Speed? = null,
    val orientation: Orientation = Orientation.TOP_TO_BOTTOM,
    val autoCutter: Boolean = false,
    val fullCut: Boolean = true,
)

enum class PrinterErrorCode {
    NOT_CONNECTED,
    CONNECT_TIMEOUT,
    CONNECT_FAILED,
    UNSUPPORTED_MODEL,
    BLUETOOTH_UNAVAILABLE,
    PRINT_FAILED,
    STATUS_FAILED,
    INVALID_ARGUMENT,
    UNSUPPORTED_PLATFORM,
}

/** The single error type of the printer layer; the code tells JS what happened. */
class PrinterException(val code: PrinterErrorCode, message: String, cause: Throwable? = null) :
    Exception(message, cause)
