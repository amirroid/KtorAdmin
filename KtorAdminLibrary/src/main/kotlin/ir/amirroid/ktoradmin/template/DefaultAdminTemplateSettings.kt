package ir.amirroid.ktoradmin.template

import kotlin.String

/**
 * Configuration for the default admin template.
 *
 * Controls colors, typography, shapes, spacing, sidebar behavior, header style,
 * and other visual aspects of the default template.
 *
 * Every property has a sensible default so users only need to override what they want.
 */
data class DefaultAdminTemplateSettings(
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
        val primaryColor: String = "#292D32",
        val secondaryColor: String = "#9A6C00",
        val backgroundGradientStart: String = "#F3E7CB",
        val backgroundGradientEnd: String = "#E3E5E6",
        val highlightColor: String = "#ffb300",
        val errorColor: String = "red",
        val evenRowColor: String = "rgba(243, 231, 203, 0.3)",
        val oddRowColor: String = "transparent",
        val hoverRowColor: String = "rgba(154, 108, 0, 0.2)",
    )

    data class Typography(
        val fontFamily: String = "'Istok Web', sans-serif",
        val fontScale: Double = 1.0,
    )

    data class Shapes(
        val sidebarBorderRadius: String = "24px",
        val menuItemBorderRadius: String = "16px",
        val dropdownBorderRadius: String = "12px",
    )

    data class Spacing(
        val bodyPadding: String = "16px",
        val sidebarMargin: String = "16px",
        val sidebarWidth: String = "300px",
    )

    data class SidebarStyle(
        val backdropBlur: String = "8px",
        val backgroundOpacity: Double = 0.7,
    )

    data class HeaderStyle(
        val content: HeaderContent = HeaderContent.Text(),
        val height: String = "54px",
    )

    sealed interface HeaderContent {
        data class Text(
            val prefix: String = "Ktor",
            val text: String = "Admin",
        ) : HeaderContent

        data class Image(
            val url: String,
            val altText: String = "",
            val height: String = "32px",
        ) : HeaderContent
    }

    data class AnimationStyle(
        val enabled: Boolean = true,
        val transitionDuration: String = "0.3s",
        val transitionTiming: String = "cubic-bezier(0.4, 0, 0.2, 1)",
    )

    companion object {
        val DefaultDarkModeColors =
            Colors(
                primaryColor = "#E1E1E1",
                secondaryColor = "#FFB84D",
                backgroundGradientStart = "#1A1D21",
                backgroundGradientEnd = "#2A2D32",
                highlightColor = "#FFD700",
                errorColor = "#FF453A",
                evenRowColor = "rgba(255, 184, 77, 0.15)",
                oddRowColor = "transparent",
                hoverRowColor = "rgba(255, 184, 77, 0.2)",
            )
    }
}
