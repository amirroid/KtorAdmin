package models.chart

import utils.toSuitableStringForFile

/**
 * Data class to configure chart settings for the admin dashboard.
 *
 * This class is used to specify the settings for rendering a chart, including the data fields,
 * chart style, and other optional properties like colors, limits, and sorting.
 *
 * @param labelField The field name that will be used as labels in the chart (e.g., the X-axis labels).
 * @param valuesFields A list of field names that represent the data values to be plotted in the chart (e.g., the Y-axis values).
 * @param chartStyle The style of the chart (e.g., line, bar, pie, etc.), defined by the `AdminChartStyle` enum.
 * @param fillColors (Optional) A list of colors used to fill the chart area. If not specified, it defaults to an empty list.
 * @param borderColors (Optional) A list of colors for the chart's borders. If not specified, it defaults to an empty list.
 * @param limitCount (Optional) The maximum number of data points to display in the chart. If not specified, it defaults to null (no limit).
 * @param orderQuery (Optional) A query string to define the sorting order of the data (e.g., "name DESC"). If not specified, it defaults to null.
 */
data class ChartConfig(
    val labelField: String,
    val valuesFields: List<String>,
    val chartStyle: AdminChartStyle,
    val fillColors: List<String> = emptyList(),
    val borderColors: List<String> = emptyList(),
    val limitCount: Int? = null,
    val orderQuery: String? = null,
)


internal fun ChartConfig.toSuitableStringForFile() = """
    |ChartConfig(
    |    labelField = "$labelField",
    |    valuesFields = ${valuesFields.toSuitableStringForFile()},
    |    chartStyle = AdminChartStyle.${chartStyle},
    |    fillColors = ${fillColors.toSuitableStringForFile()},
    |    borderColors = ${borderColors.toSuitableStringForFile()},
    |    limitCount = ${limitCount?.toString()},
    |    orderQuery = ${orderQuery?.let { "\"$it\"" }},
    |)
""".trimMargin("|")