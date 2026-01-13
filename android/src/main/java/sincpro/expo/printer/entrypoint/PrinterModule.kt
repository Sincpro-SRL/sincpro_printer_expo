package sincpro.expo.printer.entrypoint

import android.content.Context
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import sincpro.expo.printer.adapter.bixolon.BixolonPrinterAdapter
import sincpro.expo.printer.domain.Alignment
import sincpro.expo.printer.domain.ConnectionConfig
import sincpro.expo.printer.domain.ConnectionType
import sincpro.expo.printer.domain.FontSize
import sincpro.expo.printer.domain.MediaConfig
import sincpro.expo.printer.domain.MediaType
import sincpro.expo.printer.domain.Receipt
import sincpro.expo.printer.domain.ReceiptLine
import sincpro.expo.printer.infrastructure.bluetooth.AndroidBluetoothProvider
import sincpro.expo.printer.infrastructure.orchestration.EventBus
import sincpro.expo.printer.infrastructure.orchestration.PrintJobOrchestrator
import sincpro.expo.printer.infrastructure.permission.PermissionService
import sincpro.expo.printer.service.ConnectivityService
import sincpro.expo.printer.service.LowLevelPrintService
import sincpro.expo.printer.service.PrintService

/**
 * ENTRYPOINT - Printer Module
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  EXPO MODULE - Public API for JavaScript/TypeScript                     │
 * │                                                                         │
 * │  Responsibilities:                                                      │
 * │  • Public API exposed to JS/TS                                          │
 * │  • Dependency injection for all layers                                  │
 * │  • JSON parsing → domain objects                                        │
 * │  • Error translation → readable messages                                │
 * │                                                                         │
 * │  IMPORTANT: This library provides LOW-LEVEL primitives.                 │
 * │  Clients define their business models (Invoice, Ticket, etc.)           │
 * │  and convert them to Receipt before calling this API.                   │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * ARCHITECTURE:
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  ENTRYPOINT (This file)                                    │
 *   │  PrinterModule - Expo Module API                           │
 *   └─────────────────────┬───────────────────────────────────────┘
 *                         │
 *   ┌─────────────────────▼───────────────────────────────────────┐
 *   │  SERVICE                                                    │
 *   │  ├── ConnectivityService (Bluetooth + Connection)           │
 *   │  ├── PrintService (High-level: Receipt, QR, Text)           │
 *   │  └── LowLevelPrintService (Primitives: drawText, drawQR)    │
 *   └─────────────────────┬───────────────────────────────────────┘
 *                         │
 *   ┌─────────────────────▼───────────────────────────────────────┐
 *   │  ADAPTER                                                    │
 *   │  └── BixolonPrinterAdapter (Bixolon SDK wrapper)            │
 *   └─────────────────────┬───────────────────────────────────────┘
 *                         │
 *   ┌─────────────────────▼───────────────────────────────────────┐
 *   │  DOMAIN (Pure Kotlin - no dependencies)                     │
 *   │  ├── IPrinterAdapter, IBluetoothProvider                    │
 *   │  ├── Receipt, ReceiptLine, MediaConfig                      │
 *   │  └── BluetoothDeviceInfo, ConnectionConfig                  │
 *   └─────────────────────────────────────────────────────────────┘
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  INFRASTRUCTURE (Cross-cutting concerns)                    │
 *   │  ├── AndroidBluetoothProvider (Android Bluetooth API)       │
 *   │  ├── PrintJobOrchestrator (Mutex, lifecycle)                │
 *   │  └── EventBus (Pub/Sub for events)                          │
 *   └─────────────────────────────────────────────────────────────┘
 */
class PrinterModule : Module() {
    // DEPENDENCY INJECTION

    // Infrastructure - Android wrappers
    private lateinit var bluetoothProvider: AndroidBluetoothProvider
    private lateinit var permissionService: PermissionService
    private lateinit var eventBus: EventBus
    private lateinit var orchestrator: PrintJobOrchestrator

    // Adapter - Printer implementation
    private lateinit var printerAdapter: BixolonPrinterAdapter

