package models

import utils.AdminTable

internal class TableGroup(
    val group: String,
    val tables: List<AdminTable>
)

internal fun List<AdminTable>.toTableGroups() = groupBy { it.getGroupName() }.map { (groupName, tables) ->
    TableGroup(
        group = groupName ?: "Default",
        tables = tables
    )
}