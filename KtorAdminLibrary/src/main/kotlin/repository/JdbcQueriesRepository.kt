package repository

import com.vladsch.kotlin.jdbc.*
import utils.AdminTable
import utils.getAllAllowToShowColumns

internal object JdbcQueriesRepository {
    fun getAllData(table: AdminTable): List<List<String>> {
        val dataSource = if (table.getGroupName() == null) HikariCP.dataSource() else HikariCP.dataSource()
        return using(session(dataSource)) { session ->
            session.list(
                sqlQuery(table.createGetAllQuery())
            ) { raw ->
                table.getAllAllowToShowColumns().map { raw.any(it.columnName).toString() }
            }
        }
    }

    private fun AdminTable.createGetAllQuery() = buildString {
        append("SELECT ")
        append(getAllAllowToShowColumns().joinToString(separator = ", ", prefix = "", postfix = "") { it.columnName })
        append(" FROM ")
        append(getTableName())
    }
}