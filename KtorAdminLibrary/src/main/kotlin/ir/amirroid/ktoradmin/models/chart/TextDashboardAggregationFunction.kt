package ir.amirroid.ktoradmin.models.chart

/**
 * Defines aggregation functions for text-based dashboard sections.
 *
 * These functions determine how text data is processed and displayed in the dashboard.
 *
 * **Usage Notes:**
 * - For **`SUM`**, **`COUNT`**, **`AVERAGE`**, and **`PROFIT_PERCENTAGE`**, the field **must be numeric**.
 * - **`LAST_ITEM`** is the only function that supports non-numeric fields.
 */
enum class TextDashboardAggregationFunction {

    /** Computes the sum of numerical values in the specified field. */
    SUM,

    /** Counts the number of occurrences in the specified field. */
    COUNT,

    /** Calculates the average of numerical values in the specified field. */
    AVERAGE,

    /** Computes the profit percentage based on relevant numeric data fields. */
    PROFIT_PERCENTAGE,

    /** Retrieves the last entered item in the specified field (supports non-numeric values). */
    LAST_ITEM
}
