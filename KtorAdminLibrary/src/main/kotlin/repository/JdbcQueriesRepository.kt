package repository

import com.vladsch.kotlin.jdbc.*
import configuration.DynamicConfiguration
import dashboard.chart.ChartDashboardSection
import dashboard.simple.TextDashboardSection
import models.chart.ChartDashboardAggregationFunction
import models.chart.getFieldFunctionBasedOnAggregationFunction
import models.chart.getFieldNameBasedOnAggregationFunction
import formatters.extractTextInCurlyBraces
import formatters.formatToDisplayInTable
import formatters.populateTemplate
import getters.putColumn
import getters.toTypedValue
import models.ColumnSet
import models.DataWithPrimaryKey
import models.chart.ChartData
import models.chart.ChartLabelsWithValues
import models.chart.TextDashboardAggregationFunction
import models.chart.TextData
import models.common.DisplayItem
import models.getCurrentDateClass
import models.order.Order
import models.types.ColumnType
import panels.*
import utils.formatAsIntegerIfPossible
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Repository class for handling JDBC database operations.
 * Provides methods for CRUD operations and data retrieval with filtering and pagination.
 */
internal object JdbcQueriesRepository {
    // region Database Connection

    /**
     * Executes database operations using the specified data source.
     * @param lambda Operation to execute within the database session
     * @return Result of the operation
     */
    private fun <T> AdminJdbcTable.usingDataSource(lambda: (Session) -> T): T {
        val key = getDatabaseKey()
        val dataSource = if (key == null) HikariCP.dataSource() else HikariCP.dataSource(key)
        return using(session(dataSource), lambda)
    }

    // endregion

    // region Data Retrieval

    /**
     * Retrieves all data from the table with optional filtering, search, pagination, and ordering.
     * @param table Target database table
     * @param search Search string to filter results
     * @param currentPage Page number for pagination
     * @param filters List of column filters
     * @param order Sorting order
     * @return List of data with primary keys
     */
    fun getAllData(
        table: AdminJdbcTable,
        search: String?,
        currentPage: Int?,
        filters: MutableList<Triple<ColumnSet, String, Any>>,
        order: Order?
    ): List<DataWithPrimaryKey> {
        val result = mutableListOf<DataWithPrimaryKey>()
        table.usingDataSource { session ->
            session.prepare(
                sqlQuery(
                    table.createGetAllQuery(
                        search = search,
                        currentPage = currentPage,
                        filters = filters,
                        order = order
                    )
                )
            ).use { prepareStatement ->
                prepareStatement.prepareGetAllData(
                    table, search, filters, currentPage
                )
                prepareStatement.executeQuery().use { rs ->
                    while (rs.next()) {
                        val primaryKey =
                            rs.getObject("${table.getTableName()}_${table.getPrimaryKey()}")?.toString() ?: "UNKNOWN"
                        val data = table.getAllAllowToShowColumns().map { column ->
                            rs.getObject("${table.getTableName()}_${column.columnName}")
                                .formatToDisplayInTable(column.type)
                        }
                        result.add(DataWithPrimaryKey(primaryKey, data))
                    }
                }
            }
        }
        return result
    }

    /**
     * Gets the total count of records matching the specified criteria.
     * @param table Target database table
     * @param search Search string to filter results
     * @param filters List of column filters
     * @return Total count of matching records
     */
    fun getCount(
        table: AdminJdbcTable,
        search: String?,
        filters: List<Triple<ColumnSet, String, Any>>
    ): Int {
        return table.usingDataSource { session ->
            session.prepare(sqlQuery(table.createGetAllCountQuery(search = search, null, filters, null)))
                .use { preparedStatement ->
                    preparedStatement.prepareGetAllData(table, search, filters, null)
                    preparedStatement.executeQuery()?.use { rs ->
                        if (rs.next()) {
                            rs.getInt(1)
                        } else 0
                    }
                }
        } ?: 0
    }

