package sincpro.expo.printer.entrypoint

import android.content.Context
import com.sincpro.printer.SincproPrinterSdk
import com.sincpro.printer.domain.Alignment
import com.sincpro.printer.domain.BarcodeType
import com.sincpro.printer.domain.FontSize
import com.sincpro.printer.domain.MediaConfig
import com.sincpro.printer.domain.Receipt
import com.sincpro.printer.domain.ReceiptLine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class PrinterModule : Module() {
    private lateinit var sdk: SincproPrinterSdk

    override fun definition() =
        ModuleDefinition {
            Name("SincproPrinter")

            OnCreate {
                val context = appContext.reactContext as Context
                sdk = SincproPrinterSdk(context)
            }

            // ============================================================
            // BLUETOOTH API
            // ============================================================

            Function("getPairedDevices") {
                sdk.bixolon.connectivity
                    .getPairedDevices()
                    .map { devices ->
                        devices.map { device ->
                            mapOf(
                                "name" to device.name,
                                "address" to device.address,
                                "isPrinter" to device.isPrinter,
                            )
                        }
                    }.getOrElse { emptyList() }
            }

            Function("getPairedPrinters") {
                sdk.bixolon.connectivity
                    .getPairedPrinters()
                    .map { printers ->
                        printers.map { printer ->
                            mapOf(
                                "name" to printer.name,
                                "address" to printer.address,
                            )
                        }
                    }.getOrElse { emptyList() }
            }

            // ============================================================
            // CONNECTION API
            // ============================================================

            AsyncFunction("connectBluetooth") { address: String, timeoutMs: Double? ->
                sdk.bixolon.connectivity
                    .connectBluetooth(
                        address = address,
                        timeoutMs = timeoutMs?.toLong() ?: 10000,
                    ).getOrThrow()
            }

            AsyncFunction("connectWifi") { ip: String, port: Int?, timeoutMs: Double? ->
                sdk.bixolon.connectivity
                    .connectWifi(
                        ip = ip,
                        port = port ?: 9100,
                        timeoutMs = timeoutMs?.toLong() ?: 10000,
                    ).getOrThrow()
            }

            AsyncFunction("connectUsb") {
                sdk.bixolon.connectivity
                    .connectUsb()
                    .getOrThrow()
            }

            AsyncFunction("disconnect") {
                sdk.bixolon.connectivity
                    .disconnect()
                    .getOrThrow()
            }

            Function("isConnected") {
                sdk.bixolon.connectivity.isConnected()
            }

            AsyncFunction("getStatus") {
                val status =
                    sdk.bixolon.connectivity
                        .getStatus()
                        .getOrThrow()
                mapOf(
                    "connectionState" to status.connectionState.name,
                    "hasPaper" to status.hasPaper,
                    "isCoverOpen" to status.isCoverOpen,
                    "isOverheated" to status.isOverheated,
                    "hasError" to status.hasError,
                    "errorMessage" to status.errorMessage,
                )
            }

            AsyncFunction("getInfo") {
                val info =
                    sdk.bixolon.connectivity
                        .getInfo()
                        .getOrThrow()
                mapOf(
                    "model" to info.model,
                    "firmware" to info.firmware,
                    "serial" to info.serial,
                    "dpi" to info.dpi,
                )
            }

            Function("getDpi") {
                sdk.bixolon.connectivity.getDpi()
            }

            // ============================================================
            // PRINT API - Text
            // ============================================================

            AsyncFunction("printText") { text: String, options: Map<String, Any?>? ->
                val fontSize = parseFontSize(options?.get("fontSize") as? String)
                val alignment = parseAlignment(options?.get("alignment") as? String)
                val bold = options?.get("bold") as? Boolean ?: false
                val media = parseMediaConfig(options?.get("media") as? Map<String, Any?>)

                sdk.bixolon.print
                    .printText(text, fontSize, alignment, bold, media)
                    .getOrThrow()
            }

            AsyncFunction("printTexts") { texts: List<String>, options: Map<String, Any?>? ->
                val fontSize = parseFontSize(options?.get("fontSize") as? String)
                val media = parseMediaConfig(options?.get("media") as? Map<String, Any?>)

                sdk.bixolon.print
                    .printTexts(texts, fontSize, media)
                    .getOrThrow()
            }

            // ============================================================
            // PRINT API - QR & Barcode
            // ============================================================

            AsyncFunction("printQR") { data: String, options: Map<String, Any?>? ->
                val size = (options?.get("size") as? Number)?.toInt() ?: 5
                val alignment = parseAlignment(options?.get("alignment") as? String)
                val media = parseMediaConfig(options?.get("media") as? Map<String, Any?>)

                sdk.bixolon.print
                    .printQR(data, size, alignment, media)
                    .getOrThrow()
            }

            AsyncFunction("printBarcode") { data: String, options: Map<String, Any?>? ->
                val type = parseBarcodeType(options?.get("type") as? String)
                val height = (options?.get("height") as? Number)?.toInt() ?: 60
                val alignment = parseAlignment(options?.get("alignment") as? String)
                val media = parseMediaConfig(options?.get("media") as? Map<String, Any?>)

                sdk.bixolon.print
                    .printBarcode(data, type, height, alignment, media)
                    .getOrThrow()
            }

            // ============================================================
            // PRINT API - Images & PDF
            // ============================================================

            AsyncFunction("printImageBase64") { base64Data: String, options: Map<String, Any?>? ->
                val alignment = parseAlignment(options?.get("alignment") as? String)
                val media = parseMediaConfig(options?.get("media") as? Map<String, Any?>)

                sdk.bixolon.print
                    .printImageBase64(base64Data, alignment, media)
                    .getOrThrow()
            }

            AsyncFunction("printPdfBase64") { base64Data: String, options: Map<String, Any?>? ->
                val page = (options?.get("page") as? Number)?.toInt() ?: 1
                val alignment = parseAlignment(options?.get("alignment") as? String)
                val media = parseMediaConfig(options?.get("media") as? Map<String, Any?>)

                sdk.bixolon.print
                    .printPdfBase64(base64Data, page, alignment, media)
                    .getOrThrow()
            }

            Function("getPdfPageCount") { base64Data: String ->
                sdk.bixolon.print.getPdfPageCount(base64Data)
            }

            // ============================================================
            // PRINT API - Receipt (High Level)
            // ============================================================

            AsyncFunction("printReceipt") { receiptData: Map<String, Any?>, options: Map<String, Any?>? ->
                val receipt = parseReceipt(receiptData)
                val media = parseMediaConfig(options?.get("media") as? Map<String, Any?>)
                val copies = (options?.get("copies") as? Number)?.toInt() ?: 1

                sdk.bixolon.print
                    .printReceipt(receipt, media, copies)
                    .getOrThrow()
            }

            // ============================================================
            // PRINT API - Columns (Key-Value style)
            // ============================================================

            AsyncFunction("printKeyValue") { key: String, value: String, options: Map<String, Any?>? ->
                val fontSize = parseFontSize(options?.get("fontSize") as? String)
                val bold = options?.get("bold") as? Boolean ?: false
                val media = parseMediaConfig(options?.get("media") as? Map<String, Any?>)

                sdk.bixolon.print
                    .printKeyValue(key, value, fontSize, bold, media)
                    .getOrThrow()
            }
        }

    // ============================================================
    // PARSERS
    // ============================================================

    private fun parseFontSize(value: String?): FontSize =
        when (value?.lowercase()) {
            "small" -> FontSize.SMALL
            "medium" -> FontSize.MEDIUM
            "large" -> FontSize.LARGE
            "xlarge" -> FontSize.XLARGE
            else -> FontSize.MEDIUM
        }

    private fun parseAlignment(value: String?): Alignment =
        when (value?.lowercase()) {
            "left" -> Alignment.LEFT
            "center" -> Alignment.CENTER
            "right" -> Alignment.RIGHT
            else -> Alignment.LEFT
        }

    private fun parseBarcodeType(value: String?): BarcodeType =
        when (value?.uppercase()) {
            "CODE128" -> BarcodeType.CODE128
            "CODE39" -> BarcodeType.CODE39
            "EAN13" -> BarcodeType.EAN13
            "EAN8" -> BarcodeType.EAN8
            "UPCA" -> BarcodeType.UPCA
            "UPCE" -> BarcodeType.UPCE
            "CODE93" -> BarcodeType.CODE93
            "CODABAR" -> BarcodeType.CODABAR
            else -> BarcodeType.CODE128
        }

    private fun parseMediaConfig(data: Map<String, Any?>?): MediaConfig {
        if (data == null) return MediaConfig.continuous80mm()

        val preset = data["preset"] as? String
        if (preset != null) {
            return when (preset) {
                "continuous58mm" -> MediaConfig.continuous58mm()
                "continuous80mm" -> MediaConfig.continuous80mm()
                else -> MediaConfig.continuous80mm()
            }
        }

        val widthDots = (data["widthDots"] as? Number)?.toInt() ?: 640
        val heightDots = (data["heightDots"] as? Number)?.toInt() ?: 0

        return MediaConfig(widthDots, heightDots)
    }

    private fun parseReceipt(data: Map<String, Any?>): Receipt {
        val header = parseReceiptLines(data["header"] as? List<*>)
        val body = parseReceiptLines(data["body"] as? List<*>)
        val footer = parseReceiptLines(data["footer"] as? List<*>)

        return Receipt(header, body, footer)
    }

    private fun parseReceiptLines(data: List<*>?): List<ReceiptLine> {
        if (data == null) return emptyList()

        return data.mapNotNull { item ->
            val lineData = item as? Map<String, Any?> ?: return@mapNotNull null
            parseReceiptLine(lineData)
        }
    }

    private fun parseReceiptLine(data: Map<String, Any?>): ReceiptLine? {
        val type = data["type"] as? String ?: return null

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

            "qr" -> {
                ReceiptLine.QR(
                    data = data["data"] as? String ?: "",
                    size = (data["size"] as? Number)?.toInt() ?: 5,
                    alignment = parseAlignment(data["alignment"] as? String),
                )
            }

            "barcode" -> {
                ReceiptLine.Barcode(
                    data = data["data"] as? String ?: "",
                    type = parseBarcodeType(data["barcodeType"] as? String),
                    height = (data["height"] as? Number)?.toInt() ?: 60,
                    alignment = parseAlignment(data["alignment"] as? String),
                )
            }

            "image" -> {
                val base64 = data["base64"] as? String ?: return null
                val bitmap =
                    com.sincpro.printer.infrastructure.BinaryConverter
                        .base64ToBitmap(base64)
                        ?: return null
                ReceiptLine.Image(
                    bitmap = bitmap,
                    alignment = parseAlignment(data["alignment"] as? String),
                )
            }

            "separator" -> {
                ReceiptLine.Separator(
                    char = (data["char"] as? String)?.firstOrNull() ?: '-',
                    length = (data["length"] as? Number)?.toInt() ?: 48,
                )
            }

            "space" -> {
                ReceiptLine.Space(
                    lines = (data["lines"] as? Number)?.toInt() ?: 1,
                )
            }

            "columns" -> {
                val columnsData = data["columns"] as? List<*> ?: return null
                val columns =
                    columnsData.mapNotNull { col ->
                        val colData = col as? Map<String, Any?> ?: return@mapNotNull null
                        ReceiptLine.Column(
                            text = colData["text"] as? String ?: "",
                            widthRatio = (colData["widthRatio"] as? Number)?.toFloat() ?: 0.5f,
                            alignment = parseAlignment(colData["alignment"] as? String),
                        )
                    }
                ReceiptLine.Columns(
                    columns = columns,
                    fontSize = parseFontSize(data["fontSize"] as? String),
                    bold = data["bold"] as? Boolean ?: false,
                )
            }

            else -> {
                null
            }
        }
    }
}
