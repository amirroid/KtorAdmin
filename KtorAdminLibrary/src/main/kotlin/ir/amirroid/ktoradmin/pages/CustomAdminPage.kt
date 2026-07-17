package ir.amirroid.ktoradmin.pages

import io.ktor.server.application.ApplicationCall

/**
 * Base class for creating reusable, class-based custom admin pages.
 *
 * Subclass this to create standalone pages that live inside the admin shell
 * (navigation bar, sidebar, header). The developer only provides the page
 * content — the surrounding admin interface is handled automatically.
 *
 * Example:
 * ```kotlin
 * class SettingsPage : CustomAdminPage() {
 *     override val path = "settings"
 *     override val title = "Settings"
 *     override val icon = "/static/images/settings.svg"
 *     override val groupName = "Management"
 *     override val order = 1
 *
 *     override suspend fun content(call: ApplicationCall): String {
 *         return "<h2>App Settings</h2><p>Configure your application.</p>"
 *     }
 * }
 *
 * // Registration
 * install(KtorAdmin) {
 *     customPage(SettingsPage())
 * }
 * ```
 *
 * Supports nested paths:
 * ```kotlin
 * class ThemeSettingsPage : CustomAdminPage() {
 *     override val path = "settings/theme"
 *     override val title = "Theme Settings"
 *     // ...
 * }
 * ```
 */
abstract class CustomAdminPage {
    /** URL path segment under resources. Supports nested paths like "settings/theme". */
    abstract val path: String

    /** Display title shown in the sidebar and page header. */
    abstract val title: String

    /** Optional description for documentation/metadata. */
    open val description: String? = null

    /** Optional SVG icon path for sidebar display. */
    open val icon: String? = null

    /** Sidebar group name. Pages are grouped together in the sidebar. */
    open val groupName: String? = null

    /** Ordering position within the group (lower values appear first). */
    open val order: Int = 0

    /** Whether the page is visible in the sidebar. */
    open val visible: Boolean = true

    /** Optional list of required roles/permissions. */
    open val permissions: List<String>? = null

    /**
     * Returns the HTML content for this page.
     *
     * The content is automatically wrapped in the standard admin shell
     * (sidebar, header, toolbar). Only provide the page-specific content here.
     */
    abstract suspend fun content(call: ApplicationCall): String

    /**
     * Renders the page inside the admin shell.
     *
     * Override this to take full control over the entire page rendering,
     * bypassing the default admin shell. Most users should NOT override this.
     */
    open suspend fun render(call: ApplicationCall): String = content(call)
}