    /**
     * Retrieves and processes chart data from the specified table based on the provided chart configuration.
     *
     * This function executes a SQL query to fetch relevant data, applies aggregation (if specified),
     * and structures the data into `ChartData` format. It supports different aggregation functions:
     * - `ALL`: Stores individual values separately without aggregation.
     * - `SUM`, `COUNT`, `AVERAGE`: Aggregates values based on the selected function.
     *
     * The function ensures:
     * - Unique labels are collected.
     * - Values are grouped per label.
     * - Colors are assigned using `provideFillColor` and `provideBorderColor`.
     *
     * @param table The database table containing the chart data.
     * @param section The chart configuration defining fields, aggregation, and other settings.
     * @return A `ChartData` object containing labels and their corresponding values.
     */
    fun getChartData(table: AdminJdbcTable, section: ChartDashboardSection): ChartData {
        val groupedData = mutableMapOf<String, MutableList<MutableList<Double>>>() // Store lists separately for "ALL"
        val aggregationFunction = section.aggregationFunction
        val labelsSet =
            if (aggregationFunction == ChartDashboardAggregationFunction.ALL) mutableListOf() else mutableSetOf<String>()

        return table.usingDataSource { session ->
            session.prepare(sqlQuery(section.createGetAllChartData())).use { preparedStatement ->
                section.limitCount?.let { preparedStatement.setInt(1, it) }

                preparedStatement.executeQuery().use { rs ->
                    while (rs.next()) {
                        val label = rs.getLabelOrDefault(section.labelField)
                        labelsSet.add(label)

                        val values = section.valuesFields.map { field ->
                            rs.getDoubleOrDefault(
                                getFieldNameBasedOnAggregationFunction(
                                    aggregationFunction,
                                    field.fieldName
                                )
                            )
                        }

                        // If ALL, store values separately without aggregation
                        if (aggregationFunction == ChartDashboardAggregationFunction.ALL) {
                            groupedData.computeIfAbsent(label) { MutableList(section.valuesFields.size) { mutableListOf() } }
                                .forEachIndexed { index, list -> list.add(values[index]) }
                        } else {
                            // Aggregate values for SUM, COUNT, AVERAGE
                            groupedData.computeIfAbsent(label) {
                                MutableList(section.valuesFields.size) {
                                    mutableListOf(
                                        0.0
                                    )
                                }
                            }
                                .forEachIndexed { index, list ->
                                    list[0] += values[index] // Accumulate the values
                                }
                        }
                    }
                }

                val labels = labelsSet.toList()
                val values = section.valuesFields.mapIndexed { index, field ->
                    val currentValues = mutableListOf<Double>()
                    val currentIndexes = mutableMapOf<String, Int>()
                    labels.distinct().forEach { currentIndexes[it] = 0 }
                    labels.forEach { label ->
                        groupedData[label]?.get(index)?.get(currentIndexes[label]!!)?.let { currentValues.add(it) }
                        currentIndexes[label] = currentIndexes[label]!! + 1
                    }

                    // Create ChartLabelsWithValues with the correct values, colors, and labels
                    ChartLabelsWithValues(
                        displayName = field.displayName,
                        values = currentValues,
                        fillColors = labels.map { section.provideFillColor(it, field.displayName) },
                        borderColors = labels.map { section.provideBorderColor(it, field.displayName) }
                    )
                }

                ChartData(
                    labels = labels,
                    values = values,
                    section = section
                )
            }
        }
    }

