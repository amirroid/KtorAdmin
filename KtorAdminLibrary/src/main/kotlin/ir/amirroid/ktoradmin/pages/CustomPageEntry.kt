package ir.amirroid.ktoradmin.pages

import io.ktor.server.application.ApplicationCall

/**
 * Internal unified representation of a custom page, regardless of whether it was
 * registered via DSL block or class-based approach.
 *
 * This is used by the sidebar renderer and the page controller to treat both
 * registration styles uniformly.
 */
internal data class CustomPageEntry(
    val path: String,
    val title: String,
    val description: String?,
    val icon: String?,
    val groupName: String?,
    val order: Int,
    val visible: Boolean,
    val permissions: List<String>?,
    val renderer: suspend (ApplicationCall) -> String,
) {
    companion object {
        /** Creates an entry from a DSL-registered [CustomPage]. */
        fun from(page: CustomPage) =
            CustomPageEntry(
                path = page.path,
                title = page.title,
                description = page.description,
                icon = page.icon,
                groupName = page.groupName,
                order = page.order,
                visible = page.visible,
                permissions = page.permissions,
                renderer = page.renderContent,
            )

        /** Creates an entry from a class-based [CustomAdminPage]. */
        fun from(page: CustomAdminPage) =
            CustomPageEntry(
                path = page.path,
                title = page.title,
                description = page.description,
                icon = page.icon,
                groupName = page.groupName,
                order = page.order,
                visible = page.visible,
                permissions = page.permissions,
                renderer = { call -> page.render(call) },
            )
    }
}
