package models.chart

import dashboard.ChartDashboardSection

internal class ChartData(
    val labels: List<String>,
    val values: List<ChartLabelsWithValues>,
    val config: ChartConfig? = null,
    val section: ChartDashboardSection? = null,
)

internal class ChartLabelsWithValues(
    val values: List<Double>,
    val fillColor: String? = null,
    val borderColor: String? = null,
)