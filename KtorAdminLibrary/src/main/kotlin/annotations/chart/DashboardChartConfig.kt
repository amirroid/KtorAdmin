package annotations.chart

import models.chart.AdminChartStyle

/**
 * Annotation for configuring a chart in the admin dashboard.
 *
 * This annotation allows defining how a chart should be rendered by specifying its label field,
 * data fields, chart style, colors, and optional configurations like data limits and sorting.
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
 *     tensions = .6f,
 *     borderWidth = 2f
 * )
 * ```
 * This example defines a **line chart** displaying **sales** and **profit** by **category**.
 * The chart will have **blue and orange borders**, **green and gold fill colors**, and be limited
 * to **10 data points**, sorted by **date in descending order**.
 *
 * @param sectionName The section where the chart is displayed in the dashboard.
 * @param labelField The field used as the labels in the chart (e.g., X-axis labels).
 * @param valuesFields The fields representing data values to be plotted (e.g., Y-axis values).
 * @param chartStyle The visual style of the chart (e.g., line, bar, pie).
 * @param fillColors (Optional) The fill colors for the chart. Defaults to an empty array.
 * @param borderColors (Optional) The border colors for the chart. Defaults to an empty array.
 * @param limitCount (Optional) The maximum number of data points to display. Defaults to `Int.MAX_VALUE`.
 * @param orderQuery (Optional) The sorting query for data (e.g., `"date DESC"`). Defaults to an empty string.
 * @param tension (Optional) A value between 0 and 1 to control the smoothness of line charts. A higher value results in a smoother curve.
 * @param borderWidth (Optional) The width of the chart's borders. If not specified, it defaults to 1f.
 *
 * **Notes:**
 * - `labelField` and `valuesFields` should match column names in a SQL table or fields in a collection.
 * - If multiple colors are provided for `fillColors` and `borderColors`, they will be applied sequentially.
 * - `limitCount` is useful for performance optimization when handling large datasets.
 * - `orderQuery` helps in sorting data before rendering the chart.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DashboardChartConfig(
    val sectionName: String,
    val labelField: String,
    val valuesFields: Array<String>,
    val chartStyle: AdminChartStyle,
    val fillColors: Array<String> = [],
    val borderColors: Array<String> = [],
    val limitCount: Int = Int.MAX_VALUE,
    val orderQuery: String = "",
    val tension: Float = 0f,
    val borderWidth: Float = 1f,
)