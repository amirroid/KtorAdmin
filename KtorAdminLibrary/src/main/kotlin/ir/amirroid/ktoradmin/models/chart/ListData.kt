package ir.amirroid.ktoradmin.models.chart

import ir.amirroid.ktoradmin.dashboard.list.ListDashboardSection
import ir.amirroid.ktoradmin.models.DataWithPrimaryKey

internal data class ListData(
    val section: ListDashboardSection,
    val values: List<DataWithPrimaryKey>,
    val fields: List<FieldData>,
    val pluralName: String
)

internal data class FieldData(
    val name: String,
    val type: String,
    val fieldName: String
)