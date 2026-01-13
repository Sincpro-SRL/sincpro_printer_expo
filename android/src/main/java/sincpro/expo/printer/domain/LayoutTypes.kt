package sincpro.expo.printer.domain

/**
 * DOMAIN - Shared Layout Types
 *
 * Common enums used across multiple aggregates.
 *
 * Font size options
 */
enum class FontSize(
    val dots: Int,
) {
    SMALL(20),
    MEDIUM(30),
    LARGE(40),
    XLARGE(50),
}

/**
 * Horizontal alignment options
 */
enum class Alignment {
    LEFT,
    CENTER,
    RIGHT,
}
