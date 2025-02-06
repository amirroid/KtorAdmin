package dashboard

import models.chart.AdminChartStyle

/**
 * Base class for configuring a chart in the dashboard section.
 *
 * This class provides an abstraction for defining the chart's configuration, including the aggregation function,
 * table name, label and data fields, chart style, optional colors, and additional chart settings like data limits,
 * sorting, and border customization.
 *
 * The following properties are required:
 * - `aggregationFunction`: The aggregation function for the chart (e.g., AVERAGE, SUM).
 * - `tableName`: The name of the table that contains the data.
 * - `labelField`: The field to be used as the X-axis labels.
 * - `valuesFields`: A list of fields that will represent the data values on the Y-axis.
 *
 * The following properties are optional and have default values:
 * - `limitCount`: The maximum number of data points to display. Defaults to null.
 * - `orderQuery`: The sorting query for data (e.g., `"date DESC"`). Defaults to null.
 * - `tension`: The smoothness of line charts, ranging from `0.0f` (no smoothness) to `1.0f` (maximum smoothness). Defaults to `0.5f`.
 * - `borderWidth`: The width of the chart's borders. Defaults to `1f`.
 *
 * Example usage:
 * ```
 * class SalesChart : ChartDashboardSection() {
 *     override val aggregationFunction = DashboardAggregationFunction.SUM
 *     override val tableName = "sales_data"
 *     override val labelField = "category"
 *     override val valuesFields = listOf("sales", "profit")
 *     override val chartStyle = AdminChartStyle.LINE
 *     override val limitCount = 10
 *     override val orderQuery = "date DESC"
 *     override val tension = 0.6f
 *     override val borderWidth = 2f
 * }
 * ```
 */
abstract class ChartDashboardSection : DashboardSection {
    override val sectionType: String
        get() = SECTION_TYPE

    // Aggregation function for the chart (e.g., SUM, AVG)
    abstract val aggregationFunction: DashboardAggregationFunction

    // The table from which the chart's data will be fetched
    abstract val tableName: String

    // The field used as the X-axis labels
    abstract val labelField: String

    // A list of fields representing the Y-axis data
    abstract val valuesFields: List<String>

    // Method for providing a custom border color for a given label and value field
    abstract fun provideBorderColor(label: String, valueField: String): String?

    // Method for providing a custom fill color for a given label and value field
    abstract fun provideFillColor(label: String, valueField: String): String?

    // The visual style of the chart (e.g., line, bar, pie)
    abstract val chartStyle: AdminChartStyle

    // The maximum number of data points to be displayed. Defaults to null.
    open val limitCount: Int? = null

    // Sorting query for data. Defaults to null.
    open val orderQuery: String? = null

    // The smoothness of line charts. Ranges from 0.0f to 1.0f. Defaults to 0.5f.
    open val tension: Float = 0.5f

    // The width of the chart's borders. Defaults to 1f.
    open val borderWidth: Float = 1f

    companion object {
        // Constant for the chart section type
        private const val SECTION_TYPE = "chart"
    }
}