    fun getTextData(table: AdminJdbcTable, section: TextDashboardSection): TextData {
        return table.usingDataSource { session ->
            session.prepare(sqlQuery(section.createGetAllData())).use { prepareStatement ->
                prepareStatement.executeQuery().use { rs ->
                    var value = ""
                    value = when (section.aggregationFunction) {
                        TextDashboardAggregationFunction.LAST_ITEM -> {
                            if (rs.next()) {
                                val itemObject = rs.getObject(section.fieldName)
                                itemObject?.toString()?.toDoubleOrNull()?.formatAsIntegerIfPossible()?.toString()
                                    ?: itemObject.toString()
                            } else ""
                        }

                        TextDashboardAggregationFunction.PROFIT_PERCENTAGE -> {
                            var nextItem = 0.0
                            var previewsItem = 0.0
                            if (rs.next()) {
                                nextItem = rs.getDouble(
                                    section.fieldName
                                )
                            }
                            if (rs.next()) {
                                previewsItem = rs.getDouble(
                                    section.fieldName
                                )
                            }
                            runCatching { ((nextItem - previewsItem).div(previewsItem) * 100).formatAsIntegerIfPossible() }.getOrNull()
                                .toString() + "%"
                        }

                        else -> {
                            if (rs.next()) {
                                rs.getDouble("aggregationFunctionValue").formatAsIntegerIfPossible().toString()
                            } else ""
                        }
                    }
                    TextData(
                        value = value,
                        section = section
                    )
                }
            }
        }
    }

    private fun ResultSet.getLabelOrDefault(field: String) = getObject(field)?.toString() ?: "N/A"

    private fun ResultSet.getDoubleOrDefault(field: String) = getObject(field)?.toString()?.toDoubleOrNull() ?: 0.0


    /**
     * Prepares statement parameters for data retrieval operations.
     */
    private fun PreparedStatement.prepareGetAllData(
        table: AdminJdbcTable,
        search: String?,
        filters: List<Triple<ColumnSet, String, Any>>,
        currentPage: Int?,
    ) {
        val columns = table.getAllColumns()
        val searches = table.getSearches()
        val filtersColumns = table.getFilters()

        // Process filters
        val hasFilters = if (filters.isEmpty()) emptyList() else filtersColumns.mapNotNull { item ->
            val pathParts = item.split('.')
            pathParts.first().let { part ->
                if (!filters.any { it.first.columnName == part }) {
                    return@mapNotNull null
                } else {
                    return@mapNotNull columns.first { it.columnName == part }
                }
            }
        }

        // Process searches
        val hasSearches = if (search == null) emptyList() else searches.mapNotNull { item ->
            val pathParts = item.split('.')
            pathParts.first().let { part ->
                columns.find { it.columnName == part }
            }
        }

        // Set search parameters
        if (hasSearches.isNotEmpty()) {
            hasSearches.forEachIndexed { index, _ ->
                setString(
                    index + 1,
                    "%${search!!}%",
                )
            }
        }

        // Set filter parameters
        if (hasFilters.isNotEmpty()) {
            var currentIndex = hasSearches.size + 1
            hasFilters.forEach { columnSet ->
                val correspondFilters = filters.filter { it.first.columnName == columnSet.columnName }
                correspondFilters.forEach { filter ->
                    putColumn(
                        columnType = columnSet.type,
                        value = filter.third,
                        index = currentIndex
                    )
                    currentIndex++
                }
            }
        }
        val filtersNames = hasFilters.map { it.columnName }
        val filtersCount = filters.count { it.first.columnName in filtersNames }


        // Set pagination parameters
        if (currentPage != null) {
            setInt(
                filtersCount + hasSearches.size + 1,
                DynamicConfiguration.maxItemsInPage
            )
            setInt(
                filtersCount + hasSearches.size + 2,
                DynamicConfiguration.maxItemsInPage * currentPage
            )
        }
    }

    // endregion

    // region Reference Data

    /**
     * Retrieves reference data for a specific column.
     * @param table Target database table
     * @param referenceColumn Column to get references for
     * @return List of display items
     */
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

    // endregion

    // region Data Modification

    /**
     * Checks if data has been changed compared to initial value.
     */
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

