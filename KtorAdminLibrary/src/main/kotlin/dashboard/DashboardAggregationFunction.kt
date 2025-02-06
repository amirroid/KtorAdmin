package dashboard

enum class DashboardAggregationFunction {
    ALL,
    SUM,
    COUNT,
    AVERAGE,
}

internal fun getFieldNameBasedOnAggregationFunction(
    aggregationFunction: DashboardAggregationFunction,
    field: String
) = when (aggregationFunction) {
    DashboardAggregationFunction.COUNT -> "${field}_count"
    DashboardAggregationFunction.SUM -> "${field}_sum"
    DashboardAggregationFunction.AVERAGE -> "${field}_avg"
    else -> field
}

internal fun getFieldFunctionBasedOnAggregationFunction(
    aggregationFunction: DashboardAggregationFunction,
    field: String
) = when (aggregationFunction) {
    DashboardAggregationFunction.COUNT -> "COUNT($field) AS ${field}_count"
    DashboardAggregationFunction.SUM -> "SUM($field) AS ${field}_sum"
    DashboardAggregationFunction.AVERAGE -> "AVG($field) AS ${field}_avg"
    else -> field
}