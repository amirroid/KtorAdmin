package ir.amirroid.ktoradmin.pages

import io.ktor.server.application.ApplicationCall

/**
 * Represents a standalone custom page in the admin interface.
 *
 * Custom pages are not tied to any CRUD resource or database entity.
 * They can render fully custom UI and behavior.
 *
 * @property path URL path segment under resources (supports nested paths like "settings/theme").
 * @property title Display title shown in the sidebar and page header.
 * @property description Optional description for documentation/metadata.
 * @property icon Optional SVG icon path for sidebar display.
 * @property groupName Optional sidebar group name. Pages are grouped together in the sidebar.
 * @property order Ordering position within the group (lower values appear first).
 * @property visible Whether the page is visible in the sidebar.
 * @property permissions Optional list of required roles/permissions.
 * @property renderContent Suspend function that returns the HTML content for this page.
 */
data class CustomPage(
    val path: String,
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val groupName: String? = null,
    val order: Int = 0,
    val visible: Boolean = true,
    val permissions: List<String>? = null,
    val renderContent: suspend (ApplicationCall) -> String,
)
