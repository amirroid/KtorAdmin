package ir.amirroid.ktoradmin.template

/**
 * Represents a font family with its weight variants and metadata.
 *
 * Supports local font files (bundled in resources), external stylesheets (e.g., Google Fonts),
 * and custom fallback stacks. The [regular] and [bold] properties accept resource-relative
 * paths (e.g., `"/static/font/MyFont-Regular.ttf"`) that are used for both CSS `@font-face`
 * generation and PDF rendering.
 *
 * Use the companion object factory methods for common patterns:
 * - [FontFamily.Default] — the built-in Istok Web font
 * - [FontFamily.fromGoogleFonts] — load a Google Fonts family via stylesheet
 * - [FontFamily.fromResource] — load a font from local resource files
 * - [FontFamily.fromStylesheet] — load from any external CSS URL
 *
 * @property name The CSS font-family name (e.g., `"Inter"`, `"Istok Web"`).
 * @property regular Resource path for the regular (400) weight font file, or `null` if not bundled.
 * @property bold Resource path for the bold (700) weight font file, or `null` if not bundled.
 * @property stylesheet External stylesheet URL that defines the font (e.g., a Google Fonts link).
 * @property fallbacks Ordered list of CSS fallback fonts used when the primary font is unavailable.
 */
data class FontFamily(
    val name: String = "Istok Web",
    val regular: String? = "/static/font/IstokWeb-Regular.ttf",
    val bold: String? = "/static/font/IstokWeb-Bold.ttf",
    val stylesheet: String? = null,
    val fallbacks: List<String> = listOf("sans-serif"),
) {
    /**
     * The complete CSS `font-family` value, including the primary name and all fallbacks.
     *
     * Example output: `'Inter', sans-serif`
     */
    val cssFamily: String
        get() = "'$name', ${fallbacks.joinToString(", ") { "'$it'" }}"

    /**
     * Whether this font family includes at least one locally bundled font file.
     */
    val hasLocalFonts: Boolean
        get() = regular != null || bold != null

    companion object {
        /**
         * The default Istok Web font family bundled with KtorAdmin.
         */
        val Default =
            FontFamily(
                name = "Istok Web",
                regular = "/static/font/IstokWeb-Regular.ttf",
                bold = "/static/font/IstokWeb-Bold.ttf",
            )

        /**
         * Creates a [FontFamily] backed by a Google Fonts stylesheet.
         *
         * @param name The font name as it appears on Google Fonts (e.g., `"Inter"`).
         * @param weights The font weights to include (default: 400 and 700).
         * @param fallbacks CSS fallback fonts.
         * @return A [FontFamily] with only a [stylesheet] — no local files.
         */
        fun fromGoogleFonts(
            name: String,
            weights: List<Int> = listOf(400, 700),
            fallbacks: List<String> = listOf("sans-serif"),
        ): FontFamily {
            val weightsParam = "wght@${weights.joinToString(";")}"
            return FontFamily(
                name = name,
                regular = null,
                bold = null,
                stylesheet = "https://fonts.googleapis.com/css2?family=${name.replace(" ", "+")}:$weightsParam&display=swap",
                fallbacks = fallbacks,
            )
        }

        /**
         * Creates a [FontFamily] from local resource font files.
         *
         * @param name The CSS font-family name.
         * @param regular Resource path for the regular weight font.
         * @param bold Resource path for the bold weight font, or `null`.
         * @param fallbacks CSS fallback fonts.
         * @return A [FontFamily] with local file paths — no external stylesheet.
         */
        fun fromResource(
            name: String,
            regular: String,
            bold: String? = null,
            fallbacks: List<String> = listOf("sans-serif"),
        ): FontFamily =
            FontFamily(
                name = name,
                regular = regular,
                bold = bold,
                fallbacks = fallbacks,
            )

        /**
         * Creates a [FontFamily] from an external stylesheet URL without local font files.
         *
         * @param name The CSS font-family name.
         * @param stylesheetUrl The full URL of the CSS that defines the font.
         * @param fallbacks CSS fallback fonts.
         * @return A [FontFamily] with only a [stylesheet].
         */
        fun fromStylesheet(
            name: String,
            stylesheetUrl: String,
            fallbacks: List<String> = listOf("sans-serif"),
        ): FontFamily =
            FontFamily(
                name = name,
                regular = null,
                bold = null,
                stylesheet = stylesheetUrl,
                fallbacks = fallbacks,
            )
    }
}
