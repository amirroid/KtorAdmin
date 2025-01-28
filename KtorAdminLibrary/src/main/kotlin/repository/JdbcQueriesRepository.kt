package repository

import com.vladsch.kotlin.jdbc.*
import configuration.DynamicConfiguration
import formatters.extractTextInCurlyBraces
import formatters.populateTemplate
import models.ColumnSet
import models.DataWithPrimaryKey
import models.common.DisplayItem
import models.types.ColumnType
import panels.AdminJdbcTable
import panels.getAllAllowToShowColumns
import panels.getAllAllowToShowFieldsInUpsert

internal object JdbcQueriesRepository {
    private const val NULL = "NULL"

    private fun <T> AdminJdbcTable.usingDataSource(lambda: (Session) -> T): T {
        val key = getDatabaseKey()
        val dataSource = if (key == null) HikariCP.dataSource() else HikariCP.dataSource(key)
        return using(session(dataSource), lambda)
    }

    fun getAllData(
        table: AdminJdbcTable,
        search: String?,
        currentPage: Int?,
        filters: MutableList<Pair<ColumnSet, String>>
    ): List<DataWithPrimaryKey> =
        table.usingDataSource { session ->
            session.list(
                sqlQuery(
                    table.createGetAllQuery(
                        search = search,
                        currentPage = currentPage,
                        filters = filters
                    )
                )
            ) { raw ->
                DataWithPrimaryKey(
                    primaryKey = raw.any("${table.getTableName()}_${table.getPrimaryKey()}").toString(),
                    data = table.getAllAllowToShowColumns()
                        .map { raw.anyOrNull("${table.getTableName()}_${it.columnName}").toString() }
                )
            }
        }

    fun getCount(
        table: AdminJdbcTable,
        search: String?,
        filters: List<Pair<ColumnSet, String>>
    ): Int =
        table.usingDataSource { session ->
            session.count(sqlQuery(table.createGetAllQuery(search = search, null, filters)))
        }

