package sincpro.example.app_printer_sdk

import android.content.Context
import android.util.Base64
import com.sincpro.printer.SincproPrinterSdk
import com.sincpro.printer.domain.*

class TestCaseRepository(
    private val sdk: SincproPrinterSdk,
    private val context: Context
) {
    // Helper to convert Result<Unit> to Result<String>
    private fun Result<Unit>.withMessage(msg: String = "✓ Done"): Result<String> = map { msg }

    fun getAllTestCases(): List<TestCase> = buildList {
        addAll(connectivityTests())
        addAll(printTextTests())
        addAll(printQrTests())
        addAll(printBarcodeTests())
        addAll(printReceiptTests())
        addAll(printBinaryTests())
        addAll(errorHandlingTests())
        addAll(configurationTests())
    }

    private fun connectivityTests() = listOf(
        TestCase(
            id = "conn_01",
            name = "Get Paired Devices",
            description = "List all paired Bluetooth devices",
            category = TestCategory.CONNECTIVITY,
            requiresConnection = false
        ) {
            sdk.bixolon.connectivity.getPairedDevices()
                .map { devices -> 
                    buildString {
                        appendLine("Found ${devices.size} devices:")
                        devices.forEach { appendLine("• ${it.name} (${it.address})") }
                    }.trim()
                }
        },

        TestCase(
            id = "conn_02",
            name = "Get Paired Printers",
            description = "List only paired printer devices",
            category = TestCategory.CONNECTIVITY,
            requiresConnection = false
        ) {
            sdk.bixolon.connectivity.getPairedPrinters()
                .map { printers -> 
                    buildString {
                        appendLine("Found ${printers.size} printers:")
                        printers.forEach { appendLine("• ${it.name} (${it.address})") }
                    }.trim()
                }
        },

        TestCase(
            id = "conn_03",
            name = "Check Connection Status",
            description = "Verify isConnected() returns correct state",
            category = TestCategory.CONNECTIVITY,
            requiresConnection = false
        ) {
            val connected = sdk.bixolon.connectivity.isConnected()
            Result.success("Connected: $connected")
        },

        TestCase(
            id = "conn_04",
            name = "Get Printer Status",
            description = "Get paper and error status from printer",
            category = TestCategory.CONNECTIVITY
        ) {
            sdk.bixolon.connectivity.getStatus()
                .map { status ->
                    buildString {
                        appendLine("Connected: ${status.isConnected}")
                        appendLine("Has Paper: ${status.hasPaper}")
                        append("Has Error: ${status.hasError}")
                    }
                }
        },

        TestCase(
            id = "conn_05",
            name = "Get Printer Info",
            description = "Get model, firmware, serial, DPI",
            category = TestCategory.CONNECTIVITY
        ) {
            sdk.bixolon.connectivity.getInfo()
                .map { info ->
                    buildString {
                        appendLine("Model: ${info.model}")
                        appendLine("Firmware: ${info.firmware}")
                        appendLine("Serial: ${info.serialNumber}")
                        append("DPI: ${info.dpi}")
                    }
                }
        },

        TestCase(
            id = "conn_06",
            name = "Disconnect",
            description = "Disconnect from printer",
            category = TestCategory.CONNECTIVITY
        ) {
            sdk.bixolon.connectivity.disconnect()
                .map { "Disconnected successfully" }
        }
    )

    private fun printTextTests() = listOf(
        TestCase(
            id = "text_01",
            name = "Print Simple Text",
            description = "Print 'Hello World' with default settings",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printText("Hello World - SDK Test").withMessage("Printed: Hello World")
        },

        TestCase(
            id = "text_02",
            name = "Print Text - Left Aligned",
            description = "Print text aligned to left",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printText(
                text = "LEFT ALIGNED TEXT",
                alignment = Alignment.LEFT
            ).withMessage("Printed LEFT aligned")
        },

        TestCase(
            id = "text_03",
            name = "Print Text - Center Aligned",
            description = "Print text centered",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printText(
                text = "CENTER ALIGNED",
                alignment = Alignment.CENTER
            ).withMessage("Printed CENTER aligned")
        },

        TestCase(
            id = "text_04",
            name = "Print Text - Right Aligned",
            description = "Print text aligned to right",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printText(
                text = "RIGHT ALIGNED",
                alignment = Alignment.RIGHT
            ).withMessage("Printed RIGHT aligned")
        },

        TestCase(
            id = "text_05",
            name = "Print Text - Small Font",
            description = "Print with SMALL font size",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printText(
                text = "Small Font Text",
                fontSize = FontSize.SMALL
            ).withMessage("Printed SMALL font")
        },

        TestCase(
            id = "text_06",
            name = "Print Text - Large Font",
            description = "Print with LARGE font size",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printText(
                text = "Large Font",
                fontSize = FontSize.LARGE
            ).withMessage("Printed LARGE font")
        },

        TestCase(
            id = "text_07",
            name = "Print Text - XLarge Font",
            description = "Print with XLARGE font size",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printText(
                text = "XLarge",
                fontSize = FontSize.XLARGE
            ).withMessage("Printed XLARGE font")
        },

        TestCase(
            id = "text_08",
            name = "Print Text - Bold",
            description = "Print bold text",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printText(
                text = "BOLD TEXT",
                bold = true
            ).withMessage("Printed BOLD text")
        },

        TestCase(
            id = "text_09",
            name = "Print Multiple Lines",
            description = "Print list of strings",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printTexts(
                texts = listOf(
                    "Line 1: First item",
                    "Line 2: Second item",
                    "Line 3: Third item",
                    "Line 4: Fourth item"
                )
            ).withMessage("Printed 4 lines")
        },

        TestCase(
            id = "text_10",
            name = "Print Key-Value",
            description = "Print key-value pair (label: value)",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printKeyValue(
                key = "Total:",
                value = "$99.99"
            ).withMessage("Printed: Total: \$99.99")
        },

        TestCase(
            id = "text_11",
            name = "Print Key-Value Bold",
            description = "Print bold key-value pair",
            category = TestCategory.PRINT_TEXT
        ) {
            sdk.bixolon.print.printKeyValue(
                key = "TOTAL:",
                value = "$199.99",
                bold = true
            ).withMessage("Printed BOLD: TOTAL: \$199.99")
        }
    )

    private fun printQrTests() = listOf(
        TestCase(
            id = "qr_01",
            name = "Print QR - URL",
            description = "Print QR code with URL",
            category = TestCategory.PRINT_QR
        ) {
            sdk.bixolon.print.printQR(
                data = "https://sincpro.com",
                size = 5
            ).withMessage("QR: sincpro.com, size=5")
        },

        TestCase(
            id = "qr_02",
            name = "Print QR - Small",
            description = "Print small QR code (size=3)",
            category = TestCategory.PRINT_QR
        ) {
            sdk.bixolon.print.printQR(
                data = "SMALL-QR-TEST",
                size = 3
            ).withMessage("QR: SMALL, size=3")
        },

        TestCase(
            id = "qr_03",
            name = "Print QR - Large",
            description = "Print large QR code (size=8)",
            category = TestCategory.PRINT_QR
        ) {
            sdk.bixolon.print.printQR(
                data = "LARGE-QR-TEST",
                size = 8
            ).withMessage("QR: LARGE, size=8")
        },

        TestCase(
            id = "qr_04",
            name = "Print QR - Left Aligned",
            description = "Print QR aligned to left",
            category = TestCategory.PRINT_QR
        ) {
            sdk.bixolon.print.printQR(
                data = "LEFT-QR",
                size = 4,
                alignment = Alignment.LEFT
            ).withMessage("QR: LEFT aligned")
        },

        TestCase(
            id = "qr_05",
            name = "Print QR - Right Aligned",
            description = "Print QR aligned to right",
            category = TestCategory.PRINT_QR
        ) {
            sdk.bixolon.print.printQR(
                data = "RIGHT-QR",
                size = 4,
                alignment = Alignment.RIGHT
            ).withMessage("QR: RIGHT aligned")
        }
    )

    private fun printBarcodeTests() = listOf(
        TestCase(
            id = "barcode_01",
            name = "Print Barcode - CODE128",
            description = "Print CODE128 barcode",
            category = TestCategory.PRINT_BARCODE
        ) {
            sdk.bixolon.print.printBarcode(
                data = "ABC123456",
                type = BarcodeType.CODE128
            ).withMessage("Barcode: CODE128 - ABC123456")
        },

        TestCase(
            id = "barcode_02",
            name = "Print Barcode - CODE39",
            description = "Print CODE39 barcode",
            category = TestCategory.PRINT_BARCODE
        ) {
            sdk.bixolon.print.printBarcode(
                data = "CODE39TEST",
                type = BarcodeType.CODE39
            ).withMessage("Barcode: CODE39")
        },

        TestCase(
            id = "barcode_03",
            name = "Print Barcode - EAN13",
            description = "Print EAN13 barcode (13 digits)",
            category = TestCategory.PRINT_BARCODE
        ) {
            sdk.bixolon.print.printBarcode(
                data = "5901234123457",
                type = BarcodeType.EAN13
            ).withMessage("Barcode: EAN13")
        },

        TestCase(
            id = "barcode_04",
            name = "Print Barcode - Tall",
            description = "Print tall barcode (height=100)",
            category = TestCategory.PRINT_BARCODE
        ) {
            sdk.bixolon.print.printBarcode(
                data = "TALL123",
                type = BarcodeType.CODE128,
                height = 100
            ).withMessage("Barcode: height=100")
        },

        TestCase(
            id = "barcode_05",
            name = "Print Barcode - Short",
            description = "Print short barcode (height=30)",
            category = TestCategory.PRINT_BARCODE
        ) {
            sdk.bixolon.print.printBarcode(
                data = "SHORT123",
                type = BarcodeType.CODE128,
                height = 30
            ).withMessage("Barcode: height=30")
        }
    )

    private fun printReceiptTests() = listOf(
        TestCase(
            id = "receipt_01",
            name = "Print Simple Receipt",
            description = "Print basic receipt with header/body/footer",
            category = TestCategory.PRINT_RECEIPT
        ) {
            val receipt = Receipt(
                header = listOf(
                    ReceiptLine.Text("SINCPRO STORE", FontSize.LARGE, true, Alignment.CENTER),
                    ReceiptLine.Text("Test Receipt", FontSize.MEDIUM, false, Alignment.CENTER),
                    ReceiptLine.Separator()
                ),
                body = listOf(
                    ReceiptLine.KeyValue("Item 1", "$10.00"),
                    ReceiptLine.KeyValue("Item 2", "$15.00"),
                    ReceiptLine.KeyValue("Item 3", "$25.00"),
                    ReceiptLine.Separator('-'),
                    ReceiptLine.KeyValue("TOTAL", "$50.00", FontSize.LARGE, true)
                ),
                footer = listOf(
                    ReceiptLine.Space(1),
                    ReceiptLine.Text("Thank you!", FontSize.MEDIUM, false, Alignment.CENTER),
                    ReceiptLine.Space(2)
                )
            )
            sdk.bixolon.print.printReceipt(receipt).withMessage("Receipt: 3 items, Total=\$50")
        },

        TestCase(
            id = "receipt_02",
            name = "Print Receipt with QR",
            description = "Print receipt including QR code",
            category = TestCategory.PRINT_RECEIPT
        ) {
            val receipt = Receipt(
                header = listOf(
                    ReceiptLine.Text("RECEIPT WITH QR", FontSize.LARGE, true, Alignment.CENTER),
                    ReceiptLine.Separator()
                ),
                body = listOf(
                    ReceiptLine.KeyValue("Order", "#12345"),
                    ReceiptLine.KeyValue("Total", "$99.99"),
                    ReceiptLine.Space(1),
                    ReceiptLine.QR("https://sincpro.com/order/12345", 5, Alignment.CENTER)
                ),
                footer = listOf(
                    ReceiptLine.Space(1),
                    ReceiptLine.Text("Scan for details", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Space(2)
                )
            )
            sdk.bixolon.print.printReceipt(receipt).withMessage("Receipt with QR code")
        },

        TestCase(
            id = "receipt_03",
            name = "Print Receipt with Barcode",
            description = "Print receipt including barcode",
            category = TestCategory.PRINT_RECEIPT
        ) {
            val receipt = Receipt(
                header = listOf(
                    ReceiptLine.Text("RECEIPT WITH BARCODE", FontSize.LARGE, true, Alignment.CENTER),
                    ReceiptLine.Separator()
                ),
                body = listOf(
                    ReceiptLine.KeyValue("Product", "Widget Pro"),
                    ReceiptLine.KeyValue("Price", "$29.99"),
                    ReceiptLine.Space(1),
                    ReceiptLine.Barcode("1234567890", BarcodeType.CODE128, 2, 60, Alignment.CENTER)
                ),
                footer = listOf(
                    ReceiptLine.Space(2)
                )
            )
            sdk.bixolon.print.printReceipt(receipt).withMessage("Receipt with barcode")
        },

        TestCase(
            id = "receipt_04",
            name = "Print Long Receipt",
            description = "Print receipt with many items",
            category = TestCategory.PRINT_RECEIPT
        ) {
            val items = (1..10).map { i ->
                ReceiptLine.KeyValue("Item $i", "$${i * 5}.00")
            }
            val receipt = Receipt(
                header = listOf(
                    ReceiptLine.Text("LONG RECEIPT TEST", FontSize.LARGE, true, Alignment.CENTER),
                    ReceiptLine.Text("10 Items", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Separator()
                ),
                body = items + listOf(
                    ReceiptLine.Separator('='),
                    ReceiptLine.KeyValue("TOTAL", "$275.00", FontSize.LARGE, true)
                ),
                footer = listOf(
                    ReceiptLine.Space(1),
                    ReceiptLine.Text("Thank you for shopping!", FontSize.MEDIUM, false, Alignment.CENTER),
                    ReceiptLine.Space(3)
                )
            )
            sdk.bixolon.print.printReceipt(receipt).withMessage("Long receipt: 10 items")
        },

        TestCase(
            id = "receipt_05",
            name = "Print Multiple Copies",
            description = "Print receipt with 2 copies",
            category = TestCategory.PRINT_RECEIPT
        ) {
            val receipt = Receipt(
                header = listOf(
                    ReceiptLine.Text("COPY TEST", FontSize.LARGE, true, Alignment.CENTER),
                    ReceiptLine.Separator()
                ),
                body = listOf(
                    ReceiptLine.Text("This should print 2 copies", FontSize.MEDIUM, false, Alignment.CENTER)
                ),
                footer = listOf(
                    ReceiptLine.Space(2)
                )
            )
            sdk.bixolon.print.printReceipt(receipt, copies = 2).withMessage("Printed 2 copies")
        },

        TestCase(
            id = "receipt_06",
            name = "Print Invoice (Factura)",
            description = "Print full invoice with columns, 5 items, totals",
            category = TestCategory.PRINT_RECEIPT
        ) {
            val receipt = Receipt(
                header = listOf(
                    // Company header
                    ReceiptLine.Text("SINCPRO S.R.L.", FontSize.LARGE, true, Alignment.CENTER),
                    ReceiptLine.Text("NIT: 123456789", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Text("Av. Principal #123, La Paz", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Text("Tel: (2) 2123456", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Space(1),
                    ReceiptLine.Text("FACTURA", FontSize.LARGE, true, Alignment.CENTER),
                    ReceiptLine.Separator('='),
                    
                    // Invoice info with columns
                    ReceiptLine.Columns(
                        ReceiptLine.Column("No. Factura:", 0.4f),
                        ReceiptLine.Column("F-001234", 0.6f, Alignment.RIGHT)
                    ),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("Fecha:", 0.4f),
                        ReceiptLine.Column("14/01/2026", 0.6f, Alignment.RIGHT)
                    ),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("Hora:", 0.4f),
                        ReceiptLine.Column("15:30:45", 0.6f, Alignment.RIGHT)
                    ),
                    ReceiptLine.Separator(),
                    
                    // Customer info
                    ReceiptLine.Columns(
                        ReceiptLine.Column("Cliente:", 0.3f),
                        ReceiptLine.Column("Andres Gutierrez", 0.7f)
                    ),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("NIT/CI:", 0.3f),
                        ReceiptLine.Column("9876543", 0.7f)
                    ),
                    ReceiptLine.Separator('=')
                ),
                body = listOf(
                    // Items header
                    ReceiptLine.Columns(
                        ReceiptLine.Column("CANT", 0.12f),
                        ReceiptLine.Column("DESCRIPCION", 0.48f),
                        ReceiptLine.Column("P.U.", 0.20f, Alignment.RIGHT),
                        ReceiptLine.Column("TOTAL", 0.20f, Alignment.RIGHT),
                        fontSize = FontSize.SMALL,
                        bold = true
                    ),
                    ReceiptLine.Separator('-'),
                    
                    // Item 1
                    ReceiptLine.Columns(
                        ReceiptLine.Column("2", 0.12f),
                        ReceiptLine.Column("Producto Alpha", 0.48f),
                        ReceiptLine.Column("50.00", 0.20f, Alignment.RIGHT),
                        ReceiptLine.Column("100.00", 0.20f, Alignment.RIGHT),
                        fontSize = FontSize.SMALL
                    ),
                    // Item 2
                    ReceiptLine.Columns(
                        ReceiptLine.Column("1", 0.12f),
                        ReceiptLine.Column("Servicio Beta Plus", 0.48f),
                        ReceiptLine.Column("150.00", 0.20f, Alignment.RIGHT),
                        ReceiptLine.Column("150.00", 0.20f, Alignment.RIGHT),
                        fontSize = FontSize.SMALL
                    ),
                    // Item 3
                    ReceiptLine.Columns(
                        ReceiptLine.Column("3", 0.12f),
                        ReceiptLine.Column("Accesorio Gamma", 0.48f),
                        ReceiptLine.Column("25.00", 0.20f, Alignment.RIGHT),
                        ReceiptLine.Column("75.00", 0.20f, Alignment.RIGHT),
                        fontSize = FontSize.SMALL
                    ),
                    // Item 4
                    ReceiptLine.Columns(
                        ReceiptLine.Column("5", 0.12f),
                        ReceiptLine.Column("Material Delta", 0.48f),
                        ReceiptLine.Column("10.00", 0.20f, Alignment.RIGHT),
                        ReceiptLine.Column("50.00", 0.20f, Alignment.RIGHT),
                        fontSize = FontSize.SMALL
                    ),
                    // Item 5
                    ReceiptLine.Columns(
                        ReceiptLine.Column("1", 0.12f),
                        ReceiptLine.Column("Premium Epsilon", 0.48f),
                        ReceiptLine.Column("125.00", 0.20f, Alignment.RIGHT),
                        ReceiptLine.Column("125.00", 0.20f, Alignment.RIGHT),
                        fontSize = FontSize.SMALL
                    ),
                    
                    ReceiptLine.Separator('='),
                    
                    // Totals
                    ReceiptLine.Columns(
                        ReceiptLine.Column("SUBTOTAL:", 0.6f),
                        ReceiptLine.Column("Bs 500.00", 0.4f, Alignment.RIGHT)
                    ),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("DESCUENTO (10%):", 0.6f),
                        ReceiptLine.Column("-Bs 50.00", 0.4f, Alignment.RIGHT)
                    ),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("BASE IMPONIBLE:", 0.6f),
                        ReceiptLine.Column("Bs 450.00", 0.4f, Alignment.RIGHT)
                    ),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("IVA (13%):", 0.6f),
                        ReceiptLine.Column("Bs 58.50", 0.4f, Alignment.RIGHT)
                    ),
                    ReceiptLine.Separator(),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("TOTAL A PAGAR:", 0.5f),
                        ReceiptLine.Column("Bs 508.50", 0.5f, Alignment.RIGHT),
                        fontSize = FontSize.LARGE,
                        bold = true
                    ),
                    ReceiptLine.Space(1),
                    
                    // Payment info
                    ReceiptLine.Separator(),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("Efectivo:", 0.6f),
                        ReceiptLine.Column("Bs 600.00", 0.4f, Alignment.RIGHT)
                    ),
                    ReceiptLine.Columns(
                        ReceiptLine.Column("Cambio:", 0.6f),
                        ReceiptLine.Column("Bs 91.50", 0.4f, Alignment.RIGHT)
                    )
                ),
                footer = listOf(
                    ReceiptLine.Space(1),
                    ReceiptLine.Separator('='),
                    ReceiptLine.Text("CODIGO DE CONTROL", FontSize.SMALL, true, Alignment.CENTER),
                    ReceiptLine.Text("A1-B2-C3-D4-E5", FontSize.MEDIUM, false, Alignment.CENTER),
                    ReceiptLine.Space(1),
                    ReceiptLine.QR("https://sincpro.com/factura/F-001234", 5, Alignment.CENTER),
                    ReceiptLine.Text("Escanee para verificar", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Space(1),
                    ReceiptLine.Separator(),
                    ReceiptLine.Text("Esta factura contribuye al", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Text("desarrollo del pais.", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Text("El cambio no constituye pago.", FontSize.SMALL, false, Alignment.CENTER),
                    ReceiptLine.Space(1),
                    ReceiptLine.Text("*** GRACIAS POR SU COMPRA ***", FontSize.MEDIUM, true, Alignment.CENTER),
                    ReceiptLine.Space(3)
                )
            )
            sdk.bixolon.print.printReceipt(receipt).withMessage("Invoice: 5 items, Total=Bs 508.50")
        }
    )

    private fun errorHandlingTests() = listOf(
        TestCase(
            id = "error_01",
            name = "Print Without Connection",
            description = "Try to print when disconnected (should fail)",
            category = TestCategory.ERROR_HANDLING,
            requiresConnection = false
        ) {
            if (sdk.bixolon.connectivity.isConnected()) {
                Result.failure(Exception("Test skipped: Already connected"))
            } else {
                val result = sdk.bixolon.print.printText("This should fail")
                if (result.isFailure) {
                    Result.success("Expected error: ${result.exceptionOrNull()?.message}")
                } else {
                    Result.failure(Exception("Expected failure but got success"))
                }
            }
        },

        TestCase(
            id = "error_02",
            name = "Connect Invalid Address",
            description = "Try connecting to invalid MAC address",
            category = TestCategory.ERROR_HANDLING,
            requiresConnection = false
        ) {
            val result = sdk.bixolon.connectivity.connectBluetooth(
                address = "00:00:00:00:00:00",
                timeoutMs = 3000
            )
            if (result.isFailure) {
                Result.success("Expected error: ${result.exceptionOrNull()?.message}")
            } else {
                Result.failure(Exception("Expected connection failure"))
            }
        },

        TestCase(
            id = "error_03",
            name = "Print Empty Text",
            description = "Print empty string",
            category = TestCategory.ERROR_HANDLING
        ) {
            sdk.bixolon.print.printText("").withMessage("Empty text sent")
        },

        TestCase(
            id = "error_04",
            name = "Print Very Long Text",
            description = "Print text that exceeds paper width",
            category = TestCategory.ERROR_HANDLING
        ) {
            sdk.bixolon.print.printText(
                "A".repeat(200)
            ).withMessage("200 chars sent")
        },

        TestCase(
            id = "error_05",
            name = "Print Invalid Barcode Data",
            description = "Print EAN13 with wrong digit count",
            category = TestCategory.ERROR_HANDLING
        ) {
            sdk.bixolon.print.printBarcode(
                data = "123", // EAN13 requires 13 digits
                type = BarcodeType.EAN13
            ).withMessage("Invalid EAN13 sent")
        },

        TestCase(
            id = "error_06",
            name = "Double Disconnect",
            description = "Call disconnect twice",
            category = TestCategory.ERROR_HANDLING,
            requiresConnection = false
        ) {
            sdk.bixolon.connectivity.disconnect()
            sdk.bixolon.connectivity.disconnect().withMessage("Double disconnect OK")
        },

        TestCase(
            id = "error_07",
            name = "Print Special Characters",
            description = "Print text with special chars (UTF-8)",
            category = TestCategory.ERROR_HANDLING
        ) {
            sdk.bixolon.print.printText("Café ñoño €100 日本語").withMessage("UTF-8 chars sent")
        }
    )

    private fun configurationTests() = listOf(
        TestCase(
            id = "config_01",
            name = "Print on 58mm Paper",
            description = "Print using 58mm media config",
            category = TestCategory.CONFIGURATION
        ) {
            sdk.bixolon.print.printText(
                text = "58mm Paper Test",
                media = MediaConfig.continuous58mm()
            ).withMessage("Printed on 58mm (384 dots)")
        },

        TestCase(
            id = "config_02",
            name = "Print on 80mm Paper",
            description = "Print using 80mm media config (default)",
            category = TestCategory.CONFIGURATION
        ) {
            sdk.bixolon.print.printText(
                text = "80mm Paper Test (Default)",
                media = MediaConfig.continuous80mm()
            ).withMessage("Printed on 80mm (576 dots)")
        },

        TestCase(
            id = "config_03",
            name = "Get DPI",
            description = "Get printer DPI value",
            category = TestCategory.CONFIGURATION
        ) {
            val dpi = sdk.bixolon.connectivity.getDpi()
            Result.success("Printer DPI: $dpi")
        },

        TestCase(
            id = "config_04",
            name = "Print Image - Test Pattern",
            description = "Print a test pattern image",
            category = TestCategory.CONFIGURATION
        ) {
            // Create a simple test pattern bitmap
            val width = 200
            val height = 100
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint()
            
            // Draw white background
            canvas.drawColor(android.graphics.Color.WHITE)
            
            // Draw black rectangle
            paint.color = android.graphics.Color.BLACK
            canvas.drawRect(10f, 10f, 190f, 90f, paint)
            
            // Draw white text area
            paint.color = android.graphics.Color.WHITE  
            canvas.drawRect(20f, 30f, 180f, 70f, paint)
            
            // Print using printImage API
            sdk.bixolon.print.printImage(bitmap, Alignment.CENTER).withMessage("Image: 200x100 test pattern")
        },

        TestCase(
            id = "config_05",
            name = "Print Image - Gradient",
            description = "Print gradient test pattern",
            category = TestCategory.CONFIGURATION
        ) {
            // Create a gradient test pattern
            val width = 300
            val height = 100
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
            val canvas = android.graphics.Canvas(bitmap)
            
            // Draw gradient from white to black
            val paint = android.graphics.Paint()
            for (x in 0 until width) {
                val gray = 255 - (x * 255 / width)
                paint.color = android.graphics.Color.rgb(gray, gray, gray)
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
            }
            
            sdk.bixolon.print.printImage(bitmap, Alignment.LEFT).withMessage("Image: 300x100 gradient")
        }
    )

    // ============================================================
    // Binary Tests (PNG/PDF from raw resources)
    // ============================================================
    
    private fun printBinaryTests() = listOf(
        // Test that decodes PNG manually like config_04/05 do (they work!)
        TestCase(
            id = "binary_00",
            name = "Print PNG - Manual Decode",
            description = "Decode PNG manually and draw on white canvas",
            category = TestCategory.PRINT_IMAGE
        ) {
            // Read raw bytes directly (not base64)
            val bytes = context.resources.openRawResource(R.raw.sincpro_simbolo).use { it.readBytes() }
            
            // Decode to bitmap
            val original = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (original == null) {
                return@TestCase Result.failure(Exception("Failed to decode PNG"))
            }
            
            android.util.Log.d("TEST", "Original: ${original.width}x${original.height}, config=${original.config}, hasAlpha=${original.hasAlpha()}")
            
            // Create RGB_565 bitmap with white background (like config_04 does)
            val printBitmap = android.graphics.Bitmap.createBitmap(
                original.width, 
                original.height, 
                android.graphics.Bitmap.Config.RGB_565
            )
            val canvas = android.graphics.Canvas(printBitmap)
            
            // WHITE background first!
            canvas.drawColor(android.graphics.Color.WHITE)
            
            // Draw original on top
            canvas.drawBitmap(original, 0f, 0f, null)
            
            original.recycle()
            
            android.util.Log.d("TEST", "Print bitmap: ${printBitmap.width}x${printBitmap.height}, config=${printBitmap.config}")
            
            // Print using printImage API
            sdk.bixolon.print.printImage(printBitmap, Alignment.CENTER)
                .withMessage("PNG: ${printBitmap.width}x${printBitmap.height}")
        },

        TestCase(
            id = "binary_01",
            name = "Print PNG from Raw",
            description = "Print 'Sincpro Simbolo.png' from raw resources",
            category = TestCategory.PRINT_IMAGE
        ) {
            val base64 = readRawResourceAsBase64(R.raw.sincpro_simbolo)
            if (base64 != null) {
                sdk.bixolon.print.printImageBase64(
                    base64Data = base64,
                    alignment = Alignment.CENTER
                ).withMessage("PNG: ${base64.length} bytes base64")
            } else {
                Result.failure(Exception("Failed to read PNG resource"))
            }
        },

        TestCase(
            id = "binary_02",
            name = "Print PNG - Left Aligned",
            description = "Print PNG image aligned to left",
            category = TestCategory.PRINT_IMAGE
        ) {
            val base64 = readRawResourceAsBase64(R.raw.sincpro_simbolo)
            if (base64 != null) {
                sdk.bixolon.print.printImageBase64(
                    base64Data = base64,
                    alignment = Alignment.LEFT
                ).withMessage("PNG: LEFT aligned")
            } else {
                Result.failure(Exception("Failed to read PNG resource"))
            }
        },

        TestCase(
            id = "binary_03",
            name = "Print PNG - Right Aligned",
            description = "Print PNG image aligned to right",
            category = TestCategory.PRINT_IMAGE
        ) {
            val base64 = readRawResourceAsBase64(R.raw.sincpro_simbolo)
            if (base64 != null) {
                sdk.bixolon.print.printImageBase64(
                    base64Data = base64,
                    alignment = Alignment.RIGHT
                ).withMessage("PNG: RIGHT aligned")
            } else {
                Result.failure(Exception("Failed to read PNG resource"))
            }
        },

        TestCase(
            id = "binary_04",
            name = "Print PDF Page 1",
            description = "Print first page of 'comprobante' PDF",
            category = TestCategory.PRINT_PDF
        ) {
            val base64 = readRawResourceAsBase64(R.raw.comprobante_1767994755165)
            if (base64 != null) {
                val pageCount = sdk.bixolon.print.getPdfPageCount(base64)
                sdk.bixolon.print.printPdfBase64(
                    base64Data = base64,
                    page = 1
                ).withMessage("PDF: page 1 of $pageCount")
            } else {
                Result.failure(Exception("Failed to read PDF resource"))
            }
        },

        TestCase(
            id = "binary_05",
            name = "Print PDF - All Pages",
            description = "Print all pages of PDF",
            category = TestCategory.PRINT_PDF
        ) {
            val base64 = readRawResourceAsBase64(R.raw.comprobante_1767994755165)
            if (base64 != null) {
                val pageCount = sdk.bixolon.print.getPdfPageCount(base64)
                var lastResult: Result<Unit> = Result.success(Unit)
                for (page in 1..pageCount) {
                    lastResult = sdk.bixolon.print.printPdfBase64(
                        base64Data = base64,
                        page = page
                    )
                    if (lastResult.isFailure) break
                }
                lastResult.withMessage("PDF: printed all $pageCount pages")
            } else {
                Result.failure(Exception("Failed to read PDF resource"))
            }
        },

        TestCase(
            id = "binary_06",
            name = "Print PDF - Page 2",
            description = "Print second page of PDF if exists",
            category = TestCategory.PRINT_PDF
        ) {
            val base64 = readRawResourceAsBase64(R.raw.comprobante_1767994755165)
            if (base64 != null) {
                val pageCount = sdk.bixolon.print.getPdfPageCount(base64)
                if (pageCount >= 2) {
                    sdk.bixolon.print.printPdfBase64(
                        base64Data = base64,
                        page = 2
                    ).withMessage("PDF: printed page 2 of $pageCount")
                } else {
                    Result.success("PDF only has $pageCount page(s), page 2 does not exist")
                }
            } else {
                Result.failure(Exception("Failed to read PDF resource"))
            }
        },

        TestCase(
            id = "binary_07",
            name = "Print PNG - 58mm Paper",
            description = "Print PNG on 58mm paper width",
            category = TestCategory.PRINT_IMAGE
        ) {
            val base64 = readRawResourceAsBase64(R.raw.sincpro_simbolo)
            if (base64 != null) {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                sdk.bixolon.print.printImageBase64(
                    base64Data = base64,
                    alignment = Alignment.CENTER,
                    media = MediaConfig.continuous58mm()
                ).withMessage("PNG: ${bytes.size} bytes, 58mm paper")
            } else {
                Result.failure(Exception("Failed to read PNG resource"))
            }
        },

        // Diagnostic test
        TestCase(
            id = "binary_diag_01",
            name = "DIAGNOSTIC: PDF File Check",
            description = "Check PDF file creation and reading",
            category = TestCategory.PRINT_PDF
        ) {
            val base64 = readRawResourceAsBase64(R.raw.comprobante_1767994755165)
            if (base64 != null) {
                // Decode to bytes
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                
                // Check PDF header (should be %PDF)
                val header = String(bytes.take(8).toByteArray())
                val isPdf = header.startsWith("%PDF")
                
                // Try to get page count
                val pageCount = sdk.bixolon.print.getPdfPageCount(base64)
                
                if (isPdf && pageCount > 0) {
                    Result.success("PDF valid: ${bytes.size} bytes, header='$header', pages=$pageCount")
                } else {
                    Result.failure(Exception("PDF validation failed: isPdf=$isPdf, pages=$pageCount"))
                }
            } else {
                Result.failure(Exception("Failed to read PDF resource"))
            }
        },

        TestCase(
            id = "binary_diag_02",
            name = "DIAGNOSTIC: Image Check",
            description = "Check image decoding",
            category = TestCategory.PRINT_IMAGE
        ) {
            val base64 = readRawResourceAsBase64(R.raw.sincpro_simbolo)
            if (base64 != null) {
                // Decode to bytes
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                
                // Check PNG header (should be 0x89 PNG)
                val header = bytes.take(8).map { it.toInt() and 0xFF }
                val isPng = header[0] == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                val formatType = if (isPng) "PNG" else "JPEG/Other"
                
                // Try to decode bitmap
                val options = android.graphics.BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                
                if (bitmap != null) {
                    Result.success("Image: $formatType, ${bytes.size} bytes, ${bitmap.width}x${bitmap.height}px, config=${bitmap.config}")
                } else {
                    Result.failure(Exception("Failed to decode bitmap"))
                }
            } else {
                Result.failure(Exception("Failed to read PNG resource"))
            }
        }
    )

    // ============================================================
    // Helper: Read raw resource as Base64
    // ============================================================
    
    private fun readRawResourceAsBase64(resourceId: Int): String? {
        return try {
            context.resources.openRawResource(resourceId).use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            println("Error reading raw resource: ${e.message}")
            null
        }
    }
}
