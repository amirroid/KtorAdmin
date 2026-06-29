package ir.amirroid.ktoradmin.templates.fluent

import ir.amirroid.ktoradmin.template.FontFamily

data class FluentAdminTemplateSettings(
    val colors: Colors = Colors(),
    val darkModeColors: Colors = DefaultDarkModeColors,
    val typography: Typography = Typography(),
    val shapes: Shapes = Shapes(),
    val spacing: Spacing = Spacing(),
    val sidebar: SidebarStyle = SidebarStyle(),
    val header: HeaderStyle = HeaderStyle(),
    val animations: AnimationStyle = AnimationStyle(),
) {
    data class Colors(
        val brandColor: String = "#0078D4",
        val brandHoverColor: String = "#106EBE",
        val brandPressedColor: String = "#005A9E",
        val surfaceColor: String = "#FFFFFF",
        val surfaceHoverColor: String = "#F5F5F5",
        val backgroundLayerColor: String = "#FAFAFA",
        val subtleColor: String = "#616161",
        val defaultBorderColor: String = "#EDEBE9",
        val strongBorderColor: String = "#C8C8C8",
        val errorColor: String = "#D13438",
        val successColor: String = "#107C10",
        val warningColor: String = "#FFB900",
        val infoColor: String = "#0078D4",
    )

    data class Typography(
        val font: FontFamily = FluentFontFamily.SegoeUi,
        val fontScale: Double = 1.0,
    )

    data class Shapes(
        val borderRadiusSmall: String = "4px",
        val borderRadiusMedium: String = "8px",
        val borderRadiusLarge: String = "12px",
    )

    data class Spacing(
        val bodyPadding: String = "0px",
        val contentPadding: String = "24px",
    )

    data class SidebarStyle(
        val width: String = "280px",
    )

    data class HeaderStyle(
        val content: HeaderContent = HeaderContent.Text(),
        val height: String = "48px",
    )

    sealed interface HeaderContent {
        data class Text(
            val prefix: String = "Ktor",
            val text: String = "Admin",
        ) : HeaderContent

        data class Image(
            val url: String,
            val altText: String = "",
            val height: String = "24px",
        ) : HeaderContent
    }

    data class AnimationStyle(
        val enabled: Boolean = true,
        val transitionDuration: String = "0.15s",
        val transitionTiming: String = "cubic-bezier(0.4, 0, 0.2, 1)",
    )

    companion object {
        val DefaultDarkModeColors =
            Colors(
                brandColor = "#60CDFF",
                brandHoverColor = "#C7E0F4",
                brandPressedColor = "#C7E0F4",
                surfaceColor = "#1E1E1E",
                surfaceHoverColor = "#2D2D2D",
                backgroundLayerColor = "#141414",
                subtleColor = "#ADADAD",
                defaultBorderColor = "#404040",
                strongBorderColor = "#505050",
                errorColor = "#F17077",
                successColor = "#6CCB5F",
                warningColor = "#FCE100",
                infoColor = "#60CDFF",
            )
    }
}
