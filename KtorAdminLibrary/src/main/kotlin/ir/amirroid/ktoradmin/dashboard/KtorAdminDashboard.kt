package ir.amirroid.ktoradmin.dashboard

import ir.amirroid.ktoradmin.dashboard.grid.Grid

/**
 * An abstract class for the Ktor admin dashboard, utilizing an internal grid for layout management.
 *
 * This class calls the abstract [configure] method during initialization,
 * which must be implemented by subclasses.
 * Additionally, it provides [configureLayout] to customize the grid layout using a builder function.
 *
 * ## Usage:
 * To use a custom admin dashboard, create a subclass of `KtorAdminDashboard`
 * and register it during Ktor setup:
 *
 * ```kotlin
 * class MyDashboard : KtorAdminDashboard() {
 *     override val title = "Overview"
 *     override val icon = "/static/images/dashboard.svg"
 *     override val groupName = "Analytics"
 *
 *     override fun KtorAdminDashboard.configure() {
 *         configureLayout {
 *             addSection(section = MyChartSection())
 *         }
 *     }
 * }
 *
 * install(KtorAdmin) {
 *     dashboard {
 *         register(MyDashboard())
 *     }
 * }
 * ```
 */
abstract class KtorAdminDashboard {
    internal val grid = Grid()

    /** Display title shown in the sidebar and page header. */
    open val title: String = "Dashboard"

    /** Optional SVG icon path for sidebar display. */
    open val icon: String? = null

    /** Sidebar group name. Dashboards are grouped together in the sidebar navigation. */
    open val groupName: String? = null

    /** Ordering position within the group (lower values appear first). */
    open val order: Int = 0

    /** Whether this dashboard is visible in the sidebar. */
    open val visible: Boolean = true

    /**
     * Whether this dashboard is the default landing page at the admin root path.
     *
     * Exactly one dashboard should be marked as primary.
     * If no dashboard has `isPrimary = true`, the admin root shows no dashboard.
     * If multiple dashboards are marked primary, the first registered one wins.
     */
    open val isPrimary: Boolean = false

    /**
     * URL path segment for this dashboard's page.
     * If null, derives from the class simple name (lowercased).
     * Must be unique across all registered dashboards.
     */
    open val path: String? = null

    init {
        configure()
    }

    abstract fun KtorAdminDashboard.configure()

    /**
     * Configures the grid layout using the provided builder function.
     *
     * @param builder A lambda function to modify the grid layout.
     */
    fun configureLayout(builder: Grid.() -> Unit) {
        grid.apply(builder)
    }
}