    // Services - Business logic
    private lateinit var connectivityService: ConnectivityService
    private lateinit var printService: PrintService
    private lateinit var lowLevelService: LowLevelPrintService

    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // MODULE DEFINITION

    override fun definition() =
        ModuleDefinition {
            Name("SincproPrinter")

            OnCreate {
                val context = appContext.reactContext as Context
                initializeDependencies(context)
                Log.d(this::class.simpleName, "✅ PrinterModule initialized")
            }

            OnDestroy {
                orchestrator.shutdown()
                Log.d(this::class.simpleName, "✅ PrinterModule destroyed")
            }

            // BLUETOOTH API

            AsyncFunction("isBluetoothEnabled") {
                connectivityService.isBluetoothEnabled()
            }

            AsyncFunction("isBluetoothSupported") {
                connectivityService.isBluetoothSupported()
            }

            AsyncFunction("getPairedDevices") {
                try {
                    val devices = connectivityService.getPairedDevices().getOrThrow()
                    devices.map { device ->
                        mapOf(
                            "name" to device.name,
                            "address" to device.address,
                            "type" to device.type.name,
                            "isPrinter" to device.isPrinter,
                        )
                    }
                } catch (e: Exception) {
                    Log.e(this::class.simpleName, "❌ Get paired devices failed", e)
                    throw e
                }
            }

            AsyncFunction("getPairedPrinters") {
                try {
                    val printers = connectivityService.getPairedPrinters().getOrThrow()
                    printers.map { device ->
                        mapOf(
                            "name" to device.name,
                            "address" to device.address,
                            "type" to device.type.name,
                        )
                    }
                } catch (e: Exception) {
                    Log.e(this::class.simpleName, "❌ Get paired printers failed", e)
                    throw e
                }
            }

            AsyncFunction("startBluetoothDiscovery") {
                try {
                    connectivityService.startDiscovery().getOrThrow()
                } catch (e: Exception) {
                    Log.e(this::class.simpleName, "❌ Start discovery failed", e)
                    throw e
                }
            }

            AsyncFunction("stopBluetoothDiscovery") {
                try {
                    connectivityService.stopDiscovery().getOrThrow()
                } catch (e: Exception) {
                    Log.e(this::class.simpleName, "❌ Stop discovery failed", e)
                    throw e
                }
            }

            Function("isDiscovering") {
                connectivityService.isDiscovering()
            }

            // PERMISSION API

            Function("hasBluetoothPermissions") {
                permissionService.hasBluetoothPermissions()
            }

            Function("getRequiredPermissions") {
                permissionService.getRequiredBluetoothPermissions()
            }

            Function("getMissingPermissions") {
                permissionService.getMissingBluetoothPermissions()
            }

            Function("getPermissionStatus") {
                val status = permissionService.getPermissionStatus()
                mapOf(
                    "allGranted" to status.allGranted,
                    "grantedPermissions" to status.grantedPermissions,
                    "deniedPermissions" to status.deniedPermissions,
                    "androidVersion" to status.androidVersion,
                )
            }

            // CONNECTION API

            AsyncFunction("connect") { address: String, port: Int ->
                try {
                    val config =
                        ConnectionConfig(
                            address = address,
                            port = port,
                            type = ConnectionType.BLUETOOTH,
                        )
                    connectivityService.connect(config).getOrThrow()
                    Log.d(this::class.simpleName, "✅ Connected to $address:$port")
                    true
                } catch (e: Exception) {
                    Log.e(this::class.simpleName, "❌ Connect failed", e)
                    throw e
                }
            }

            AsyncFunction("connectBluetooth") { address: String ->
                try {
                    connectivityService.connectBluetooth(address).getOrThrow()
                    Log.d(this::class.simpleName, "✅ Connected to $address")
                    true
                } catch (e: Exception) {
                    Log.e(this::class.simpleName, "❌ Connect failed", e)
                    throw e
                }
            }

            AsyncFunction("disconnect") {
                try {
                    connectivityService.disconnect().getOrThrow()
                    Log.d(this::class.simpleName, "✅ Disconnected")
                    true
                } catch (e: Exception) {
                    Log.e(this::class.simpleName, "❌ Disconnect failed", e)
                    throw e
                }
            }

            AsyncFunction("getConnectionStatus") {
                try {
                    val info = connectivityService.getConnectionStatus().getOrThrow()
                    mapOf(
                        "address" to info.address,
                        "port" to info.port,
                        "type" to info.type.name,
                        "status" to info.status.name,
                    )
                } catch (e: Exception) {
                    Log.e(this::class.simpleName, "❌ Get status failed", e)
                    throw e
                }
            }

            Function("isConnected") {
                connectivityService.isConnected()
            }

            // PRINT API (High Level)

            AsyncFunction("printReceipt") { receiptData: Map<String, Any?> ->
                scope.launch {
                    try {
                        val receipt = parseReceipt(receiptData)
                        val mediaConfig = parseMediaConfig(receiptData["mediaConfig"] as? Map<String, Any?>)
                        val copies = (receiptData["copies"] as? Number)?.toInt() ?: 1

                        printService.printReceipt(receipt, mediaConfig, copies).getOrThrow()
                        Log.d(this::class.simpleName, "✅ Receipt printed")
                    } catch (e: Exception) {
                        Log.e(this::class.simpleName, "❌ Print receipt failed", e)
                        throw e
                    }
                }
            }

            AsyncFunction("printLines") { linesData: List<Map<String, Any?>>, mediaConfigData: Map<String, Any?>? ->
                scope.launch {
                    try {
                        val lines = linesData.map { parseReceiptLine(it) }
                        val mediaConfig = parseMediaConfig(mediaConfigData)

                        printService.printLines(lines, mediaConfig).getOrThrow()
                        Log.d(this::class.simpleName, "✅ Lines printed")
                    } catch (e: Exception) {
                        Log.e(this::class.simpleName, "❌ Print lines failed", e)
                        throw e
                    }
                }
            }

            AsyncFunction("printQRCode") { data: String, size: Int ->
                scope.launch {
                    try {
                        printService.printQRCode(data, size).getOrThrow()
                        Log.d(this::class.simpleName, "✅ QR code printed")
                    } catch (e: Exception) {
                        Log.e(this::class.simpleName, "❌ Print QR code failed", e)
                        throw e
                    }
                }
            }

            AsyncFunction("printText") { text: String, fontSizeStr: String?, alignmentStr: String?, bold: Boolean? ->
                scope.launch {
                    try {
                        val fontSize = parseFontSize(fontSizeStr)
                        val alignment = parseAlignment(alignmentStr)

                        printService.printText(text, fontSize, alignment, bold ?: false).getOrThrow()
                        Log.d(this::class.simpleName, "✅ Text printed")
                    } catch (e: Exception) {
                        Log.e(this::class.simpleName, "❌ Print text failed", e)
                        throw e
                    }
                }
            }
        }

