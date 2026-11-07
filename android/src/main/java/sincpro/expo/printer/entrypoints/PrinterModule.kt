package sincpro.expo.printer.entrypoints

import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import sincpro.expo.printer.domain.Connection
import sincpro.expo.printer.domain.PrinterErrorCode
import sincpro.expo.printer.domain.PrinterException
import sincpro.expo.printer.domain.PrinterInfo
import sincpro.expo.printer.domain.PrinterStatus
import sincpro.expo.printer.services.PrinterService

private const val CONNECTION_EVENT = "onConnectionChange"

private fun Connection.toMap(): Map<String, Any?> =
    mapOf("state" to state.name, "address" to address, "reason" to reason?.value)

private fun PrinterStatus.toMap(): Map<String, Any?> =
    mapOf(
        "connectionState" to connectionState.name,
        "hasPaper" to hasPaper,
        "isCoverOpen" to isCoverOpen,
        "isOverheated" to isOverheated,
        "isCutterJammed" to isCutterJammed,
        "isRibbonEnd" to isRibbonEnd,
        "hasMediaError" to hasMediaError,
        "hasError" to hasError,
        "errorMessage" to errorMessage,
    )

private fun PrinterInfo.toMap(): Map<String, Any?> =
    mapOf("model" to model, "firmware" to firmware, "serial" to serial, "dpi" to dpi)

private fun Throwable.toCodedException(): CodedException =
    when (this) {
        is PrinterException -> CodedException(code.name, message, this)
        is CodedException -> this
        else -> CodedException(PrinterErrorCode.PRINT_FAILED.name, message ?: "Unexpected printer error", this)
    }

/** Exposure layer only: maps JS calls to [PrinterService] and its results back to JS values. */
class PrinterModule : Module() {
    private var printerService: PrinterService? = null

    private val service: PrinterService
        get() = printerService ?: throw CodedException(PrinterErrorCode.BLUETOOTH_UNAVAILABLE.name, "Printer module not ready", null)

    private fun <T> runOnSdk(promise: Promise, task: PrinterService.() -> T, toJs: (T) -> Any? = { null }) {
        service.enqueue(task) { result ->
            result.fold(
                onSuccess = { promise.resolve(toJs(it)) },
                onFailure = { promise.reject(it.toCodedException()) },
            )
        }
    }

    override fun definition() =
        ModuleDefinition {
            Name("SincproPrinter")

            Events(CONNECTION_EVENT)

            OnCreate {
                val context = requireNotNull(appContext.reactContext).applicationContext
                printerService = PrinterService(context) { connection -> sendEvent(CONNECTION_EVENT, connection.toMap()) }
            }

            OnDestroy {
                printerService?.destroy()
                printerService = null
            }

            Function("getPairedDevices") {
                service.pairedDevices().map { mapOf("name" to it.name, "address" to it.address, "transport" to it.transport.value) }
            }

            AsyncFunction("scan") { _: Int, promise: Promise ->
                runOnSdk(promise, { pairedDevices() }) { devices ->
                    devices.map { mapOf("name" to it.name, "address" to it.address, "transport" to it.transport.value) }
                }
            }

            AsyncFunction("connectBluetooth") { address: String, timeoutMs: Int, promise: Promise ->
                runOnSdk(promise, { connectBluetooth(address, timeoutMs) })
            }

            AsyncFunction("connectWifi") { ip: String, port: Int, timeoutMs: Int, promise: Promise ->
                runOnSdk(promise, { connectWifi(ip, port, timeoutMs) })
            }

            AsyncFunction("connectUsb") { promise: Promise ->
                runOnSdk(promise, { connectUsb(10000) })
            }

            AsyncFunction("disconnect") { promise: Promise ->
                runOnSdk(promise, { disconnect() })
            }

            Function("isConnected") { service.isConnected() }

            Function("getConnection") { service.connection().toMap() }

            AsyncFunction("getStatus") { promise: Promise ->
                runOnSdk(promise, { status() }) { it.toMap() }
            }

            AsyncFunction("getInfo") { promise: Promise ->
                runOnSdk(promise, { info() }) { it.toMap() }
            }

            Function("getDpi") { service.dpi() }

            AsyncFunction("setConfig") { config: PrinterConfigRecord, promise: Promise ->
                runOnSdk(promise, { setConfig(config.toDomain()) })
            }

            Function("getConfig") { service.config.toMap() }

            AsyncFunction("print") { job: PrintJobRecord, promise: Promise ->
                val printJob =
                    try {
                        job.toDomain()
                    } catch (error: Throwable) {
                        promise.reject(error.toCodedException())
                        return@AsyncFunction
                    }
                runOnSdk(promise, { print(printJob) })
            }

            Function("getPdfPageCount") { base64: String -> service.pdfPageCount(base64) }
        }
}
