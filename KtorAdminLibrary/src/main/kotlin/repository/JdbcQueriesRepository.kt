package repository

import com.vladsch.kotlin.jdbc.*
import configuration.DynamicConfiguration
import formatters.extractTextInCurlyBraces
import formatters.populateTemplate
import models.ColumnSet
import models.DataWithPrimaryKey
import models.ReferenceItem
import utils.AdminTable
import utils.getAllAllowToShowColumns

internal object JdbcQueriesRepository {
    private const val NULL = "NULL"

    private fun <T> AdminTable.usingDataSource(lambda: (Session) -> T): T {
        val dataSource = HikariCP.dataSource()
        return using(session(dataSource), lambda)
    }

    fun getAllData(table: AdminTable, search: String?, currentPage: Int?): List<DataWithPrimaryKey> =
        table.usingDataSource { session ->
            session.list(sqlQuery(table.createGetAllQuery(search = search, currentPage = currentPage))) { raw ->
                DataWithPrimaryKey(
                    primaryKey = raw.any("${table.getTableName()}_${table.getPrimaryKey()}").toString(),
                    data = table.getAllAllowToShowColumns()
                        .map { raw.anyOrNull("${table.getTableName()}_${it.columnName}").toString() }
                )
            }
        }

    fun getCount(table: AdminTable, search: String?): Int =
        table.usingDataSource { session ->
            session.count(sqlQuery(table.createGetAllQuery(search = search, null)))
        }

    fun getAllReferences(table: AdminTable, referenceColumn: String): List<ReferenceItem> =
        table.usingDataSource { session ->
            session.list(sqlQuery(table.createGetAllReferencesQuery(referenceColumn))) { raw ->
                val referenceKey = raw.any("${table.getTableName()}_$referenceColumn").toString()
                val displayFormat = table.getDisplayFormat()
                ReferenceItem(
                    referenceKey = referenceKey,
                    item = displayFormat?.let {
                        val displayFormatValues = it.extractTextInCurlyBraces()
                        populateTemplate(
                            it,
                            displayFormatValues.associateWith { item ->
                                if (item == referenceColumn) {
                                    referenceKey
                                } else raw.anyOrNull(
                                    item.split(".").joinToString(separator = "_")
                                )?.toString()
                            })
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

    fun insertData(table: AdminTable, parameters: List<String?>): Int? {
        return table.usingDataSource { session ->
            session.transaction { tx ->
                tx.updateGetId(sqlQuery(table.createInsertQuery(parameters)))
            }
        }
    }

    fun updateChangedData(table: AdminTable, parameters: List<String?>, primaryKey: String): Pair<Int, List<String>>? {
        val initialData = getData(table, primaryKey)
        return if (initialData == null) {
            insertData(table, parameters)?.let { id -> id to table.getAllAllowToShowColumns().map { it.columnName } }
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
                        tx.updateGetId(
                            sqlQuery(
                                table.createUpdateQuery(
                                    changedData.map { it.first },
                                    changedData.map { it.second },
                                    primaryKey
                                )
                            )
                        )
                    }
                }?.let { id -> id to changedData.map { it.first.columnName } }
            } else null
        }
    }

    private fun AdminTable.createGetAllQuery(search: String?, currentPage: Int?) = buildString {
        val columns = getAllAllowToShowColumns().plus(getPrimaryKeyColumn()).distinctBy { it.columnName }
        val selectColumns = columns.map { columnSet ->
            "${getTableName()}.${columnSet.columnName} AS ${getTableName()}_${columnSet.columnName}"
        }

        append("SELECT ")
        append(selectColumns.joinToString(", "))
        append(" FROM ")
        append(getTableName())

        if (search != null) {
            append(" ")
            append(createSearchConditions(search))
        }

        currentPage?.let {
            append(createPaginationQuery(it))
        }
    }.also { println(it) }

    private fun AdminTable.createSearchConditions(search: String): String {
        val joinConditions = mutableListOf<String>()
        val searchConditions = getSearchColumns().map { columnPath ->
            val pathParts = columnPath.split('.')
            var currentTable = getTableName()
            val currentColumn = pathParts.last()

            pathParts.firstOrNull()?.let { part ->
                val columnSet = getAllColumns().find { it.columnName == part }
                val nextTable = columnSet?.reference?.tableName
                val currentReferenceColumn = columnSet?.reference?.columnName

                if (nextTable != null && currentReferenceColumn != null) {
                    joinConditions.add("LEFT JOIN $nextTable ON ${currentTable}.${part} = ${nextTable}.${currentReferenceColumn}")
                    currentTable = nextTable
                }
            }

            "LOWER(${currentTable}.${currentColumn}) LIKE LOWER('%$search%')"
        }

        return joinConditions.joinToString(" ") + " WHERE " + searchConditions.joinToString(" OR ") { it }
    }


    private fun createPaginationQuery(currentPage: Int) = buildString {
        append(" LIMIT ${DynamicConfiguration.maxItemsInPage}")
        append(" OFFSET ${DynamicConfiguration.maxItemsInPage * currentPage}")
    }


    private fun AdminTable.createGetAllReferencesQuery(leftReferenceColumn: String): String {
        val columns = getDisplayFormat()?.extractTextInCurlyBraces().orEmpty()
        val selectColumns = mutableSetOf<String>()
        val joins = mutableListOf<String>()

        selectColumns.add("${getTableName()}.$leftReferenceColumn AS ${getTableName()}_$leftReferenceColumn")

        columns.forEach { column ->
            if (column.contains('.')) {
                var currentTable = getTableName()
                var currentColumn = ""
                val path = column.split('.')

                for (i in path.indices) {
                    val referenceColumn = path[i]
                    val nextColumn = path.getOrNull(i + 1)
                    val columnSet = getAllColumns().find { it.columnName == referenceColumn }
                    val reference = columnSet?.reference

                    if (reference != null) {
                        val joinTable = reference.tableName
                        val joinColumn = reference.columnName
                        currentColumn = nextColumn ?: referenceColumn

                        joins.add("LEFT JOIN $joinTable ON $currentTable.$referenceColumn = $joinTable.$joinColumn")
                        currentTable = joinTable
                    } else if (i == path.lastIndex) {
                        currentColumn = referenceColumn
                    }
                }

                if (currentColumn.isNotEmpty()) {
                    selectColumns.add("$currentTable.$currentColumn AS ${path.joinToString("_")}")
                }
            } else {
                if (column != leftReferenceColumn && column != getPrimaryKey()) {
                    selectColumns.add("${getTableName()}.$column")
                }
            }
        }

        return buildString {
            append("SELECT DISTINCT ")
            append(selectColumns.joinToString(", "))
            append(" FROM ")
            append(getTableName())
            joins.forEach { append(" $it") }
        }
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
        val columns = getAllAllowToShowColumns()
        val parametersWithNULL = parameters.mapIndexed { index, parameter ->
            if (columns[index].nullable && parameter.isNullOrEmpty()) NULL else parameter?.addQuotationIfIsString()
                ?: NULL
        }
        append("INSERT INTO ")
        append(getTableName())
        append(" (")
        append(columns.joinToString(", ") { it.columnName })
        append(") VALUES (")
        append(parametersWithNULL.joinToString(", "))
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
            column.columnName to parameters[index]?.let {
                if (it.isEmpty() && column.nullable) NULL else it.addQuotationIfIsString()
            }
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
