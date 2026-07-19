package ir.amirroid.ktoradmin.dashboard

/**
 * Internal unified representation of a registered dashboard.
 *
 * Stores the dashboard instance along with its resolved metadata,
 * decoupling the template model from the dashboard class hierarchy.
 */
internal data class DashboardEntry(
    val path: String,
    val title: String,
    val icon: String?,
    val groupName: String?,
    val order: Int,
    val visible: Boolean,
    val isPrimary: Boolean,
    val dashboard: KtorAdminDashboard,
) {
    companion object {
        fun from(dashboard: KtorAdminDashboard): DashboardEntry {
            val resolvedPath =
                dashboard.path
                    ?: dashboard::class.java.simpleName.replaceFirstChar { it.lowercaseChar() }
            return DashboardEntry(
                path = resolvedPath,
                title = dashboard.title,
                icon = dashboard.icon,
                groupName = dashboard.groupName,
                order = dashboard.order,
                visible = dashboard.visible,
                isPrimary = dashboard.isPrimary,
                dashboard = dashboard,
            )
        }
    }
}
