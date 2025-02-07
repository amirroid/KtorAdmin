package models.chart

import dashboard.ChartDashboardSection

internal class ChartData(
    val labels: List<String>,
    val values: List<ChartLabelsWithValues>,
    val section: ChartDashboardSection,
)

internal class ChartLabelsWithValues(
    val displayName: String,
    val values: List<Double>,
    val fillColors: List<String?> = emptyList(),
    val borderColors: List<String?> = emptyList(),
)