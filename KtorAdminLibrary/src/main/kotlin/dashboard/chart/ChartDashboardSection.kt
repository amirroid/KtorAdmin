package dashboard.chart

import models.chart.ChartDashboardAggregationFunction
import dashboard.base.DashboardSection
import models.chart.AdminChartStyle
import models.chart.ChartField

/**
 * Base class for configuring a chart in the dashboard section.
 *
 * This class defines the structure for configuring a chart, including its data source, visualization style,
 * and additional customization options.
 *
 * ### **Required Properties:**
 * - **`aggregationFunction`** → Determines how data is aggregated (e.g., SUM, AVERAGE).
 * - **`tableName`** → The name of the table from which data is fetched.
 * - **`labelField`** → The field used as labels on the X-axis.
 * - **`valuesFields`** → A list of fields representing values plotted on the Y-axis.
 * - **`chartStyle`** → The visual representation style of the chart (e.g., LINE, BAR, PIE).
 *
 * ### **Optional Properties (with Defaults):**
 * - **`limitCount`** → Maximum number of data points to display (default: `null`).
 * - **`orderQuery`** → SQL-style sorting condition (default: `null`).
 * - **`tension`** → Defines line smoothness (range: `0.0f` to `1.0f`, default: `0.5f`).
 * - **`borderWidth`** → Thickness of chart borders (default: `1f`).
 * - **`borderRadius`** → Corner radius for border styling (default: `0f`).
 *
 * ### **Customization Methods:**
 * - `provideBorderColor(label, valueField)`: Returns a custom border color based on the label and value field.
 * - `provideFillColor(label, valueField)`: Returns a custom fill color based on the label and value field.
 *
 * ### **Example Usage:**
 * ```kotlin
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

    /** Defines how the chart data is aggregated (e.g., SUM, AVERAGE). */
    abstract val aggregationFunction: ChartDashboardAggregationFunction

    /** The database table that provides the chart's data. */
    abstract val tableName: String

    /** The field used for labeling data points on the X-axis. */
    abstract val labelField: String

    /** The fields representing data values plotted on the Y-axis. */
    abstract val valuesFields: List<ChartField>

    /** Determines the chart's visual style (e.g., LINE, BAR, PIE). */
    abstract val chartStyle: AdminChartStyle

    /**
     * Returns a custom border color for a given label and value field.
     * @param label The label associated with the data point.
     * @param valueField The field representing the data value.
     * @return The border color as a string (e.g., hex code or predefined color name).
     */
    abstract fun provideBorderColor(label: String, valueField: String): String?

    /**
     * Returns a custom fill color for a given label and value field.
     * @param label The label associated with the data point.
     * @param valueField The field representing the data value.
     * @return The fill color as a string (e.g., hex code or predefined color name).
     */
    abstract fun provideFillColor(label: String, valueField: String): String?

    /** Maximum number of data points to display (null for unlimited). */
    open val limitCount: Int? = null

    /** Sorting condition for data retrieval (e.g., `"date DESC"`). */
    open val orderQuery: String? = null

    /** Defines line smoothness in line charts (range: `0.0f` to `1.0f`, default: `0.5f`). */
    open val tension: Float = 0.5f

    /** The thickness of chart borders (default: `1f`). */
    open val borderWidth: Float = 1f

    /** The corner radius for border styling (default: `0f`). */
    open val borderRadius: Float = 0f

    /**
     * Defines the format for displaying the tooltip in the chart.
     *
     * @property tooltipFormat A template string used for formatting the tooltip.
     *                          `{field}` is replaced with the display name of the field,
     *                          and `{value}` is replaced with the corresponding value for that field.
     */
    open val tooltipFormat: String = "{field}: {value}"

    companion object {
        /** Constant representing the section type as `"chart"`. */
        private const val SECTION_TYPE = "chart"
    }
}