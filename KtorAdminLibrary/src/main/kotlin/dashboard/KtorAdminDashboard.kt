package dashboard

import dashboard.grid.Grid

/**
 * An abstract class for the Ktor admin dashboard, utilizing an internal grid for layout management.
 *
 * This class calls the abstract [configure] method during initialization,
 * which must be implemented by subclasses.
 * Additionally, it provides [configureLayout] to customize the grid layout using a builder function.
 *
 * ## Usage:
 * To use a custom admin dashboard, create a subclass of `KtorAdminDashboard`
 * and provide its implementation during Ktor setup:
 *
 * ```kotlin
 * install(KtorAdmin) {
 *     adminDashboard = CustomDashboard()
 * }
 * ```
 */
abstract class KtorAdminDashboard {
    internal val grid = Grid()

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
