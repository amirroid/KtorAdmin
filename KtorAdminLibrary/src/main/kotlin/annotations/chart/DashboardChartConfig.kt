package annotations.chart

import models.chart.AdminChartStyle

/**
 * Annotation to configure a chart for a dashboard.
 *
 * This annotation provides the configuration for rendering a chart, including the label field,
 * value fields, chart style, optional border and fill colors, as well as additional settings
 * like the maximum number of values to display and the query for sorting. This annotation is
 * used to define how the chart should be rendered based on the provided configuration.
 *
 * Example:
 * ```
 * @DashboardChartConfig(
 *     labelField = "category",
 *     valuesFields = ["sales", "profit"],
 *     chartStyle = AdminChartStyle.LINE,
 *     borderColors = ["#0000FF", "#FF5733"],
 *     fillColors = ["#00FF00", "#FFD700"],
 *     limitCount = 10,
 *     orderQuery = "date DESC"
 * )
 * ```
 * This configuration will generate a line chart with categories as labels, sales and profit as values,
 * a border with colors blue and orange, and a fill with green and gold. The chart will be limited to 10 data points,
 * ordered by date in descending order.
 *
 * @param labelField The field that will be used as labels in the chart (corresponds to a column in the SQL DB or a field in a collection).
 * @param valuesFields The fields that will provide the data values for the chart (corresponds to columns in the SQL DB or fields in collections).
 * @param chartStyle The style of the chart (determines how the chart is visually rendered, e.g., line, bar, etc.).
 * @param fillColors (Optional) The fill colors of the chart. If multiple colors are provided, they will be applied in sequence.
 *                   Defaults to an empty list if not specified.
 * @param borderColors (Optional) The border colors of the chart. If multiple colors are provided, they will be applied in sequence.
 *                     Defaults to an empty list if not specified.
 * @param limitCount (Optional) The maximum number of data points to display in the chart. Defaults to `Int.MAX_VALUE` if not specified.
 * @param orderQuery (Optional) The query for sorting the data before displaying (e.g., "date DESC"). Defaults to an empty string if not specified.
 *
 * Note:
 * - Fields in `labelField` and `valuesFields` correspond to column names in a SQL database or field names in a collection.
 * - Both `fillColors` and `borderColors` are optional; if not provided, they default to empty arrays.
 * - `limitCount` is useful for limiting the number of data points shown on the chart, especially for performance optimization.
 * - `orderQuery` allows for defining a sorting order for the data (e.g., by date, name, etc.).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DashboardChartConfig(
    val labelField: String,
    val valuesFields: Array<String>,
    val chartStyle: AdminChartStyle,
    val fillColors: Array<String> = [],
    val borderColors: Array<String> = [],
    val limitCount: Int = Int.MAX_VALUE,
    val orderQuery: String = "",
)