    // DEPENDENCY INJECTION SETUP

    private fun initializeDependencies(context: Context) {
        // 1. Infrastructure (Android wrappers)
        bluetoothProvider = AndroidBluetoothProvider(context)
        permissionService = PermissionService(context)
        eventBus = EventBus()

        // 2. Adapter (Printer implementation)
        printerAdapter = BixolonPrinterAdapter(context)

        // 3. Infrastructure (Orchestration - needs adapter)
        orchestrator = PrintJobOrchestrator(printerAdapter, eventBus)

        // 4. Services (Business logic - needs adapter + infrastructure)
        connectivityService =
            ConnectivityService(
                bluetoothProvider = bluetoothProvider,
                eventBus = eventBus,
                printerAdapter = printerAdapter,
            )

        printService =
            PrintService(
                printerAdapter = printerAdapter,
                orchestrator = orchestrator,
                eventBus = eventBus,
            )

        lowLevelService =
            LowLevelPrintService(
                printerAdapter = printerAdapter,
                orchestrator = orchestrator,
                eventBus = eventBus,
            )
    }

    // PARSERS (JSON → Domain)

    private fun parseReceipt(data: Map<String, Any?>): Receipt {
        val header =
            (data["header"] as? List<*>)?.map {
                parseReceiptLine(it as Map<String, Any?>)
            } ?: emptyList()

        val details =
            (data["details"] as? List<*>)?.map {
                parseReceiptLine(it as Map<String, Any?>)
            } ?: emptyList()

        val footer =
            (data["footer"] as? List<*>)?.map {
                parseReceiptLine(it as Map<String, Any?>)
            } ?: emptyList()

        return Receipt(header, details, footer)
    }

