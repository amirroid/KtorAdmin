package models.response

import models.ColumnSet
import models.common.Reference
import panels.AdminJdbcTable
import repository.JdbcQueriesRepository

data class TableResponse(
    val values: List<Pair<String, Any?>?>,
    val referenceValues: Map<ColumnSet, List<String>>
)


internal fun TableResponse.updateSelectedReferences(
    table: AdminJdbcTable,
    tables: List<AdminJdbcTable>,
    primaryKey: String
) {
    referenceValues.forEach { item ->
        val joinTable =
            tables.firstOrNull { (item.key.reference as Reference.ManyToMany).joinTable == it.getTableName() }
                ?: throw IllegalArgumentException("Join table not found for reference: ${(item.key.reference as Reference.ManyToMany).joinTable}")

        JdbcQueriesRepository.updateSelectedReferenceInListReference(
            table = table,
            joinTable = joinTable,
            columnSet = item.key,
            primaryKey = primaryKey,
            newIds = item.value
        )
    }
}