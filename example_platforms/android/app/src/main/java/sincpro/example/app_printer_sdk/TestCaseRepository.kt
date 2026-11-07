package sincpro.example.app_printer_sdk

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import sincpro.expo.printer.domain.ConnectionChangeReason
import sincpro.expo.printer.domain.ConnectionState
import sincpro.expo.printer.domain.Density
import sincpro.expo.printer.domain.PrintJob
import sincpro.expo.printer.domain.PrinterConfig
import sincpro.expo.printer.domain.PrinterErrorCode
import sincpro.expo.printer.domain.PrinterException
import sincpro.expo.printer.domain.Speed
import sincpro.expo.printer.services.PrinterService

private const val UNREACHABLE_MAC = "00:11:22:33:44:55"

class TestCaseRepository(
    private val service: PrinterService,
    private val context: Context,
    private val selectedAddress: () -> String?,
) {
    private val logoBase64: String by lazy { rawBase64(R.raw.sincpro_simbolo) }
    private val receiptPdfBase64: String by lazy { rawBase64(R.raw.comprobante_1767994755165) }

    private fun rawBase64(resource: Int): String =
        context.resources.openRawResource(resource).use { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }

    private suspend fun print(job: PrintJob): Result<String> =
        runCatching { service.call { print(job) } }.map { "✓ Sent ${job.commands.size} command(s)" }

    private suspend fun expectError(code: PrinterErrorCode, task: suspend () -> Unit): Result<String> =
        runCatching {
            try {
                task()
            } catch (error: PrinterException) {
                check(error.code == code) { "Expected $code, got ${error.code}: ${error.message}" }
                return@runCatching "✓ ${error.code}: ${error.message}"
            }
            error("Expected $code but the call succeeded")
        }

    private fun requireAddress(): String = selectedAddress() ?: error("Select a printer with Connect first")

    fun getAllTestCases(): List<TestCase> =
        connectivityTests() + printTextTests() + printQrTests() + printBarcodeTests() + printBinaryTests() +
            printReceiptTests() + errorHandlingTests() + configurationTests()

    private fun connectivityTests() =
        listOf(
            TestCase("conn_01", "Get Paired Devices", "List all paired Bluetooth devices", TestCategory.CONNECTIVITY, requiresConnection = false) {
                runCatching {
                    val devices = service.pairedDevices()
                    "Found ${devices.size} devices:\n" + devices.joinToString("\n") { "• ${it.name} (${it.address}, ${it.transport.value})" }
                }
            },
            TestCase("conn_02", "Check Connection Status", "isConnected() and connection() from the SDK link", TestCategory.CONNECTIVITY, requiresConnection = false) {
                runCatching { "isConnected=${service.isConnected()} · ${service.connection()}" }
            },
            TestCase("conn_03", "Get Printer Status", "Paper, cover, overheat, cutter, ribbon, media", TestCategory.CONNECTIVITY) {
                runCatching { service.call { status() }.toString() }
            },
            TestCase("conn_04", "Get Printer Info", "Model, firmware, serial and dpi", TestCategory.CONNECTIVITY) {
                runCatching { service.call { info() }.toString() }
            },
            TestCase("conn_05", "Reconnect Same Printer", "Connecting again to the connected printer must keep the session", TestCategory.CONNECTIVITY) {
                runCatching {
                    service.call { connectBluetooth(requireAddress(), 10000) }
                    check(service.isConnected()) { "Session was lost on reconnect" }
                    "✓ Still connected: ${service.connection()}"
                }
            },
            TestCase(
                "conn_06", "Double Connect (regression)",
                "Two connects at once must not leave an orphan socket; ends CONNECTED", TestCategory.CONNECTIVITY, requiresConnection = false,
            ) {
                runCatching {
                    val address = requireAddress()
                    service.call { disconnect() }
                    coroutineScope {
                        listOf(
                            async { runCatching { service.call { connectBluetooth(address, 10000) } } },
                            async { runCatching { service.call { connectBluetooth(address, 10000) } } },
                        ).awaitAll()
                    }
                    check(service.isConnected()) { "Not connected after double connect: ${service.connection()}" }
                    service.call { print(TestJob().text("Double connect OK").build()) }
                    "✓ Connected and printing: ${service.connection()}"
                }
            },
            TestCase("conn_07", "Disconnect / Reconnect", "Disconnect, then connect again and print", TestCategory.CONNECTIVITY) {
                runCatching {
                    val address = requireAddress()
                    service.call { disconnect() }
                    check(!service.isConnected()) { "Still connected after disconnect" }
                    service.call { connectBluetooth(address, 10000) }
                    service.call { print(TestJob().text("Reconnect OK").build()) }
                    "✓ ${service.connection()}"
                }
            },
            TestCase(
                "conn_08", "Link Lost (manual)",
                "Run it, then TURN OFF the printer within 15 s: state must become DISCONNECTED/link_lost", TestCategory.CONNECTIVITY,
            ) {
                runCatching {
                    repeat(30) {
                        val connection = service.connection()
                        if (connection.state == ConnectionState.DISCONNECTED) {
                            check(connection.reason == ConnectionChangeReason.LINK_LOST) { "Unexpected reason: $connection" }
                            return@runCatching "✓ Detected: $connection"
                        }
                        delay(500)
                    }
                    error("Printer still reported as connected after 15 s")
                }
            },
            TestCase(
                "conn_09", "Connect Timeout",
                "Unreachable MAC with 4 s timeout must fail fast (drops the current session)", TestCategory.CONNECTIVITY, requiresConnection = false,
            ) {
                val startedAt = System.currentTimeMillis()
                runCatching {
                    try {
                        service.call { connectBluetooth(UNREACHABLE_MAC, 4000) }
                        error("Connected to an unreachable MAC")
                    } catch (error: PrinterException) {
                        val elapsed = System.currentTimeMillis() - startedAt
                        "✓ ${error.code} after ${elapsed}ms · state=${service.connection().state}"
                    }
                }
            },
        )

    private fun printTextTests() =
        listOf(
            TestCase("text_01", "Print Simple Text", "Medium font, left", TestCategory.PRINT_TEXT) { print(TestJob().text("Hello from Sincpro Printer").build()) },
            TestCase("text_02", "Print Text - Center Aligned", "Center alignment", TestCategory.PRINT_TEXT) {
                print(TestJob().text("CENTERED", align = Align.CENTER).build())
            },
            TestCase("text_03", "Print Text - Right Aligned", "Right alignment", TestCategory.PRINT_TEXT) {
                print(TestJob().text("RIGHT", align = Align.RIGHT).build())
            },
            TestCase("text_04", "Print Text - Font Sizes", "Small, medium, large and xlarge without overlap", TestCategory.PRINT_TEXT) {
                print(
                    TestJob().text("Small", Font.SMALL).text("Medium", Font.MEDIUM).text("Large", Font.LARGE)
                        .text("XLarge", Font.XLARGE).build(),
                )
            },
            TestCase("text_05", "Print Text - Bold", "Bold style", TestCategory.PRINT_TEXT) { print(TestJob().text("BOLD TEXT", bold = true).build()) },
            TestCase("text_06", "Print Text - Accents", "Spanish characters: áéíóú ñ ¿¡", TestCategory.PRINT_TEXT) {
                print(TestJob().text("áéíóú ÁÉÍÓÚ ñÑ ¿¡ °").build())
            },
        )

    private fun printQrTests() =
        listOf(
            TestCase("qr_01", "Print QR - URL", "Size 5, centered", TestCategory.PRINT_QR) { print(TestJob().qr("https://sincpro.com.bo").build()) },
            TestCase("qr_02", "Print QR - Small", "Size 3", TestCategory.PRINT_QR) { print(TestJob().qr("SMALL", size = 3).build()) },
            TestCase("qr_03", "Print QR - Large", "Size 8", TestCategory.PRINT_QR) { print(TestJob().qr("LARGE", size = 8).build()) },
            TestCase("qr_04", "Print QR - Left Aligned", "Left", TestCategory.PRINT_QR) { print(TestJob().qr("LEFT", align = Align.LEFT).build()) },
        )

    private fun printBarcodeTests() =
        listOf(
            TestCase("barcode_01", "Print Barcode - CODE128", "CODE128 with text", TestCategory.PRINT_BARCODE) { print(TestJob().barcode("SINCPRO123").build()) },
            TestCase("barcode_02", "Print Barcode - CODE39", "CODE39", TestCategory.PRINT_BARCODE) {
                print(TestJob().barcode("ABC123", Symbology.CODE39).build())
            },
            TestCase("barcode_03", "Print Barcode - EAN13", "EAN13", TestCategory.PRINT_BARCODE) {
                print(TestJob().barcode("7501031311309", Symbology.EAN13).build())
            },
            TestCase("barcode_04", "Print Barcode - Tall", "Bar height 120", TestCategory.PRINT_BARCODE) {
                print(TestJob().barcode("TALL", barHeight = 120).build())
            },
        )

    private fun printBinaryTests() =
        listOf(
            TestCase("image_01", "Print Image (logo)", "PNG from res/raw, centered", TestCategory.PRINT_IMAGE) { print(TestJob().image(logoBase64).build()) },
            TestCase("pdf_01", "PDF Page Count", "Pages of the sample receipt PDF", TestCategory.PRINT_PDF, requiresConnection = false) {
                runCatching { "Pages: ${service.pdfPageCount(receiptPdfBase64)}" }
            },
            TestCase("pdf_02", "Print PDF Page 1", "Sample receipt PDF rendered to 72mm", TestCategory.PRINT_PDF) { print(TestJob().pdf(receiptPdfBase64).build()) },
        )

    private fun printReceiptTests() =
        listOf(
            TestCase("receipt_01", "Print Receipt", "Logo, text, separator, QR and barcode in one job", TestCategory.PRINT_RECEIPT) {
                print(
                    TestJob()
                        .image(logoBase64)
                        .text("SINCPRO SRL", Font.LARGE, bold = true, align = Align.CENTER)
                        .text("Comprobante de prueba", align = Align.CENTER)
                        .separator()
                        .text("Coca Cola 2L          2   30.00", Font.SMALL)
                        .text("Pan                   5   10.00", Font.SMALL)
                        .separator()
                        .text("TOTAL          Bs 40.00", bold = true)
                        .qr("https://sincpro.com.bo/r/123", size = 4)
                        .barcode("123456789", barHeight = 50)
                        .text("Gracias por su compra", align = Align.CENTER)
                        .space(40)
                        .build(),
                )
            },
            TestCase("receipt_02", "Print Receipt x2 Copies", "Same job, two copies", TestCategory.PRINT_RECEIPT) {
                print(TestJob().text("COPY TEST", Font.LARGE, align = Align.CENTER).build(copies = 2))
            },
        )

    private fun errorHandlingTests() =
        listOf(
            TestCase("error_01", "Print While Disconnected", "Must fail with NOT_CONNECTED (disconnects first)", TestCategory.ERROR_HANDLING, requiresConnection = false) {
                expectError(PrinterErrorCode.NOT_CONNECTED) {
                    service.call { disconnect() }
                    service.call { print(TestJob().text("should not print").build()) }
                }
            },
            TestCase("error_02", "Invalid Image", "Must fail with INVALID_ARGUMENT", TestCategory.ERROR_HANDLING) {
                expectError(PrinterErrorCode.INVALID_ARGUMENT) {
                    service.call { print(PrintJob(TestJob().build().media, 1, listOf(sincpro.expo.printer.domain.PrintCommand.Image(0, 20, 0, "bm90LWFuLWltYWdl", 200)))) }
                }
            },
            TestCase("error_03", "PDF Page Out of Range", "Page 99 must fail with INVALID_ARGUMENT", TestCategory.ERROR_HANDLING) {
                expectError(PrinterErrorCode.INVALID_ARGUMENT) { service.call { print(TestJob().pdf(receiptPdfBase64, page = 99).build()) } }
            },
        )

    private fun configurationTests() =
        listOf(
            TestCase("config_01", "Density Extra Dark", "Same config the tickets app uses", TestCategory.CONFIGURATION) {
                runCatching {
                    service.call { setConfig(PrinterConfig(density = Density.EXTRA_DARK)) }
                    service.call { print(TestJob().text("Extra dark density").build()) }
                    "✓ ${service.config}"
                }
            },
            TestCase("config_02", "Speed Medium", "1.x could never change speed; now applied", TestCategory.CONFIGURATION) {
                runCatching {
                    service.call { setConfig(service.config.copy(speed = Speed.MEDIUM)) }
                    service.call { print(TestJob().text("Speed medium").build()) }
                    "✓ ${service.config}"
                }
            },
            TestCase("config_03", "Reset Defaults", "Margins 0, density medium, speed untouched", TestCategory.CONFIGURATION) {
                runCatching {
                    service.call { setConfig(PrinterConfig()) }
                    "✓ ${service.config}"
                }
            },
        )
}
