package sincpro.expo.printer.domain

/**
 * DOMAIN - Receipt
 *
 * Represents a generic receipt with header/details/footer sections.
 * This is NOT a business model (Invoice, Ticket, etc.)
 * It's a layout structure for printing.
 *
 * Clients should convert their business models to Receipt before printing.
 */
data class Receipt(
    val header: List<ReceiptLine> = emptyList(),
    val details: List<ReceiptLine> = emptyList(),
    val footer: List<ReceiptLine> = emptyList(),
) {
    /**
     * Builder for fluent receipt construction
     */
    class Builder {
        private val header = mutableListOf<ReceiptLine>()
        private val details = mutableListOf<ReceiptLine>()
        private val footer = mutableListOf<ReceiptLine>()

        fun addHeader(line: ReceiptLine) = apply { header.add(line) }

        fun addDetail(line: ReceiptLine) = apply { details.add(line) }

        fun addFooter(line: ReceiptLine) = apply { footer.add(line) }

        fun addHeader(vararg lines: ReceiptLine) = apply { header.addAll(lines) }

        fun addDetail(vararg lines: ReceiptLine) = apply { details.addAll(lines) }

        fun addFooter(vararg lines: ReceiptLine) = apply { footer.addAll(lines) }

        fun build() =
            Receipt(
                header = header.toList(),
                details = details.toList(),
                footer = footer.toList(),
            )
    }

    companion object {
        fun builder() = Builder()
    }
}

/**
 * Receipt line types (sealed class = type-safe union)
 */
sealed class ReceiptLine {
    /**
     * Simple text line
     */
    data class Text(
        val content: String,
        val fontSize: FontSize = FontSize.MEDIUM,
        val bold: Boolean = false,
        val alignment: Alignment = Alignment.LEFT,
    ) : ReceiptLine()

    /**
     * Key-value pair (e.g., "Total: $100")
     */
    data class KeyValue(
        val key: String,
        val value: String,
        val fontSize: FontSize = FontSize.MEDIUM,
        val bold: Boolean = false,
    ) : ReceiptLine()

    /**
     * QR Code
     */
    data class QRCode(
        val data: String,
        val size: Int = 5,
        val alignment: Alignment = Alignment.CENTER,
    ) : ReceiptLine()

    /**
     * Horizontal separator line
     */
    data class Separator(
        val char: String = "-",
        val length: Int = 48,
    ) : ReceiptLine()

    /**
     * Blank space
     */
    data class Space(
        val lines: Int = 1,
    ) : ReceiptLine()

    /**
     * Bitmap image
     */
    data class Image(
        val bitmap: android.graphics.Bitmap,
        val alignment: Alignment = Alignment.CENTER,
    ) : ReceiptLine()
}
