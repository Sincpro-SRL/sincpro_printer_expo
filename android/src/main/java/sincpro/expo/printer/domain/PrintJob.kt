package sincpro.expo.printer.domain

/** Mirrors src/domain/print_job.ts: positions are already laid out in printer dots by TypeScript. */

enum class MediaType { CONTINUOUS, GAP, BLACK_MARK }

data class Media(val widthDots: Int, val heightDots: Int, val gapDots: Int, val type: MediaType)

enum class QrErrorCorrection { L, M, Q, H }

sealed class PrintCommand {
    abstract val x: Int
    abstract val y: Int
    abstract val height: Int

    data class Text(
        override val x: Int,
        override val y: Int,
        override val height: Int,
        val text: String,
        val font: Int,
        val bold: Boolean,
    ) : PrintCommand()

    data class Qr(
        override val x: Int,
        override val y: Int,
        override val height: Int,
        val data: String,
        val size: Int,
        val errorCorrection: QrErrorCorrection,
    ) : PrintCommand()

    data class Barcode(
        override val x: Int,
        override val y: Int,
        override val height: Int,
        val data: String,
        val symbology: Int,
        val narrowBarWidth: Int,
        val wideBarWidth: Int,
        val barHeight: Int,
        val showText: Boolean,
    ) : PrintCommand()

    data class Image(
        override val x: Int,
        override val y: Int,
        override val height: Int,
        val base64: String,
        val width: Int,
    ) : PrintCommand()

    data class PdfPage(
        override val x: Int,
        override val y: Int,
        override val height: Int,
        val base64: String,
        val page: Int,
        val width: Int,
    ) : PrintCommand()
}

data class PrintJob(val media: Media, val copies: Int, val commands: List<PrintCommand>)
