package ir.amirroid.ktoradmin.models.response

import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository

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