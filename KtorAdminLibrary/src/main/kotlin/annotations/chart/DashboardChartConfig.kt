package annotations.chart

import models.chart.AdminChartStyle

/**
 * Annotation for configuring a chart in the admin dashboard.
 *
 * This annotation defines how a chart should be rendered, including its label, data fields, chart style, colors,
 * and optional configurations such as data limits, sorting, and border settings.
 *
 * Example usage:
 * ```
 * @DashboardChartConfig(
 *     sectionName = "Sales Overview",
 *     labelField = "category",
 *     valuesFields = ["sales", "profit"],
 *     chartStyle = AdminChartStyle.LINE,
 *     borderColors = ["#0000FF", "#FF5733"], // Blue & Orange
 *     fillColors = ["#00FF00", "#FFD700"],   // Green & Gold
 *     limitCount = 10,
 *     orderQuery = "date DESC",
 *     aggregationFunction = "SUM",
 *     tension = .6f,
 *     borderWidth = 2f
 * )
 * ```
 * This example creates a **line chart** for **sales** and **profit** by **category**, with **blue and orange borders**,
 * **green and gold fill colors**, limited to **10 data points**, and sorted by **date in descending order**.
 * The **aggregation function** is set to **SUM**, aggregating data points by summing their values.
 *
 * @param sectionName The section name where the chart is displayed.
 * @param labelField The field used as the X-axis labels.
 * @param valuesFields The fields representing the Y-axis data.
 * @param chartStyle The chart type (e.g., line, bar, pie).
 * @param fillColors (Optional) The chart's fill colors. Default is an empty array.
 * @param borderColors (Optional) The chart's border colors. Default is an empty array.
 * @param limitCount (Optional) Maximum number of data points. Default is 100.
 * @param orderQuery (Optional) Data sorting query (e.g., `"date DESC"`). Default is empty.
 * @param aggregationFunction (Optional) Aggregation function for data points (e.g., "AVG", "SUM"). Default is empty.
 * @param tension (Optional) Line smoothness for line charts (0.0f to 1.0f). Default is 0.5f.
 * @param borderWidth (Optional) Border width. Default is 1f.
 *
 * **Notes:**
 * - `labelField` and `valuesFields` should correspond to valid column names or field names.
 * - Multiple `fillColors` and `borderColors` will be applied sequentially.
 * - `limitCount` optimizes performance for large datasets.
 * - `orderQuery` allows sorting data before chart rendering.
 * - `aggregationFunction` limits to valid options like "AVG" or "SUM" to prevent invalid inputs.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DashboardChartConfig(
    val sectionName: String,
    val labelField: String,
    val valuesFields: Array<String>,
    val chartStyle: AdminChartStyle,
    val aggregationFunction: String = "",
    val fillColors: Array<String> = [],
    val borderColors: Array<String> = [],
    val limitCount: Int = Int.MAX_VALUE,
    val orderQuery: String = "",
    val tension: Float = 0.5f,
    val borderWidth: Float = 1f,
)