    /**
     * Retrieves data for a specific primary key.
     */
    fun getData(table: AdminJdbcTable, primaryKey: String): List<String?>? =
        table.usingDataSource { session ->
            session.prepare(sqlQuery(table.createGetOneItemQuery())).use { prepareStatement ->
                val primaryKeyType = table.getPrimaryKeyColumn().type
                prepareStatement.putColumn(
                    columnType = primaryKeyType,
                    value = primaryKey.toTypedValue(primaryKeyType),
                    index = 1
                )
                prepareStatement.executeQuery().use { rs ->
                    if (rs.next()) {
                        return@usingDataSource table.getAllAllowToShowColumnsInUpsert().map { column ->
                            rs.getObject(column.columnName)?.toString()
                        }
                    }
                    return@usingDataSource null
                }
            }
        }

    /**
     * Inserts new data into the table.
     */
    fun insertData(table: AdminJdbcTable, parameters: List<Any?>): Int {
        return table.usingDataSource { session ->
            session.transaction { tx ->
                tx.prepare(sqlQuery(table.createInsertQuery())).use { preparedStatement ->
                    val columns = table.getAllAllowToShowColumnsInUpsert()
                    val insertAutoDateColumns = table.getAllAutoNowDateInsertColumns()

                    // Validate parameters count
                    if (parameters.size != columns.size) {
                        throw IllegalArgumentException("The number of parameters does not match the number of columns")
                    }

                    // Insert main columns
                    columns.forEachIndexed { index, columnSet ->
                        preparedStatement.putColumn(columnSet.type, parameters[index], index + 1)
                    }

                    // Insert auto-now date columns
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

    /**
     * Updates changed data for a specific record.
     */
    fun updateChangedData(
        table: AdminJdbcTable,
        parameters: List<Pair<String, Any?>?>,
        primaryKey: String,
        initialData: List<String?>? = getData(table, primaryKey)
    ): Pair<Int, List<String>>? {
        return if (initialData == null) {
            insertData(table, parameters.map { it?.second }).let { id ->
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

    // endregion

    // region Query Building

    /**
     * Creates query for retrieving all data with optional filters and pagination.
     */
    private fun AdminJdbcTable.createGetAllQuery(
        search: String?,
        currentPage: Int?,
        filters: List<Triple<ColumnSet, String, Any>>,
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
            if (it.name !in columns.map { column -> column.columnName } && it.direction.lowercase() !in listOf(
                    "asc",
                    "desc"
                )) return@let
            append(" ORDER BY ${it.name} ${it.direction}")
        }
        currentPage?.let {
            append(createPaginationQuery())
        }
    }

    /**
     * Creates query for retrieving total count with filters.
     */
    private fun AdminJdbcTable.createGetAllCountQuery(
        search: String?,
        currentPage: Int?,
        filters: List<Triple<ColumnSet, String, Any>>,
        order: Order? = null
    ) = buildString {
        val columns = getAllAllowToShowColumns().plus(getPrimaryKeyColumn()).distinctBy { it.columnName }

        append("SELECT COUNT(*)")
        append(" FROM ")
        append(getTableName())

        if (search != null || filters.isNotEmpty()) {
            append(" ")
            append(createFiltersConditions(search, filters))
        }
        order?.let {
            if (it.name !in columns.map { column -> column.columnName } && it.direction.lowercase() !in listOf(
                    "asc",
                    "desc"
                )) return@let
            append(" ORDER BY ${it.name} ${it.direction}")
        }
        currentPage?.let {
            append(createPaginationQuery())
        }
    }

    /**
     * Creates filter conditions for SQL queries.
     */
    private fun AdminJdbcTable.createFiltersConditions(
        search: String?,
        filters: List<Triple<ColumnSet, String, Any>>
    ): String {
        val joinConditions = mutableListOf<String>()
        val searchConditions = if (search != null) {
            getSearches().map { columnPath ->
                val pathParts = columnPath.split('.')
                var currentTable = getTableName()
                val currentColumn = pathParts.last()

                pathParts.first().let { part ->
                    val columnSet = getAllColumns().find { it.columnName == part }
                    val nextTable = columnSet?.reference?.tableName
                    val currentReferenceColumn = columnSet?.reference?.columnName

                    if (nextTable != null && currentReferenceColumn != null && pathParts.size > 1) {
                        joinConditions.add("LEFT JOIN $nextTable ON ${currentTable}.${part} = ${nextTable}.${currentReferenceColumn}")
                        currentTable = nextTable
                    }
                }

                "LOWER(${currentTable}.${currentColumn}) LIKE LOWER(?)"
            }
        } else emptyList()

        val filterConditions = if (filters.isEmpty()) emptyList() else getFilters().mapNotNull { item ->
            val pathParts = item.split('.')
            var currentTable = getTableName()
            val currentColumn = pathParts.last()

            pathParts.first().let { part ->
                if (!filters.any { it.first.columnName == part }) {
                    return@mapNotNull null
                }
                val columnSet = getAllColumns().find { it.columnName == part }
                val nextTable = columnSet?.reference?.tableName
                val currentReferenceColumn = columnSet?.reference?.columnName

                if (nextTable != null && currentReferenceColumn != null && pathParts.size > 1) {
                    joinConditions.add("LEFT JOIN $nextTable ON ${currentTable}.${part} = ${nextTable}.${currentReferenceColumn}")
                    currentTable = nextTable
                }
                filters.filter { it.first.columnName == columnSet?.columnName }
                    .joinToString(" AND ", prefix = "", postfix = "") { filterItem ->
                        "${currentTable}.${currentColumn} ${filterItem.second} ?"
                    }
            }
        }
        return if (filterConditions.isEmpty() && searchConditions.isEmpty()) {
            ""
        } else {
            buildString {
                append(joinConditions.distinct().joinToString(" "))
                append(" WHERE ")
                if (searchConditions.isNotEmpty()) {
                    append(searchConditions.joinToString(" OR ") { it })
                    if (filterConditions.isNotEmpty()) {
                        append(" AND ")
                    }
                }
                append(filterConditions.joinToString(" AND ") { it })
            }
        }
    }

    /**
     * Creates pagination query part.
     */
    private fun createPaginationQuery() = buildString {
        append(" LIMIT ?")
        append(" OFFSET ?")
    }

    /**
     * Creates query for retrieving all references.
     */
    private fun AdminJdbcTable.createGetAllReferencesQuery(leftReferenceColumn: String): String {
        val columns = getDisplayFormat()?.extractTextInCurlyBraces().orEmpty()
        val selectColumns = mutableSetOf<String>()
        val joins = mutableListOf<String>()

        selectColumns.add("${getTableName()}.$leftReferenceColumn AS ${getTableName()}_$leftReferenceColumn")

        val order = getDefaultOrder()
        order?.let {
            selectColumns.add("${getTableName()}.${it.name} AS ${getTableName()}_${it.name}")
        }

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
            order?.let { order ->
                if (order.name !in getAllColumns().map { column -> column.columnName } &&
                    order.direction.lowercase() !in listOf("asc", "desc")) return@let
                append(" ORDER BY ${order.name} ${order.direction}")
            }
        }
    }

    /**
     * Constructs an SQL query to retrieve chart data based on the specified configuration.
     *
     * This function dynamically builds a SQL `SELECT` query, incorporating:
     * - The label field (`labelField`) as a grouping key.
     * - Value fields with the appropriate aggregation function (`COUNT`, `SUM`, `AVG`).
     * - Optional ordering (`ORDER BY`) if specified.
     * - Optional row limiting (`LIMIT ?`) if a limit is provided.
     * - If aggregation is applied (excluding `ALL`), the query includes `GROUP BY labelField`.
     *
     * @return A dynamically generated SQL query string.
     */
    private fun ChartDashboardSection.createGetAllChartData() = buildString {
        val orderField = orderQuery?.substringBeforeLast(" ")?.trim()
        append("SELECT $labelField, ")
        if (orderField != null) {
            append("$orderField, ")
        }
        append(valuesFields.map {
            if (it.fieldName == orderField && orderField !in valuesFields.map { it.fieldName }) return@map it
            getFieldFunctionBasedOnAggregationFunction(aggregationFunction, it.fieldName)
        }.distinct().joinToString(", "))
        append(" FROM ")
        append(tableName)
        if (aggregationFunction != ChartDashboardAggregationFunction.ALL) {
            append(" GROUP BY $labelField")
        }
        orderQuery?.let {
            append(" ORDER BY $it")
        }
        limitCount?.let {
            append(" LIMIT ?")
        }
    }

    /**
     * Constructs an SQL query to retrieve data based on the specified configuration for a text dashboard.
     *
     * This function dynamically builds an SQL `SELECT` query based on:
     * - The field to aggregate (`fieldName`), applying the appropriate aggregation function (`COUNT`, `AVG`, `SUM`).
     * - Optional sorting (`ORDER BY`), if specified through `orderQuery`.
     * - Optional row limiting (`LIMIT ?`), if a limit is provided through `limitCount`.
     * - If aggregation is applied, the query will return the aggregated result using an alias (`aggregationFunctionValue`).
     *
     * @return A dynamically generated SQL query string.
     */

    private fun TextDashboardSection.createGetAllData() = buildString {
        when (aggregationFunction) {
            TextDashboardAggregationFunction.PROFIT_PERCENTAGE -> {
                // Generates a query to select field values for the last 2 records, sorted by date
                append("SELECT $fieldName FROM $tableName ${orderQuery?.let { "ORDER BY $it" }.orEmpty()} LIMIT 2")
            }

            TextDashboardAggregationFunction.LAST_ITEM -> {
                // Generates a query to select the field value for the last record, sorted by date
                append("SELECT $fieldName FROM $tableName ${orderQuery?.let { "ORDER BY $it" }.orEmpty()} LIMIT 1")
            }

            else -> {
                // Generates a query with the appropriate aggregation function (e.g., COUNT, AVG, SUM)
                val aggregationFunctionQuery = when (aggregationFunction) {
                    TextDashboardAggregationFunction.COUNT -> "COUNT"  // Count the number of records
                    TextDashboardAggregationFunction.AVERAGE -> "AVG"  // Calculate the average value
                    TextDashboardAggregationFunction.SUM -> "SUM"      // Calculate the sum of the field
                    else -> ""  // Fallback case if no aggregation function is specified
                }
                // Constructs the query to apply the aggregation function and provide a result alias
                append("SELECT $aggregationFunctionQuery($fieldName) as aggregationFunctionValue FROM $tableName")
            }
        }
    }

    /**
     * Creates query for retrieving a single item by primary key.
     */
    private fun AdminJdbcTable.createGetOneItemQuery() = buildString {
        append("SELECT ")
        append(
            getAllAllowToShowColumnsInUpsert()
                .plus(getPrimaryKeyColumn())
                .distinct()
                .joinToString(", ") { it.columnName }
        )
        append(" FROM ${getTableName()} WHERE ${getPrimaryKey()} = ?")
    }

    /**
     * Creates query for inserting new data.
     */
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

    /**
     * Creates query for updating existing data.
     */
    private fun AdminJdbcTable.createUpdateQuery(
        updatedColumns: List<ColumnSet>,
    ) = buildString {
        append("UPDATE ")
        append(getTableName())
        append(" SET ")
        val updateAutoDateColumns = getAllAutoNowDateUpdateColumns()
        append(
            updatedColumns.plus(updateAutoDateColumns)
                .joinToString(", ") { column -> "${column.columnName} = ?" }
        )
        append(" WHERE ")
        append(getPrimaryKey())
        append(" = ?")
    }

    /**
     * Gets the primary key column for the table.
     */
    private fun AdminJdbcTable.getPrimaryKeyColumn() = getAllColumns().first { it.columnName == getPrimaryKey() }

    /**
     * Deletes multiple rows by their IDs.
     */
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
                preparedStatement.execute()
            }
        }
    }
}