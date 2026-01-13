package sincpro.expo.printer.domain

/**
 * DOMAIN - Bluetooth Types
 *
 * Pure domain entities for Bluetooth operations.
 * No Android dependencies - just data classes and interfaces.
 *
 * INTERFACE - Bluetooth Provider (Abstraction over Android Bluetooth)
 *
 * Interface for Bluetooth operations.
 * Implemented by Infrastructure layer (AndroidBluetoothProvider).
 */
interface IBluetoothProvider {
    /**
     * Check if Bluetooth hardware is supported
     */
    fun isSupported(): Boolean

    /**
     * Check if Bluetooth is currently enabled
     */
    fun isEnabled(): Boolean

    /**
     * Get list of paired/bonded devices
     */
    fun getPairedDevices(): Result<List<BluetoothDeviceInfo>>

    /**
     * Start device discovery
     */
    fun startDiscovery(): Result<Boolean>

    /**
     * Stop device discovery
     */
    fun stopDiscovery(): Result<Boolean>

    /**
     * Check if currently discovering
     */
    fun isDiscovering(): Boolean
}

// DATA CLASSES - Bluetooth Device Information

/**
 * Information about a Bluetooth device
 */
data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val type: BluetoothDeviceType,
    val isPrinter: Boolean,
)

/**
 * Bluetooth device types
 */
enum class BluetoothDeviceType {
    CLASSIC,
    LE,
    DUAL,
    UNKNOWN,
}

// DATA CLASSES - Connection Types

/**
 * Connection configuration
 */
data class ConnectionConfig(
    val address: String,
    val port: Int = 9100,
    val type: ConnectionType = ConnectionType.BLUETOOTH,
    val timeoutMs: Long = 30_000,
)

/**
 * Connection information (current state)
 */
data class ConnectionInfo(
    val address: String,
    val port: Int,
    val type: ConnectionType,
    val status: ConnectionStatus,
)

/**
 * Connection types supported
 */
enum class ConnectionType {
    BLUETOOTH,
    WIFI,
    USB,
    UNKNOWN,
}

/**
 * Connection status
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

// EXCEPTIONS - Bluetooth and Connection Errors

class BluetoothNotSupportedException : Exception("Bluetooth is not supported on this device")

class BluetoothDisabledException : Exception("Bluetooth is disabled")

class BluetoothPermissionDeniedException : Exception("Bluetooth permission denied")

class NoAdapterConfiguredException : Exception("No printer adapter configured")

class ConnectionFailedException(
    address: String,
    cause: Throwable,
) : Exception("Failed to connect to $address: ${cause.message}", cause)
