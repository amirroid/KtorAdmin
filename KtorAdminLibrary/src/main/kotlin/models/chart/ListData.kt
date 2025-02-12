package models.chart

import dashboard.list.ListDashboardSection

internal data class ListData(
    val section: ListDashboardSection,
    val values: List<List<String>>,
    val fields: List<FieldData>
)

internal data class FieldData(
    val name: String,
    val type: String,
)