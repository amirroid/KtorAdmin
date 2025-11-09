package ir.amirroid.ktoradmin.models.chart

import ir.amirroid.ktoradmin.dashboard.chart.ChartDashboardSection

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