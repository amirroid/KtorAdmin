package dashboard.list

import dashboard.base.DashboardSection

/**
 * Base class for a list-based dashboard section.
 *
 * This class provides the foundation for sections that display tabular data,
 * including their data source, selectable fields, and optional constraints
 * such as ordering and row limits.
 *
 * ### **Required Properties:**
 * - **`tableName`** → The database table from which data is retrieved.
 *
 * ### **Optional Properties:**
 * - **`fields`** → A list of column names to include in the result (default: `null`, meaning all allowed columns).
 * - **`limitCount`** → The maximum number of rows to fetch (default: `null`, meaning no limit).
 * - **`orderQuery`** → SQL-style sorting condition for data retrieval (default: `null`).
 *
 * ### **Usage Notes:**
 * - If **`fields`** is `null`, all allowed columns of the table will be retrieved.
 * - The **`orderQuery`** should be a valid SQL `ORDER BY` clause (e.g., `"created_at DESC"`).
 *
 * ### **Example Usage:**
 * ```kotlin
 * class RecentOrdersSection : ListDashboardSection() {
 *     override val tableName = "orders"
 *     override val fields = listOf("id", "customer_name", "order_date", "total_price")
 *     override val orderQuery = "order_date DESC"
 *     override val limitCount = 10
 * }
 *
 * class ActiveUsersSection : ListDashboardSection() {
 *     override val tableName = "users"
 *     override val fields = listOf("id", "username", "last_login")
 *     override val orderQuery = "last_login DESC"
 * }
 * ```
 */
abstract class ListDashboardSection : DashboardSection {

    /** Defines the section type as `"list"`. */
    override val sectionType: String
        get() = SECTION_TYPE

    /** The database table that provides the list data. */
    abstract val tableName: String

    /** The specific columns to be included in the result (null means all allowed columns). */
    open val fields: List<String>? = null

    /** The maximum number of rows to retrieve (null means unlimited). */
    open val limitCount: Int? = null

    /** SQL sorting condition for ordering results (null means no specific ordering). */
    open val orderQuery: String? = null

    companion object {
        /** Constant representing the section type as `"list"`. */
        private const val SECTION_TYPE = "list"
    }
}