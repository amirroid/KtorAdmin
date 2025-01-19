package repository

import com.vladsch.kotlin.jdbc.*
import models.ColumnSet
import models.DataWithPrimaryKey
import models.ReferenceItem
import utils.AdminTable
import utils.extractTextInCurlyBraces
import utils.getAllAllowToShowColumns
import utils.populateTemplate

internal object JdbcQueriesRepository {

    private fun <T> AdminTable.usingDataSource(lambda: (Session) -> T): T {
        val dataSource = HikariCP.dataSource()
        return using(session(dataSource), lambda)
    }

    fun getAllData(table: AdminTable): List<DataWithPrimaryKey> =
        table.usingDataSource { session ->
            session.list(sqlQuery(table.createGetAllQuery())) { raw ->
                DataWithPrimaryKey(
                    primaryKey = raw.any(table.getPrimaryKey()).toString(),
                    data = table.getAllAllowToShowColumns().map { raw.anyOrNull(it.columnName).toString() }
                )
            }
        }

    fun getAllReferences(table: AdminTable, referenceColumn: String): List<ReferenceItem> =
        table.usingDataSource { session ->
            session.list(sqlQuery(table.createGetAllReferencesQuery(referenceColumn))) { raw ->
                val referenceKey = raw.any(referenceColumn).toString()
                val displayFormat = table.getDisplayFormat()
                ReferenceItem(
                    referenceKey = referenceKey,
                    item = displayFormat?.let {
                        val displayFormatValues = it.extractTextInCurlyBraces()
                        populateTemplate(it, displayFormatValues.associateWith { raw.anyOrNull(it)?.toString() })
                    } ?: "${table.getTableName().replaceFirstChar { it.uppercaseChar() }} Object ($referenceKey)"
                )
            }
        }

    fun getData(table: AdminTable, primaryKey: String): List<String?>? =
        table.usingDataSource { session ->
            session.first(sqlQuery(table.createGetOneItemQuery(primaryKey))) { raw ->
                table.getAllAllowToShowColumns().map { raw.anyOrNull(it.columnName)?.toString() }
            }
        }

    fun insertData(table: AdminTable, parameters: List<String?>) {
        table.usingDataSource { session ->
            session.transaction { tx ->
                tx.execute(sqlQuery(table.createInsertQuery(parameters)))
            }
        }
    }

    fun updateChangedData(table: AdminTable, parameters: List<String?>, primaryKey: String) {
        val initialData = getData(table, primaryKey)
        if (initialData == null) {
            insertData(table, parameters)
        } else {
            val columns = table.getAllAllowToShowColumns()
            val changedData = parameters.mapIndexed { index, item ->
                columns[index] to item
            }.filterIndexed { index, item ->
                val initialValue = initialData.getOrNull(index)
                initialValue != item.second && !(initialValue != null && item.second == null)
            }

            if (changedData.isNotEmpty()) {
                table.usingDataSource { session ->
                    session.transaction { tx ->
                        tx.update(
                            sqlQuery(
                                table.createUpdateQuery(
                                    changedData.map { it.first },
                                    changedData.map { it.second },
                                    primaryKey
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun AdminTable.createGetAllQuery() = buildString {
        append("SELECT ")
        append(getAllAllowToShowColumns().plus(getPrimaryKeyColumn()).distinctBy { it.columnName }
            .joinToString(", ") { it.columnName })
        append(" FROM ")
        append(getTableName())
    }

    private fun AdminTable.createGetAllReferencesQuery(referenceColumn: String) = buildString {
        append("SELECT ")
        val columns = listOf(referenceColumn) + getDisplayFormat()?.extractTextInCurlyBraces().orEmpty()
        append(columns.distinct().joinToString(", "))
        append(" FROM ")
        append(getTableName())
    }

    private fun AdminTable.createGetOneItemQuery(primaryKey: String) = buildString {
        append("SELECT ")
        append(getAllAllowToShowColumns().joinToString(", ") { it.columnName })
        append(" FROM ")
        append(getTableName())
        append(" WHERE ")
        append(getPrimaryKey())
        append(" = ")
        append(primaryKey)
    }

    private fun AdminTable.createInsertQuery(parameters: List<String?>) = buildString {
        append("INSERT INTO ")
        append(getTableName())
        append(" (")
        append(getAllAllowToShowColumns().joinToString(", ") { it.columnName })
        append(") VALUES (")
        append(parameters.joinToString(", ") { it?.addQuotationIfIsString() ?: "NULL" })
        append(")")
    }

    private fun AdminTable.createUpdateQuery(
        updatedColumns: List<ColumnSet>,
        parameters: List<String?>,
        primaryKey: String
    ) = buildString {
        append("UPDATE ")
        append(getTableName())
        append(" SET ")
        val columnsWithValues = updatedColumns.mapIndexed { index, column ->
            column.columnName to parameters[index]?.addQuotationIfIsString()
        }
        append(columnsWithValues.joinToString(", ") { (columnName, value) -> "$columnName = $value" })
        append(" WHERE ")
        append(getPrimaryKey())
        append(" = ")
        append(primaryKey)
    }

    private fun String.addQuotationIfIsString(): String =
        if (toDoubleOrNull() != null) this else "'$this'"

    private fun AdminTable.getPrimaryKeyColumn() = getAllColumns().first { it.columnName == getPrimaryKey() }
}
