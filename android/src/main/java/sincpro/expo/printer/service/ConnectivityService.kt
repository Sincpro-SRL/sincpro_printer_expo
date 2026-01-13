package sincpro.expo.printer.service

import android.util.Log
import sincpro.expo.printer.domain.BluetoothDeviceInfo
import sincpro.expo.printer.domain.BluetoothException
import sincpro.expo.printer.domain.ConnectionConfig
import sincpro.expo.printer.domain.ConnectionException
import sincpro.expo.printer.domain.ConnectionInfo
import sincpro.expo.printer.domain.IBluetoothProvider
import sincpro.expo.printer.domain.IPrinterAdapter
import sincpro.expo.printer.infrastructure.orchestration.EventBus
import sincpro.expo.printer.infrastructure.orchestration.PrinterEvent

/**
 * SERVICE - Connectivity Service
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  SELF-CONTAINED SERVICE                                                 │
 * │                                                                         │
 * │  This service encapsulates ALL connectivity-related operations:         │
 * │  • Bluetooth discovery and device listing                               │
 * │  • Printer connection and disconnection                                 │
 * │  • Connection status monitoring                                         │
 * │                                                                         │
 * │  Dependencies:                                                          │
 * │  • IBluetoothProvider (Infrastructure) - Android Bluetooth wrapper      │
 * │  • IPrinterAdapter (Adapter) - Printer-specific implementation          │
 * │  • EventBus (Infrastructure) - Event publishing                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
class ConnectivityService(
    // Infrastructure - Android Bluetooth wrapper
    private val bluetoothProvider: IBluetoothProvider,
    // Infrastructure - Event publishing
    private val eventBus: EventBus,
    // Adapter - Printer-specific implementation
    private val printerAdapter: IPrinterAdapter,
) {
    // Current connection state
    private var currentConnection: ConnectionInfo? = null

    // BLUETOOTH DISCOVERY OPERATIONS

    /**
     * Check if Bluetooth is supported on this device.
     *
     * @return true if Bluetooth hardware is available
     */
    fun isBluetoothSupported(): Boolean {
        val supported = bluetoothProvider.isSupported()
        Log.d(this::class.simpleName, "📱 Bluetooth supported: $supported")
        return supported
    }

    /**
     * Check if Bluetooth is currently enabled.
     *
     * @return true if Bluetooth is turned on
     */
    fun isBluetoothEnabled(): Boolean {
        val enabled = bluetoothProvider.isEnabled()
        Log.d(this::class.simpleName, "📶 Bluetooth enabled: $enabled")
        return enabled
    }

    /**
     * Get list of paired/bonded Bluetooth devices.
     *
     * Filters devices that appear to be printers based on name heuristics.
     *
     * @return Result containing list of paired devices, or error
     */
    fun getPairedDevices(): Result<List<BluetoothDeviceInfo>> {
        Log.d(this::class.simpleName, "🔍 Getting paired devices...")

        return bluetoothProvider.getPairedDevices().also { result ->
            result
                .onSuccess { devices ->
                    Log.d(this::class.simpleName, "📋 Found ${devices.size} paired devices")
                    devices.filter { it.isPrinter }.forEach { printer ->
                        Log.d(this::class.simpleName, "  🖨️ Printer: ${printer.name} (${printer.address})")
                    }
                }.onFailure { error ->
                    Log.e(this::class.simpleName, "❌ Failed to get paired devices: ${error.message}")
                }
        }
    }

    /**
     * Get only printer devices from paired devices.
     *
     * @return Result containing list of printer devices
     */
    fun getPairedPrinters(): Result<List<BluetoothDeviceInfo>> =
        getPairedDevices().map { devices ->
            devices.filter { it.isPrinter }
        }

    /**
     * Start Bluetooth device discovery.
     *
     * Discovered devices will be published via EventBus.
     *
     * @return Result indicating if discovery started successfully
     */
    fun startDiscovery(): Result<Boolean> {
        Log.d(this::class.simpleName, "🔎 Starting Bluetooth discovery...")

        return bluetoothProvider.startDiscovery().also { result ->
            result
                .onSuccess { started ->
                    if (started) {
                        eventBus.publish(PrinterEvent.DiscoveryStarted)
                        Log.d(this::class.simpleName, "✅ Discovery started")
                    }
                }.onFailure { error ->
                    Log.e(this::class.simpleName, "❌ Failed to start discovery: ${error.message}")
                }
        }
    }

    /**
     * Stop Bluetooth device discovery.
     *
     * @return Result indicating if discovery was stopped
     */
    fun stopDiscovery(): Result<Boolean> {
        Log.d(this::class.simpleName, "⏹️ Stopping Bluetooth discovery...")

        return bluetoothProvider.stopDiscovery().also { result ->
            result.onSuccess {
                eventBus.publish(PrinterEvent.DiscoveryStopped)
                Log.d(this::class.simpleName, "✅ Discovery stopped")
            }
        }
    }

    /**
     * Check if currently discovering devices.
     *
     * @return true if discovery is in progress
     */
    fun isDiscovering(): Boolean = bluetoothProvider.isDiscovering()

    // PRINTER CONNECTION OPERATIONS

    /**
     * Connect to a printer.
     *
     * @param config Connection configuration (address, port, type)
     * @return Result containing connection info if successful
     */
    suspend fun connect(config: ConnectionConfig): Result<ConnectionInfo> {
        Log.d(this::class.simpleName, "🔌 Connecting to ${config.address}...")

        // Update state to connecting
        currentConnection =
            ConnectionInfo(
                address = config.address,
                port = config.port,
                type = config.type,
                status = ConnectionStatus.CONNECTING,
            )
        eventBus.publish(PrinterEvent.Connecting(config.address))

        return try {
            // Delegate to printer adapter
            printerAdapter.connect(config.address, config.port).getOrThrow()

            // Update state to connected
            val connection =
                ConnectionInfo(
                    address = config.address,
                    port = config.port,
                    type = config.type,
                    status = ConnectionStatus.CONNECTED,
                )
            currentConnection = connection

            eventBus.publish(PrinterEvent.Connected(config.address))
            Log.d(this::class.simpleName, "✅ Connected to ${config.address}")

            Result.success(connection)
        } catch (e: Exception) {
            // Update state to error
            currentConnection = currentConnection?.copy(status = ConnectionStatus.ERROR)
            eventBus.publish(PrinterEvent.ConnectionFailed(config.address, e.message ?: "Unknown error"))
            Log.e(this::class.simpleName, "❌ Connection failed: ${e.message}")

            Result.failure(ConnectionFailedException(config.address, e))
        }
    }

    /**
     * Connect to a printer by MAC address with default settings.
     *
     * Convenience method for Bluetooth connections.
     *
     * @param address Bluetooth MAC address (e.g., "00:11:22:33:44:55")
     * @return Result containing connection info
     */
    suspend fun connectBluetooth(address: String): Result<ConnectionInfo> =
        connect(
            ConnectionConfig(
                address = address,
                type = ConnectionType.BLUETOOTH,
            ),
        )

    /**
     * Disconnect from current printer.
     *
     * @return Result indicating success or failure
     */
    suspend fun disconnect(): Result<Unit> {
        val current = currentConnection
        Log.d(this::class.simpleName, "🔌 Disconnecting from ${current?.address}...")

        return try {
            printerAdapter.disconnect().getOrThrow()

            val address = currentConnection?.address ?: "unknown"
            currentConnection = null

            eventBus.publish(PrinterEvent.Disconnected(address))
            Log.d(this::class.simpleName, "✅ Disconnected")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "❌ Disconnect failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get current connection status from printer.
     *
     * @return Result containing current connection info
     */
    suspend fun getConnectionStatus(): Result<ConnectionInfo> =
        try {
            val status = printerAdapter.getStatus().getOrThrow()

            val connection =
                currentConnection?.copy(
                    status = if (status.isConnected) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED,
                ) ?: ConnectionInfo(
                    address = "",
                    port = 0,
                    type = ConnectionType.UNKNOWN,
                    status = ConnectionStatus.DISCONNECTED,
                )

            Log.d(this::class.simpleName, "📊 Connection status: ${connection.status}")
            Result.success(connection)
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "❌ Failed to get status: ${e.message}")
            Result.failure(e)
        }

    /**
     * Check if currently connected to a printer.
     *
     * Uses cached state for quick check.
     *
     * @return true if connected
     */
    fun isConnected(): Boolean = currentConnection?.status == ConnectionStatus.CONNECTED

    /**
     * Get current connection info (cached).
     *
     * @return Current connection or null if not connected
     */
    fun getCurrentConnection(): ConnectionInfo? = currentConnection
}
