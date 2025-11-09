package ir.amirroid.ktoradmin.models.chart

enum class ChartDashboardAggregationFunction {
    ALL,
    SUM,
    COUNT,
    AVERAGE,
}

internal fun getFieldNameBasedOnAggregationFunction(
    aggregationFunction: ChartDashboardAggregationFunction,
    field: String
) = when (aggregationFunction) {
    ChartDashboardAggregationFunction.COUNT -> "${field}_count"
    ChartDashboardAggregationFunction.SUM -> "${field}_sum"
    ChartDashboardAggregationFunction.AVERAGE -> "${field}_avg"
    else -> field
}

internal fun getFieldFunctionBasedOnAggregationFunction(
    aggregationFunction: ChartDashboardAggregationFunction,
    field: String
) = when (aggregationFunction) {
    ChartDashboardAggregationFunction.COUNT -> "COUNT($field) AS ${field}_count"
    ChartDashboardAggregationFunction.SUM -> "SUM($field) AS ${field}_sum"
    ChartDashboardAggregationFunction.AVERAGE -> "AVG($field) AS ${field}_avg"
    else -> field
}