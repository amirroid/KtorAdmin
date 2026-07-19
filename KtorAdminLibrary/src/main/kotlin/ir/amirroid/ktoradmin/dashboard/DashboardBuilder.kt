package ir.amirroid.ktoradmin.dashboard

/**
 * DSL builder for creating dashboards inline without subclassing [KtorAdminDashboard].
 *
 * Provides mutable properties for metadata and a [configureLayout] method
 * for defining the grid layout, mirroring the [KtorAdminDashboard] API.
 *
 * ## Usage:
 * ```kotlin
 * install(KtorAdmin) {
 *     dashboard {
 *         page("overview") {
 *             title = "Overview"
 *             icon = "/static/images/dashboard.svg"
 *             groupName = "Analytics"
 *
 *             configureLayout {
 *                 addSection(section = MyChartSection(), height = "300px")
 *                 media(maxWidth = "600px", template = listOf(1))
 *             }
 *         }
 *     }
 * }
 * ```
 */
class DashboardBuilder internal constructor(
    override val path: String,
) : KtorAdminDashboard() {
    override var title: String = path.split("/").last().replaceFirstChar { it.uppercaseChar() }
    override var icon: String? = null
    override var groupName: String? = null
    override var order: Int = 0
    override var visible: Boolean = true
    override var isPrimary: Boolean = false

    override fun KtorAdminDashboard.configure() {
        // No-op: layout is configured via configureLayout { } after construction.
    }
}

/**
 * Registry for managing dashboard instances within the [dashboard][ir.amirroid.ktoradmin.configuration.KtorAdminConfiguration.dashboard] DSL.
 *
 * Provides two ways to add dashboards:
 * - [register] for pre-built [KtorAdminDashboard] instances (class-based approach).
 * - [page] for inline DSL dashboard creation.
 *
 * ## Usage:
 * ```kotlin
 * install(KtorAdmin) {
 *     dashboard {
 *         // Class-based
 *         register(MyDashboard())
 *
 *         // Inline DSL
 *         page("analytics") {
 *             title = "Analytics"
 *             groupName = "Insights"
 *             configureLayout {
 *                 addSection(section = RevenueChart(), height = "400px")
 *             }
 *         }
 *     }
 * }
 * ```
 */
class DashboardRegistry internal constructor() {
    internal val dashboards = mutableListOf<KtorAdminDashboard>()

    /**
     * Registers a pre-built [KtorAdminDashboard] instance.
     *
     * The dashboard's metadata ([KtorAdminDashboard.title], [KtorAdminDashboard.icon], etc.)
     * should be set on the instance before registration.
     *
     * @param dashboard The dashboard instance to register.
     */
    fun register(dashboard: KtorAdminDashboard) {
        dashboards.add(dashboard)
    }

    /**
     * Creates and registers an inline dashboard using the DSL.
     *
     * @param path URL path segment for this dashboard's page. Must be unique.
     * @param configure DSL builder block for configuring the dashboard's metadata and layout.
     * @return The created [DashboardBuilder] instance.
     */
    fun page(
        path: String,
        configure: DashboardBuilder.() -> Unit = {},
    ): DashboardBuilder = DashboardBuilder(path).apply(configure).also(dashboards::add)
}
