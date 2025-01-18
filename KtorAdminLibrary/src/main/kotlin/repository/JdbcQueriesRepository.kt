package repository

import com.vladsch.kotlin.jdbc.*
import models.DataWithPrimaryKey
import utils.AdminTable
import utils.getAllAllowToShowColumns

internal object JdbcQueriesRepository {
    private fun <T> AdminTable.usingDataSource(lambda: (Session) -> T): T {
        val dataSource = if (getGroupName() == null) HikariCP.dataSource() else HikariCP.dataSource()
        return using(session(dataSource), lambda)
    }

    fun getAllData(table: AdminTable): List<DataWithPrimaryKey> {
        return table.usingDataSource { session ->
            session.list(
                sqlQuery(table.createGetAllQuery())
            ) { raw ->
                DataWithPrimaryKey(
                    primaryKey = raw.any(table.getPrimaryKey()).toString(),
                    data = table.getAllAllowToShowColumns().map { raw.any(it.columnName).toString() }
                )
            }
        }
    }

    private fun AdminTable.getPrimaryKeyColumn() = getAllColumns().first { it.columnName == getPrimaryKey() }

    private fun AdminTable.createGetAllQuery() = buildString {
        append("SELECT ")
        append(
            getAllAllowToShowColumns()
                .plus(getPrimaryKeyColumn())
                .distinctBy { it.columnName }
                .joinToString(separator = ", ", prefix = "", postfix = "") { it.columnName })
        append(" FROM ")
        append(getTableName())
    }

    fun getData(table: AdminTable, primaryKey: String): List<String>? {
        return table.usingDataSource { session ->
            session.first(
                sqlQuery(table.createGetOneItemQuery(primaryKey = primaryKey))
            ) { raw ->
                table.getAllAllowToShowColumns().map { raw.any(it.columnName).toString() }
            }
        }
    }

    private fun AdminTable.createGetOneItemQuery(primaryKey: String) = buildString {
        append("SELECT ")
        append(getAllAllowToShowColumns().joinToString(separator = ", ", prefix = "", postfix = "") { it.columnName })
        append(" FROM ")
        append(getTableName())
        append(" WHERE ")
        append(getPrimaryKey())
        append(" = ")
        append(primaryKey)
    }

    fun insertData(table: AdminTable, parameters: List<String>) {
        println(table.createInsertQuery(parameters))
        table.usingDataSource { session ->
            session.transaction { tx ->
                tx.execute(
                    sqlQuery(table.createInsertQuery(parameters))
                )
            }
        }
    }

    private fun AdminTable.createInsertQuery(parameters: List<String>) = buildString {
        append("INSERT INTO ")
        append(getTableName())
        append(
            getAllAllowToShowColumns().joinToString(
                separator = ", ",
                prefix = " (",
                postfix = ") "
            ) { it.columnName })
        append("VALUES ")
        append(parameters.joinToString(separator = ", ", prefix = "(", postfix = ")") { it.addQuotationIfIsString() })
    }


    fun updateData(table: AdminTable, parameters: List<String>, primaryKey: String) {
        println(table.createInsertQuery(parameters))
        table.usingDataSource { session ->
            session.transaction { tx ->
                tx.update(
                    sqlQuery(table.createUpdateQuery(parameters, primaryKey))
                )
            }
        }
    }

    private fun AdminTable.createUpdateQuery(parameters: List<String>, primaryKey: String) = buildString {
        append("UPDATE ")
        append(getTableName())
        append(" SET ")
        val columnsWithValues =
            getAllAllowToShowColumns().mapIndexed { index, column -> column.columnName to parameters[index].addQuotationIfIsString() }
        append(
            columnsWithValues.joinToString(
                separator = ", ",
                prefix = " ",
                postfix = ""
            ) { (columnName, value) -> "$columnName = $value" })
        append(" WHERE ")
        append(getPrimaryKey())
        append(" = ")
        append(primaryKey)
    }

    private fun String.addQuotationIfIsString() = if (toDoubleOrNull() != null) this else "'$this'"
}