    fun getAllReferences(
        table: AdminJdbcTable,
        referenceColumn: String
    ): List<DisplayItem> =
        table.usingDataSource { session ->
            session.list(sqlQuery(table.createGetAllReferencesQuery(referenceColumn))) { raw ->
                val referenceKey = raw.any("${table.getTableName()}_$referenceColumn").toString()
                val displayFormat = table.getDisplayFormat()
                DisplayItem(
                    itemKey = referenceKey,
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

    private fun checkIsChangedData(columnSet: ColumnSet, initialValue: String?, currentValue: String?): Boolean =
        when (columnSet.type) {
            ColumnType.BOOLEAN -> when (currentValue) {
                "on" -> initialValue?.lowercase() !in listOf(
                    "'1'",
                    "1",
                    "true"
                )

                "off" -> initialValue?.lowercase() !in listOf(
                    "'0'",
                    "0",
                    "false"
                )

                else -> initialValue != currentValue
            }

            else -> initialValue != currentValue
        }

    fun getData(table: AdminJdbcTable, primaryKey: String): List<String?>? =
        table.usingDataSource { session ->
            session.first(sqlQuery(table.createGetOneItemQuery(primaryKey))) { raw ->
                table.getAllAllowToShowFieldsInUpsert().map { raw.anyOrNull(it.columnName)?.toString() }
            }
        }

    fun insertData(table: AdminJdbcTable, parameters: List<String?>): Int? {
        return table.usingDataSource { session ->
            session.transaction { tx ->
                tx.updateGetId(sqlQuery(table.createInsertQuery(parameters)))
            }
        }
    }

    fun updateChangedData(
        table: AdminJdbcTable,
        parameters: List<String?>,
        primaryKey: String,
        initialData: List<String?>? = getData(table, primaryKey)
    ): Pair<Int, List<String>>? {
        return if (initialData == null) {
            insertData(table, parameters)?.let { id -> id to table.getAllAllowToShowColumns().map { it.columnName } }
        } else {
            val columns = table.getAllAllowToShowFieldsInUpsert()
            val changedData = parameters.mapIndexed { index, item ->
                columns[index] to item
            }.filterIndexed { index, item ->
                val initialValue = initialData.getOrNull(index)
                checkIsChangedData(
                    item.first,
                    initialValue,
                    item.second
                ) && !(initialValue != null && item.second == null)
            }
            println("Changed data : $changedData")
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

    private fun AdminJdbcTable.createGetAllQuery(
        search: String?,
        currentPage: Int?,
        filters: List<Pair<ColumnSet, String>>
    ) = buildString {
        val columns = getAllAllowToShowColumns().plus(getPrimaryKeyColumn()).distinctBy { it.columnName }
        val selectColumns = columns.map { columnSet ->
            "${getTableName()}.${columnSet.columnName} AS ${getTableName()}_${columnSet.columnName}"
        }

        append("SELECT ")
        append(selectColumns.joinToString(", "))
        append(" FROM ")
        append(getTableName())

        if (search != null || filters.isNotEmpty()) {
            append(" ")
            append(createFiltersConditions(search, filters))
        }

        currentPage?.let {
            append(createPaginationQuery(it))
        }
    }.also { println("QUERY : $it") }

    private fun AdminJdbcTable.createFiltersConditions(
        search: String?,
        filters: List<Pair<ColumnSet, String>>
    ): String {
        val joinConditions = mutableListOf<String>()
        val searchConditions = if (search != null) {
            getSearchColumns().map { columnPath ->
                val pathParts = columnPath.split('.')
                var currentTable = getTableName()
                val currentColumn = pathParts.last()

                pathParts.firstOrNull()?.let { part ->
                    val columnSet = getAllColumns().find { it.columnName == part }
                    val nextTable = columnSet?.reference?.tableName
                    val currentReferenceColumn = columnSet?.reference?.columnName

                    if (nextTable != null && currentReferenceColumn != null && pathParts.size > 1) {
                        joinConditions.add("LEFT JOIN $nextTable ON ${currentTable}.${part} = ${nextTable}.${currentReferenceColumn}")
                        currentTable = nextTable
                    }
                }

                "LOWER(${currentTable}.${currentColumn}) LIKE LOWER('%$search%')"
            }
        } else emptyList()

        val filterConditions = if (filters.isEmpty()) emptyList() else getFilterColumns().mapNotNull { item ->
            val pathParts = item.split('.')
            var currentTable = getTableName()
            val currentColumn = pathParts.last()
            pathParts.firstOrNull()?.let { part ->
                println("filters does not exists $part")
                if (!filters.any { it.first.columnName == part }) {
                    return@let null
                }
                val columnSet = getAllColumns().find { it.columnName == part }
                println(columnSet)
                val nextTable = columnSet?.reference?.tableName
                val currentReferenceColumn = columnSet?.reference?.columnName

                if (nextTable != null && currentReferenceColumn != null && pathParts.size > 1) {
                    joinConditions.add("LEFT JOIN $nextTable ON ${currentTable}.${part} = ${nextTable}.${currentReferenceColumn}")
                    currentTable = nextTable
                }
                filters.filter { it.first == columnSet }
                    .joinToString(" AND ", prefix = "", postfix = "") { filterItem ->
                        "${currentTable}.${currentColumn} ${filterItem.second}".also { println(it) }
                    }
            }
        }
        println("$filterConditions , $searchConditions")
        return if (filterConditions.isEmpty() && searchConditions.isEmpty()) {
            ""
        } else {
            buildString {
                append(
                    joinConditions.distinct().joinToString(" ")
                )
                append(" WHERE ")
                if (searchConditions.isNotEmpty()) {
                    append(
                        searchConditions.joinToString(
                            " OR "
                        ) { it })
                    if (filters.isNotEmpty()) {
                        append(" AND ")
                    }
                }
                append(filterConditions.joinToString(" AND ") { it })
            }
        }
    }

    private fun createPaginationQuery(currentPage: Int) = buildString {
        append(" LIMIT ${DynamicConfiguration.maxItemsInPage}")
        append(" OFFSET ${DynamicConfiguration.maxItemsInPage * currentPage}")
    }

    private fun AdminJdbcTable.createGetAllReferencesQuery(leftReferenceColumn: String): String {
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

    private fun AdminJdbcTable.createGetOneItemQuery(primaryKey: String) = buildString {
        append("SELECT ")
        append(
            getAllAllowToShowFieldsInUpsert()
                .plus(getPrimaryKeyColumn())
                .distinct()
                .joinToString(", ") { it.columnName }
        )
        append(" FROM ")
        append(getTableName())
        append(" WHERE ")
        append(getPrimaryKey())
        append(" = ")
        append(primaryKey)
    }

    private fun String?.formatParameter(columnSet: ColumnSet): String = when {
        columnSet.type == ColumnType.BOOLEAN -> this?.let { if (it == "on") "'1'" else "'0'" } ?: NULL
        else -> this?.addQuotationIfIsString() ?: NULL
    }

    private fun AdminJdbcTable.createInsertQuery(parameters: List<String?>) = buildString {
        val columns = getAllAllowToShowFieldsInUpsert()
        val parametersWithNULL = parameters.mapIndexed { index, parameter ->
            if (columns[index].nullable && parameter.isNullOrEmpty()) NULL else parameter.formatParameter(columns[index])
        }
        append("INSERT INTO ")
        append(getTableName())
        append(" (")
        append(columns.joinToString(", ") { it.columnName })
        append(") VALUES (")
        append(parametersWithNULL.joinToString(", "))
        append(")")
    }

    private fun AdminJdbcTable.createUpdateQuery(
        updatedColumns: List<ColumnSet>,
        parameters: List<String?>,
        primaryKey: String
    ) = buildString {
        append("UPDATE ")
        append(getTableName())
        append(" SET ")
        val columnsWithValues = updatedColumns.mapIndexed { index, column ->
            column.columnName to parameters[index]?.let {
                if (it.isEmpty() && column.nullable) NULL else it.formatParameter(column)
            }
        }
        append(columnsWithValues.joinToString(", ") { (columnName, value) -> "$columnName = $value" })
        append(" WHERE ")
        append(getPrimaryKey())
        append(" = ")
        append(primaryKey)
    }.also { println(it) }

    private fun String.addQuotationIfIsString(): String =
        if (toDoubleOrNull() != null) this else "'$this'"

    private fun AdminJdbcTable.getPrimaryKeyColumn() = getAllColumns().first { it.columnName == getPrimaryKey() }
}