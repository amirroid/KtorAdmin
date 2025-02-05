package models.chart

import utils.toSuitableStringForFile

/**
 * Data class to configure the settings for rendering a chart in the admin dashboard.
 *
 * This class specifies various configurations for how the chart will be displayed, including the data fields to be plotted,
 * the chart style, optional properties like colors, limits on the number of data points, sorting, and additional visual properties.
 *
 * @param sectionName The name of the section or chart for identification.
 * @param labelField The field name that will be used as labels in the chart (e.g., the X-axis labels).
 * @param valuesFields A list of field names that represent the data values to be plotted in the chart (e.g., the Y-axis values).
 * @param chartStyle The style of the chart (e.g., line, bar, pie, etc.), defined by the `AdminChartStyle` enum.
 * @param fillColors (Optional) A list of colors used to fill the chart area. If not specified, it defaults to an empty list.
 * @param borderColors (Optional) A list of colors for the chart's borders. If not specified, it defaults to an empty list.
 * @param limitCount (Optional) The maximum number of data points to display in the chart. If not specified, it defaults to null (no limit).
 * @param orderQuery (Optional) A query string to define the sorting order of the data (e.g., "name DESC"). If not specified, it defaults to null.
 * @param tension (Optional) A value between 0 and 1 to control the smoothness of line charts. A higher value results in a smoother curve.
 * @param borderWidth (Optional) The width of the chart's borders. If not specified, it defaults to 1f.
 */
data class ChartConfig(
    val sectionName: String,
    val labelField: String,
    val valuesFields: List<String>,
    val chartStyle: AdminChartStyle,
    val fillColors: List<String> = emptyList(),
    val borderColors: List<String> = emptyList(),
    val limitCount: Int? = null,
    val orderQuery: String? = null,
    val tension: Float = 0f,
    val borderWidth: Float = 1f,
)


internal fun ChartConfig.toSuitableStringForFile() = """
    |ChartConfig(
    |    sectionName = "$sectionName",
    |    labelField = "$labelField",
    |    valuesFields = ${valuesFields.toSuitableStringForFile()},
    |    chartStyle = AdminChartStyle.${chartStyle},
    |    fillColors = ${fillColors.toSuitableStringForFile()},
    |    borderColors = ${borderColors.toSuitableStringForFile()},
    |    limitCount = ${limitCount?.toString()},
    |    orderQuery = ${orderQuery?.let { "\"$it\"" }},
    |    tension = ${tension}f,
    |    borderWidth = ${borderWidth}f,
    |)
""".trimMargin("|")