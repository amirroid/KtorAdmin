package ir.amirroid.ktoradmin.pages

import io.ktor.server.application.ApplicationCall

/**
 * DSL builder for configuring a [CustomPage].
 *
 * Usage:
 * ```kotlin
 * customPage("settings") {
 *     title = "Settings"
 *     description = "Application settings"
 *     icon = "/static/images/settings.svg"
 *     groupName = "Configuration"
 *     order = 10
 *
 *     render {
 *         "<h1>Settings</h1>"
 *     }
 * }
 * ```
 *
 * Supports nested paths:
 * ```kotlin
 * customPage("settings/theme") {
 *     title = "Theme Settings"
 *     render { "<h1>Theme</h1>" }
 * }
 * ```
 */
class CustomPageBuilder(private val path: String) {
    /** Display title shown in the sidebar and page header. */
    var title: String = path.split("/").last().replaceFirstChar { it.uppercaseChar() }

    /** Optional description for documentation/metadata. */
    var description: String? = null

    /** Optional SVG icon path for sidebar display. */
    var icon: String? = null

    /** Sidebar group name. Pages are grouped together in the sidebar. */
    var groupName: String? = null

    /** Ordering position within the group (lower values appear first). */
    var order: Int = 0

    /** Whether the page is visible in the sidebar. */
    var visible: Boolean = true

    /** Optional list of required roles/permissions. */
    var permissions: List<String>? = null

    private var renderFunction: (suspend (ApplicationCall) -> String)? = null

    /**
     * Sets the render function for this page.
     * The function receives the current [ApplicationCall] and returns HTML content.
     */
    fun render(block: suspend (ApplicationCall) -> String) {
        renderFunction = block
    }

    internal fun build(): CustomPage {
        val renderer = renderFunction
            ?: error("Custom page '$path' must have a render function defined via render { ... }")
        return CustomPage(
            path = path,
            title = title,
            description = description,
            icon = icon,
            groupName = groupName,
            order = order,
            visible = visible,
            permissions = permissions,
            renderContent = renderer,
        )
    }
}
