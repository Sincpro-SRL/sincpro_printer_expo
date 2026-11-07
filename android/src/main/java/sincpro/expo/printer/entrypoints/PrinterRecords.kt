package sincpro.expo.printer.entrypoints

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import sincpro.expo.printer.domain.Density
import sincpro.expo.printer.domain.Media
import sincpro.expo.printer.domain.MediaType
import sincpro.expo.printer.domain.Orientation
import sincpro.expo.printer.domain.PrintCommand
import sincpro.expo.printer.domain.PrintJob
import sincpro.expo.printer.domain.PrinterConfig
import sincpro.expo.printer.domain.PrinterErrorCode
import sincpro.expo.printer.domain.PrinterException
import sincpro.expo.printer.domain.QrErrorCorrection
import sincpro.expo.printer.domain.Speed

/** Transport shapes for the JS bridge; `toDomain()` keeps Expo types out of the inner layers. */

class MediaRecord : Record {
    @Field val widthDots: Int = 640

    @Field val heightDots: Int = 0

    @Field val gapDots: Int = 0

    @Field val type: String = "continuous"

    fun toDomain() =
        Media(
            widthDots = widthDots,
            heightDots = heightDots,
            gapDots = gapDots,
            type =
                when (type) {
                    "gap" -> MediaType.GAP
                    "black_mark" -> MediaType.BLACK_MARK
                    else -> MediaType.CONTINUOUS
                },
        )
}

/** One flat record for every command kind: fields not used by a kind keep their defaults. */
class PrintCommandRecord : Record {
    @Field val type: String = ""

    @Field val x: Int = 0

    @Field val y: Int = 0

    @Field val height: Int = 0

    @Field val text: String = ""

    @Field val font: Int = 51

    @Field val bold: Boolean = false

    @Field val data: String = ""

    @Field val size: Int = 5

    @Field val errorCorrection: String = "M"

    @Field val symbology: Int = 1

    @Field val narrowBarWidth: Int = 2

    @Field val wideBarWidth: Int = 6

    @Field val barHeight: Int = 60

    @Field val showText: Boolean = true

    @Field val base64: String = ""

    @Field val width: Int = 0

    @Field val page: Int = 1

    fun toDomain(): PrintCommand =
        when (type) {
            "text" -> PrintCommand.Text(x, y, height, text, font, bold)
            "qr" -> PrintCommand.Qr(x, y, height, data, size, QrErrorCorrection.valueOf(errorCorrection))
            "barcode" -> PrintCommand.Barcode(x, y, height, data, symbology, narrowBarWidth, wideBarWidth, barHeight, showText)
            "image" -> PrintCommand.Image(x, y, height, base64, width)
            "pdf" -> PrintCommand.PdfPage(x, y, height, base64, page, width)
            else -> throw PrinterException(PrinterErrorCode.INVALID_ARGUMENT, "Unknown print command \"$type\"")
        }
}

class PrintJobRecord : Record {
    @Field val media: MediaRecord = MediaRecord()

    @Field val copies: Int = 1

    @Field val commands: List<PrintCommandRecord> = emptyList()

    fun toDomain() = PrintJob(media.toDomain(), copies.coerceIn(1, 65535), commands.map { it.toDomain() })
}

class CutterRecord : Record {
    @Field val enabled: Boolean = false

    @Field val fullCut: Boolean = true
}

/** Missing fields fall back to the 1.x defaults, except speed, which stays untouched when unset. */
class PrinterConfigRecord : Record {
    @Field val marginLeft: Int = 0

    @Field val marginTop: Int = 0

    @Field val density: String? = null

    @Field val speed: String? = null

    @Field val orientation: String? = null

    @Field val autoCutter: CutterRecord? = null

    fun toDomain() =
        PrinterConfig(
            marginLeft = marginLeft,
            marginTop = marginTop,
            density = Density.entries.firstOrNull { it.value == density } ?: Density.MEDIUM,
            speed = Speed.entries.firstOrNull { it.value == speed },
            orientation = Orientation.entries.firstOrNull { it.value == orientation } ?: Orientation.TOP_TO_BOTTOM,
            autoCutter = autoCutter?.enabled ?: false,
            fullCut = autoCutter?.fullCut ?: true,
        )
}

fun PrinterConfig.toMap(): Map<String, Any?> =
    mapOf(
        "marginLeft" to marginLeft,
        "marginTop" to marginTop,
        "density" to density.value,
        "speed" to speed?.value,
        "orientation" to orientation.value,
        "autoCutter" to mapOf("enabled" to autoCutter, "fullCut" to fullCut),
    )
