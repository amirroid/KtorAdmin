package models.chart

internal class ChartData(
    val values: List<ChartLabelsWithValues>,
    val config: ChartConfig
)

internal class ChartLabelsWithValues(
    val label: String,
    val values: List<List<Double>>,
)