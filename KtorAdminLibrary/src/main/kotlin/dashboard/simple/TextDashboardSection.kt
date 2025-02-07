package dashboard.simple

import dashboard.base.DashboardSection
import models.chart.TextDashboardAggregationFunction

/**
 * Base class for a text-based dashboard section.
 *
 * This class defines the structure for sections that display textual data,
 * including their data source, field name, and optional sorting.
 *
 * ### **Required Properties:**
 * - **`tableName`** → The database table from which data is retrieved.
 * - **`fieldName`** → The field containing the text data to be displayed.
 * - **`hintText`** → A placeholder or description for the text data.
 * - **`aggregationFunction`** → Determines how text data is processed (e.g., COUNT, AVERAGE, LAST_ITEM).
 *
 * ### **Optional Properties:**
 * - **`orderQuery`** → SQL-style sorting condition for data retrieval (default: `null`).
 *
 * ### **Usage Notes:**
 * - For **`SUM`**, **`COUNT`**, **`AVERAGE`**, and **`PROFIT_PERCENTAGE`**, the field **must be numeric**.
 * - **`LAST_ITEM`** is the only function that supports non-numeric fields.
 *
 * ### **Example Usage:**
 * ```kotlin
 * class TotalUsersSection : TextDashboardSection() {
 *     override val tableName = "users"
 *     override val fieldName = "id" // Numeric field
 *     override val hintText = "Total number of users"
 *     override val aggregationFunction = TextDashboardAggregationFunction.COUNT // Valid
 * }
 *
 * class LatestCommentSection : TextDashboardSection() {
 *     override val tableName = "user_comments"
 *     override val fieldName = "comment_text" // Non-numeric field
 *     override val hintText = "Latest user comment"
 *     override val aggregationFunction = TextDashboardAggregationFunction.LAST_ITEM // Valid
 *     override val orderQuery = "created_at DESC"
 * }
 *
 * // ❌ Invalid example: AVERAGE cannot be applied to a text field
 * // class InvalidSection : TextDashboardSection() {
 * //     override val tableName = "user_comments"
 * //     override val fieldName = "comment_text" // Non-numeric field
 * //     override val hintText = "Invalid usage example"
 * //     override val aggregationFunction = TextDashboardAggregationFunction.AVERAGE // ❌ Not allowed
 * // }
 * ```
 */
abstract class TextDashboardSection : DashboardSection {

    /** Defines the section type as `"text"`. */
    override val sectionType: String
        get() = SECTION_TYPE

    /** The database table that provides the text data. */
    abstract val tableName: String

    /** The specific field in the table that contains the text data. */
    abstract val fieldName: String

    /** A hint or description for the section's content. */
    abstract val hintText: String

    /** Sorting condition for data retrieval (e.g., `"date DESC"`). */
    open val orderQuery: String? = null

    /** Determines how text data is processed (e.g., COUNT, AVERAGE, LAST_ITEM). */
    abstract val aggregationFunction: TextDashboardAggregationFunction

    companion object {
        /** Constant representing the section type as `"text"`. */
        private const val SECTION_TYPE = "text"
    }
}