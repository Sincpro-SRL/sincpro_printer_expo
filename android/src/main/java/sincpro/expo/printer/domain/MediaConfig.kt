package sincpro.expo.printer.domain

/**
 * DOMAIN - Media Configuration
 *
 * Configuration for print media (paper/label).
 *
 * DEFAULT: 80mm continuous paper (most common thermal printer configuration)
 *
 * DPI Reference (203 DPI standard):
 * - 1mm = 8 dots
 * - 58mm = 464 dots
 * - 72mm = 576 dots
 * - 80mm = 640 dots (DEFAULT)
 * - 104mm = 832 dots
 */
data class MediaConfig(
    val widthDots: Int,
    val heightDots: Int,
    val mediaType: MediaType,
    val gapDots: Int,
) {
    companion object {
        /**
         * DEFAULT - Continuous paper 80mm (most common)
         * 640 dots = 80mm at 203 DPI
         */
        fun default() = continuous80mm()

        /**
         * Continuous paper 80mm
         * 640 dots = 80mm at 203 DPI
         */
        fun continuous80mm() =
            MediaConfig(
                widthDots = 640,
                heightDots = 0,
                mediaType = MediaType.CONTINUOUS,
                gapDots = 0,
            )

        /**
         * Continuous paper 58mm (compact printers)
         * 464 dots = 58mm at 203 DPI
         */
        fun continuous58mm() =
            MediaConfig(
                widthDots = 464,
                heightDots = 0,
                mediaType = MediaType.CONTINUOUS,
                gapDots = 0,
            )

        /**
         * Continuous paper 104mm (wide format)
         * 832 dots = 104mm at 203 DPI
         */
        fun continuous104mm() =
            MediaConfig(
                widthDots = 832,
                heightDots = 0,
                mediaType = MediaType.CONTINUOUS,
                gapDots = 0,
            )

        /**
         * Label 80mm x 50mm
         */
        fun label80x50mm() =
            MediaConfig(
                widthDots = 640,
                heightDots = 400,
                mediaType = MediaType.LABEL_GAP,
                gapDots = 24,
            )

        /**
         * Label 100mm x 60mm
         */
        fun label100x60mm() =
            MediaConfig(
                widthDots = 800,
                heightDots = 480,
                mediaType = MediaType.LABEL_GAP,
                gapDots = 24,
            )
    }
}

/**
 * Media types
 */
enum class MediaType(
    val sdkValue: Int,
) {
    CONTINUOUS(0),
    LABEL_GAP(1),
    LABEL_BLACK_MARK(2),
}
