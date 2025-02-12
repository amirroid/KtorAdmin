package models.chart

import dashboard.list.ListDashboardSection
import models.DataWithPrimaryKey

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