    private fun parseReceiptLine(data: Map<String, Any?>): ReceiptLine {
        val type =
            data["type"] as? String
                ?: throw IllegalArgumentException("Missing 'type' in line")

        return when (type) {
            "text" -> {
                ReceiptLine.Text(
                    content = data["content"] as? String ?: "",
                    fontSize = parseFontSize(data["fontSize"] as? String),
                    bold = data["bold"] as? Boolean ?: false,
                    alignment = parseAlignment(data["alignment"] as? String),
                )
            }

            "keyValue" -> {
                ReceiptLine.KeyValue(
                    key = data["key"] as? String ?: "",
                    value = data["value"] as? String ?: "",
                    fontSize = parseFontSize(data["fontSize"] as? String),
                    bold = data["bold"] as? Boolean ?: false,
                )
            }

            "qrCode" -> {
                ReceiptLine.QRCode(
                    data = data["data"] as? String ?: "",
                    size = (data["size"] as? Number)?.toInt() ?: 5,
                    alignment = parseAlignment(data["alignment"] as? String),
                )
            }

            "separator" -> {
                ReceiptLine.Separator(
                    char = data["char"] as? String ?: "-",
                    length = (data["length"] as? Number)?.toInt() ?: 48,
                )
            }

            "space" -> {
                ReceiptLine.Space(
                    lines = (data["lines"] as? Number)?.toInt() ?: 1,
                )
            }

            else -> {
                throw IllegalArgumentException("Unknown line type: $type")
            }
        }
    }

    /**
     * Parse media config from JSON.
     * Default: 80mm continuous paper
     */
    private fun parseMediaConfig(data: Map<String, Any?>?): MediaConfig {
        if (data == null) return MediaConfig.default() // 80mm

        val preset = data["preset"] as? String
        if (preset != null) {
            return when (preset) {
                "continuous58mm" -> MediaConfig.continuous58mm()
                "continuous80mm" -> MediaConfig.continuous80mm()
                "continuous104mm" -> MediaConfig.continuous104mm()
                "label80x50mm" -> MediaConfig.label80x50mm()
                "label100x60mm" -> MediaConfig.label100x60mm()
                else -> MediaConfig.default()
            }
        }

        val widthDots = (data["widthDots"] as? Number)?.toInt() ?: 640
        val heightDots = (data["heightDots"] as? Number)?.toInt() ?: 0
        val mediaType =
            when (data["mediaType"] as? String) {
                "continuous" -> MediaType.CONTINUOUS
                "labelGap" -> MediaType.LABEL_GAP
                "labelBlackMark" -> MediaType.LABEL_BLACK_MARK
                else -> MediaType.CONTINUOUS
            }
        val gapDots = (data["gapDots"] as? Number)?.toInt() ?: 0

        return MediaConfig(widthDots, heightDots, mediaType, gapDots)
    }

    private fun parseFontSize(size: String?): FontSize =
        when (size?.lowercase()) {
            "small" -> FontSize.SMALL
            "medium" -> FontSize.MEDIUM
            "large" -> FontSize.LARGE
            "xlarge" -> FontSize.XLARGE
            else -> FontSize.MEDIUM
        }

    private fun parseAlignment(alignment: String?): Alignment =
        when (alignment?.lowercase()) {
            "left" -> Alignment.LEFT
            "center" -> Alignment.CENTER
            "right" -> Alignment.RIGHT
            else -> Alignment.LEFT
        }
}
