package repository

import com.vladsch.kotlin.jdbc.*
import configuration.DynamicConfiguration
import formatters.extractTextInCurlyBraces
import formatters.populateTemplate
import getters.putColumn
import getters.toTypedValue
import models.ColumnSet
import models.DataWithPrimaryKey
import models.common.DisplayItem
import models.getCurrentDate
import models.getCurrentDateClass
import models.order.Order
import models.types.ColumnType
import panels.*

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
        filters: MutableList<Pair<ColumnSet, String>>,
        order: Order?
    ): List<DataWithPrimaryKey> =
        table.usingDataSource { session ->
            session.list(
                sqlQuery(
                    table.createGetAllQuery(
                        search = search,
                        currentPage = currentPage,
                        filters = filters,
                        order = order
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
            session.count(sqlQuery(table.createGetAllQuery(search = search, null, filters, null)))
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
                table.getAllAllowToShowColumnsInUpsert().map { raw.anyOrNull(it.columnName)?.toString() }
            }
        }

    fun insertData(table: AdminJdbcTable, parameters: List<Any?>): Int {
        return table.usingDataSource { session ->
            session.transaction { tx ->
                tx.prepare(sqlQuery(table.createInsertQuery().also { println("SQL QUERY $it ${parameters.size}") })).use { preparedStatement ->
                    val columns = table.getAllAllowToShowColumnsInUpsert()
                    val insertAutoDateColumns = table.getAllAutoNowDateInsertColumns()

                    // Check if the size of parameters matches the size of allColumns
                    if (parameters.size != columns.size) {
                        throw IllegalArgumentException("The number of parameters does not match the number of columns")
                    }

                    // Insert the main columns
                    columns.forEachIndexed { index, columnSet ->
                        preparedStatement.putColumn(columnSet.type, parameters[index], index + 1)
                    }

                    // Insert the auto-now date columns
                    insertAutoDateColumns.forEachIndexed { index, columnSet ->
                        preparedStatement.putColumn(
                            columnSet.type,
                            columnSet.getCurrentDateClass(),
                            columns.size + index + 1
                        )
                    }
                    preparedStatement.executeUpdate()
                }
            }
        }
    }

    fun updateChangedData(
        table: AdminJdbcTable,
        parameters: List<Pair<String, Any?>?>,
        primaryKey: String,
        initialData: List<String?>? = getData(table, primaryKey)
    ): Pair<Int, List<String>>? {
        return if (initialData == null) {
            insertData(table, parameters.map { it?.second })?.let { id ->
                id to table.getAllAllowToShowColumns().map { it.columnName }
            }
        } else {
            val columns = table.getAllAllowToShowColumnsInUpsert()
            val changedData = parameters.mapIndexed { index, item ->
                columns[index] to item
            }.filterIndexed { index, item ->
                val initialValue = initialData.getOrNull(index)
                checkIsChangedData(
                    item.first,
                    initialValue,
                    item.second?.first
                ) && !(initialValue != null && item.second?.first == null)
            }
            if (changedData.isNotEmpty() || table.getAllAutoNowDateUpdateColumns().isNotEmpty()) {
                table.usingDataSource { session ->
                    session.transaction { tx ->
                        tx.prepare(
                            sqlQuery(
                                table.createUpdateQuery(
                                    changedData.map { it.first },
                                )
                            )
                        ).use { prepareStatement ->
                            changedData.forEachIndexed { index, item ->
                                prepareStatement.putColumn(
                                    item.first.type, item.second?.second, index + 1
                                )
                            }
                            val autoNowDates = table.getAllAutoNowDateUpdateColumns()
                            autoNowDates.forEachIndexed { index, columnSet ->
                                prepareStatement.putColumn(
                                    columnSet.type,
                                    value = columnSet.getCurrentDateClass(),
                                    index = index + 1 + changedData.size
                                )
                            }
                            val primaryKeyColumn = table.getPrimaryKeyColumn()
                            val primaryKeyTyped = primaryKey.toTypedValue(primaryKeyColumn.type)
                            prepareStatement.putColumn(
                                primaryKeyColumn.type,
                                primaryKeyTyped,
                                changedData.size + autoNowDates.size + 1
                            )
                            prepareStatement.executeUpdate()
                        }
                    }
                }.let { id -> id to changedData.map { it.first.columnName } }
            } else null
        }
    }

    private fun AdminJdbcTable.createGetAllQuery(
        search: String?,
        currentPage: Int?,
        filters: List<Pair<ColumnSet, String>>,
        order: Order? = null
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
        order?.let {
            append(" ORDER BY ${it.name} ${it.direction}")
        }
        currentPage?.let {
            append(createPaginationQuery(it))
        }
    }

    private fun AdminJdbcTable.createFiltersConditions(
        search: String?,
        filters: List<Pair<ColumnSet, String>>
    ): String {
        val joinConditions = mutableListOf<String>()
        val searchConditions = if (search != null) {
            getSearches().map { columnPath ->
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

        val filterConditions = if (filters.isEmpty()) emptyList() else getFilters().mapNotNull { item ->
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
                append(filterConditions.joinToString(" OR ") { it })
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
            getDefaultOrder()?.let { order ->
                append(" ORDER BY ${order.name} ${order.direction}")
            }
        }
    }

    private fun AdminJdbcTable.createGetOneItemQuery(primaryKey: String) = buildString {
        append("SELECT ")
        append(
            getAllAllowToShowColumnsInUpsert()
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

    private fun AdminJdbcTable.createInsertQuery() = buildString {
        val columns = getAllAllowToShowColumnsInUpsert()
        val insertAutoDateColumns = getAllAutoNowDateInsertColumns()
        val allColumns = (columns + insertAutoDateColumns).distinct()
        append("INSERT INTO ")
        append(getTableName())
        append(" (")
        append(allColumns.joinToString(", ") { it.columnName })
        append(") VALUES (")
        append(allColumns.joinToString(", ") { "?" })
        append(")")
    }

    private fun AdminJdbcTable.createUpdateQuery(
        updatedColumns: List<ColumnSet>,
    ) = buildString {
        append("UPDATE ")
        append(getTableName())
        append(" SET ")
        val updateAutoDateColumns = getAllAutoNowDateUpdateColumns()
        append(
            updatedColumns.plus(updateAutoDateColumns)
                .joinToString(", ") { column -> "${column.columnName} = ?" })
        append(" WHERE ")
        append(getPrimaryKey())
        append(" = ?")
    }.also { println(it) }

    private fun String.addQuotationIfIsString(): String =
        if (toDoubleOrNull() != null) this else "'$this'"

    private fun AdminJdbcTable.getPrimaryKeyColumn() = getAllColumns().first { it.columnName == getPrimaryKey() }


    fun deleteRows(table: AdminJdbcTable, selectedIds: List<String>) {
        table.usingDataSource { session ->
            session.prepare(
                sqlQuery(
                    "DELETE FROM ${table.getTableName()} WHERE ${table.getPrimaryKey()} IN (${selectedIds.joinToString { "?" }})"
                )
            ).use { preparedStatement ->
                val primaryKeyColumn = table.getPrimaryKeyColumn()
                selectedIds.forEachIndexed { index, id ->
                    val item = id.toTypedValue(primaryKeyColumn.type)
                    preparedStatement.putColumn(primaryKeyColumn.type, item, index + 1)
                }
                preparedStatement.executeUpdate()
            }
        }
    }
}