package getters

import models.ColumnSet
import models.common.DisplayItem
import repository.JdbcQueriesRepository
import panels.AdminJdbcTable

internal fun getReferencesItems(
    tables: List<AdminJdbcTable>,
    columns: List<ColumnSet>
): Map<ColumnSet, List<DisplayItem>> {
    val columnsWithReferences = columns.filter { it.reference != null }
    if (columnsWithReferences.any { column -> tables.none { it.getTableName() == column.reference!!.tableName } }) {
        throw IllegalArgumentException("Error: Some referenced tables do not exist or are not defined in the current schema.")
    }
    return columnsWithReferences.associateWith { column ->
        JdbcQueriesRepository.getAllReferences(
            table = tables.first { it.getTableName() == column.reference!!.tableName },
            referenceColumn = column.reference!!.columnName
        )
    }
}