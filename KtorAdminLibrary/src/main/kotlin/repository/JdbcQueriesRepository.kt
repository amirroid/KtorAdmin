package repository

import com.vladsch.kotlin.jdbc.*
import configuration.DynamicConfiguration
import dashboard.chart.ChartDashboardSection
import dashboard.list.ListDashboardSection
import dashboard.simple.TextDashboardSection
import models.chart.ChartDashboardAggregationFunction
import models.chart.getFieldFunctionBasedOnAggregationFunction
import models.chart.getFieldNameBasedOnAggregationFunction
import formatters.extractTextInCurlyBraces
import formatters.formatToDisplayInTable
import formatters.formatToDisplayInUpsert
import formatters.getTypedValue
import formatters.map
import formatters.populateTemplate
import formatters.restore
import getters.putColumn
import getters.toTypedValue
import hikra.KtorAdminHikariCP
import models.ColumnSet
import models.DataWithPrimaryKey
import models.chart.ChartData
import models.chart.ChartLabelsWithValues
import models.chart.FieldData
import models.chart.ListData
import models.chart.TextDashboardAggregationFunction
import models.chart.TextData
import models.common.DisplayItem
import models.common.Reference
import models.common.foreignKey
import models.common.tableName
import models.getCurrentDateClass
import models.order.Order
import models.reference.ReferenceData
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

    /**
     * Executes database operations using the specified data source.
     * @param lambda Operation to execute within the database session
     * @return Result of the operation
     */
    private fun <T> AdminJdbcTable.usingDataSource(lambda: (Session) -> T): T {
        val dataSource = getDatabaseKey()?.let { KtorAdminHikariCP.dataSource(it) } ?: KtorAdminHikariCP.dataSource()
        val session = session(dataSource)
        val invoke = using(session, lambda)
        session.close()
        return invoke
    }

    /**
     * Retrieves all data from the table with optional filtering, search, pagination, and ordering.
     * @param table Target database table
     * @param tables The list of tables
     * @param search Search string to filter results
     * @param currentPage Page number for pagination
     * @param filters List of column filters
     * @param order Sorting order
     * @return List of data with primary keys
     */
    fun getAllData(
        table: AdminJdbcTable,
        tables: List<AdminJdbcTable>,
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
                            column.mapDataIfReference(
                                value = rs.getTypedValue(column.type, "${table.getTableName()}_${column.columnName}")
                                    .restore(column),
                                tables = tables
                            )
                        }
                        result.add(DataWithPrimaryKey(primaryKey, data))
                    }
                }
            }
        }
        return result
    }

    private fun ColumnSet.mapDataIfReference(value: Any?, tables: List<AdminJdbcTable>): Any {
        return if (reference != null && value != null) {
            val relatedTable = tables.find { it.getTableName() == reference.tableName }
            if (relatedTable != null) {
                ReferenceData(
                    value = value.formatToDisplayInTable(type),
                    pluralName = relatedTable.getPluralName()
                )
            } else value.formatToDisplayInTable(type)
        } else {
            value.formatToDisplayInTable(type)
        }
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
    ): Long {
        return table.usingDataSource { session ->
            session.prepare(sqlQuery(table.createGetAllCountQuery(search = search, null, filters, null)))
                .use { preparedStatement ->
                    preparedStatement.prepareGetAllData(table, search, filters, null)
                    preparedStatement.executeQuery()?.use { rs ->
                        if (rs.next()) {
                            rs.getLong(1)
                        } else 0
                    }
                }
        } ?: 0
    }


    /**
     * Generates a CSV representation of all data from the given [AdminJdbcTable].
     * Retrieves all rows from the table and converts them into a CSV format,
     * where columns are separated by commas and rows are separated by new lines.
     * If a column value is null, it is replaced with "N/A".
     *
     * @param table The database table to retrieve data from.
     * @return A string containing the CSV representation of the table data.
     */
    fun getAllDataAsCsvFile(table: AdminJdbcTable): String {
        return table.usingDataSource { session ->
            session.list(
                sqlQuery(table.createGetAllDataAsCsvQuery())
            ) { row ->
                table.getAllAllowToShowColumns().joinToString(", ") {
                    row.anyOrNull(it.columnName)?.restore(it)?.toString() ?: "N/A"
                }
            }.joinToString("\n")
        }
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
        val columns = table.getAllColumns()
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
                            val column = columns.first { it.columnName == field.fieldName }
                            if (aggregationFunction == ChartDashboardAggregationFunction.COUNT) {
                                rs.getInt(
                                    getFieldNameBasedOnAggregationFunction(
                                        aggregationFunction,
                                        field.fieldName
                                    )
                                ).toDouble()
                            } else {
                                rs.getTypedValue(
                                    column.type,
                                    getFieldNameBasedOnAggregationFunction(
                                        aggregationFunction,
                                        field.fieldName
                                    )
                                ).restore(column)?.toString()?.toDoubleOrNull() ?: 0.0
                            }
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
        val columns = table.getAllColumns()
        val column = columns.first { it.columnName == section.fieldName }
        return table.usingDataSource { session ->
            session.prepare(sqlQuery(section.createGetAllData())).use { prepareStatement ->
                prepareStatement.executeQuery().use { rs ->
                    var value = ""
                    value = when (section.aggregationFunction) {
                        TextDashboardAggregationFunction.LAST_ITEM -> {
                            if (rs.next()) {
                                val itemObject = rs.getTypedValue(column.type, section.fieldName)
                                itemObject?.toString().restore(column)?.toDoubleOrNull()?.formatAsIntegerIfPossible()
                                    ?.toString()
                                    ?: itemObject.toString()
                            } else ""
                        }

                        TextDashboardAggregationFunction.PROFIT_PERCENTAGE -> {
                            var nextItem = 0.0
                            var previewsItem = 0.0
                            if (rs.next()) {
                                nextItem = rs.getTypedValue(
                                    column.type, section.fieldName
                                ).restore(column).toString().toDoubleOrNull() ?: 0.0
                            }
                            if (rs.next()) {
                                previewsItem = rs.getTypedValue(
                                    column.type, section.fieldName
                                ).restore(column).toString().toDoubleOrNull() ?: 0.0
                            }
                            runCatching { ((nextItem - previewsItem).div(previewsItem) * 100).formatAsIntegerIfPossible() }.getOrNull()
                                .toString() + "%"
                        }

                        TextDashboardAggregationFunction.COUNT -> {
                            if (rs.next()) {
                                rs.getInt("aggregationFunctionValue").toString()
                            } else ""
                        }

                        else -> {
                            if (rs.next()) {
                                rs.getTypedValue(column.type, "aggregationFunctionValue").restore(column).let {
                                    it.toString().toDoubleOrNull()?.formatAsIntegerIfPossible() ?: it.toString()
                                }
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


    /**
     * Checks if a given value already exists in the specified column of the table.
     *
     * @param table The table to check in.
     * @param column The column to check for duplicate values.
     * @param value The value to check for existence.
     * @param primaryKey The primary key value to exclude from the check (optional).
     * @return `true` if the value exists, otherwise `false`.
     */
    fun checkExistSameData(
        table: AdminJdbcTable,
        column: ColumnSet,
        value: Any?,
        primaryKey: String? = null
    ): Boolean {
        return table.usingDataSource { session ->
            session.prepare(
                sqlQuery(table.createExistsColumnQuery(column.columnName, primaryKey))
            ).use { preparedStatement ->
                preparedStatement.putColumn(column.type, value, 1)

                primaryKey?.let {
                    val type = table.getPrimaryKeyColumn().type
                    preparedStatement.putColumn(type, it.toTypedValue(type), 2)
                }

                preparedStatement.executeQuery().use { rs ->
                    rs.next() && rs.getBoolean(1)
                }
            }
        }
    }

    /**
     * Generates an SQL query to check if a specific value exists in a column.
     *
     * @param columnName The name of the column to check.
     * @param primaryKey The primary key column name (optional, used to exclude the current record from duplication check).
     * @return A SQL query string formatted for checking existence.
     */
    private fun AdminJdbcTable.createExistsColumnQuery(columnName: String, primaryKey: String?) = buildString {
        append("SELECT EXISTS (SELECT 1 FROM ")
        append(getTableName())
        append(" WHERE ")
        append(columnName)
        append(" = ?")

        if (primaryKey != null) {
            append(" AND ")
            append(getPrimaryKey())
            append(" != ?")
        }

        append(")")
    }


    /**
     * Retrieves a list of data for the given dashboard section from the specified table.
     * It filters the columns based on the section's field settings and fetches the corresponding rows.
     * @param table The AdminJdbcTable from which data is retrieved.
     * @param section The ListDashboardSection containing query settings.
     * @return A ListData object containing the fetched rows and field names.
     */
    fun getListSectionData(table: AdminJdbcTable, section: ListDashboardSection): ListData {
        val tableColumns = table.getAllAllowToShowColumns()
        val allColumns = table.getAllColumns()
        val columns = section.fields?.mapNotNull { fieldName ->
            allColumns.firstOrNull { it.columnName == fieldName }
        } ?: tableColumns
        val primaryKeyColumn = table.getPrimaryKey()
        val rows = mutableListOf<DataWithPrimaryKey>()

        table.usingDataSource { session ->
            session.prepare(sqlQuery(section.createGetDataQuery(columns, primaryKeyColumn)))
                .use { preparedStatement ->
                    section.limitCount?.let { preparedStatement.setInt(1, it) }
                    preparedStatement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            val primaryKey = resultSet.getObject(primaryKeyColumn)?.toString() ?: "N/A"
                            val data = columns.map { column ->
                                resultSet.getTypedValue(column.type, column.columnName)
                                    .restore(column)
                                    ?.formatToDisplayInTable(column.type)
                                    ?: "N/A"
                            }
                            rows.add(
                                DataWithPrimaryKey(
                                    primaryKey = primaryKey, data = data
                                )
                            )
                        }
                    }
                }
        }

        return ListData(
            section = section,
            values = rows,
            pluralName = table.getPluralName(),
            fields = columns.map {
                FieldData(
                    name = it.verboseName,
                    type = it.type.name,
                    fieldName = it.columnName
                )
            }
        )
    }

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

    /**
     * Creates a SQL query to select all data from the table.
     *
     * @receiver The [AdminJdbcTable] instance.
     * @return A SQL query string to fetch all rows from the table.
     */
    fun AdminJdbcTable.createGetAllDataAsCsvQuery() = "SELECT * FROM ${getTableName()}"

    // endregion

    // region Reference Data

    /**
     * Retrieves reference data for a specific column.
     * @param table Target database table
     * @return List of display items
     */
    fun getAllReferences(
        table: AdminJdbcTable,
    ): List<DisplayItem> {
        return table.usingDataSource { session ->
            session.list(sqlQuery(table.createGetAllReferencesQuery())) { raw ->
                val referenceKey =
                    raw.any("${table.getTableName()}_${table.getPrimaryKey()}").toString()
                val displayFormat = table.getDisplayFormat()
                DisplayItem(
                    itemKey = referenceKey,
                    item = displayFormat?.let {
                        val displayFormatValues = it.extractTextInCurlyBraces()
                        populateTemplate(
                            it,
                            displayFormatValues.associateWith { item ->
                                if (item == table.getPrimaryKey()) {
                                    referenceKey
                                } else {
                                    val splitItem = item.split(".")
                                    val columnSet =
                                        table.getAllColumns()
                                            .firstOrNull { it.columnName == splitItem.last() }
                                    raw.anyOrNull(
                                        splitItem.joinToString(separator = "_")
                                    ).let {
                                        if (columnSet == null) it?.toString() else it.restore(columnSet)
                                            ?.toString()
                                    }
                                }
                            })
                    } ?: "${
                        table.getSingularName().replaceFirstChar { it.uppercaseChar() }
                    } Object ($referenceKey)"
                )
            }
        }
    }


    /**
     * Checks if data has been changed compared to initial value.
     */
    private fun checkIsChangedData(
        columnSet: ColumnSet,
        initialValue: String?,
        currentValue: String?,
    ): Boolean =
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
                            rs.getTypedValue(column.type, column.columnName)?.restore(column)
                                ?.formatToDisplayInUpsert(column.type)
                        }
                    }
                    return@usingDataSource null
                }
            }
        }


    /**
     * Retrieves all related primary keys from a Many-to-Many reference.
     *
     * This function executes a query to fetch the values of `rightPrimaryKey` from the join table,
     * where the `leftPrimaryKey` matches the given primary key.
     *
     * @param table The table containing the reference.
     * @param columnSet The column set containing the Many-to-Many reference.
     * @param primaryKey The primary key value used to filter the results.
     * @return A list of primary key values from the related table.
     */
    fun getAllSelectedReferenceInListReference(
        table: AdminJdbcTable,
        columnSet: ColumnSet,
        primaryKey: String
    ): List<Any> {
        val reference = columnSet.reference as Reference.ManyToMany
        val primaryKeys = mutableListOf<Any>()

        table.usingDataSource { session ->
            session.prepare(sqlQuery(table.createSelectedReferenceInListReference(reference)))
                .use { preparedStatement ->
                    val primaryKeyColumn = table.getPrimaryKeyColumn()
                    val typedPrimaryKey = primaryKey.toTypedValue(primaryKeyColumn.type)

                    preparedStatement.putColumn(primaryKeyColumn.type, typedPrimaryKey, 1)

                    preparedStatement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            resultSet.getObject(reference.rightPrimaryKey)?.let(primaryKeys::add)
                        }
                    }
                }
        }
        return primaryKeys
    }

    /**
     * Generates the SQL query for selecting the related primary keys
     * from the join table in a Many-to-Many relationship.
     */
    private fun AdminJdbcTable.createSelectedReferenceInListReference(reference: Reference.ManyToMany) = buildString {
        append("SELECT ${reference.rightPrimaryKey} FROM ")
        append(reference.joinTable)
        append(" WHERE ")
        append(reference.leftPrimaryKey)
        append(" = ?")
    }

    /**
     * Updates a many-to-many relationship by:
     * 1. Deleting old relations that are not in `newIds`.
     * 2. If `newIds` is empty, it removes all relations for the given `primaryKey`.
     * 3. Inserting new relations only if `newIds` is not empty and not already present.
     */
    fun updateSelectedReferenceInListReference(
        table: AdminJdbcTable,
        joinTable: AdminJdbcTable,
        columnSet: ColumnSet,
        primaryKey: String,
        newIds: List<String>
    ) {
        val reference = columnSet.reference as Reference.ManyToMany

        joinTable.usingDataSource { session ->
            session.prepare(sqlQuery(createUpdateReferenceQuery(reference, newIds))).use { preparedStatement ->
                val primaryKeyColumn = table.getPrimaryKeyColumn()
                val typedPrimaryKey = primaryKey.toTypedValue(primaryKeyColumn.type)

                var index = 1
                preparedStatement.putColumn(primaryKeyColumn.type, typedPrimaryKey, index++)

                val idColumnSet = joinTable.getAllColumns().firstOrNull { it.columnName == reference.rightPrimaryKey }
                if (idColumnSet == null) {
                    throw IllegalStateException("Column '${reference.rightPrimaryKey}' not found in the join table.")
                }

                if (newIds.isNotEmpty()) {
                    newIds.forEach { id ->
                        preparedStatement.putColumn(
                            idColumnSet.type,
                            id.toTypedValue(idColumnSet.type),
                            index++
                        )
                    }
                    preparedStatement.putColumn(primaryKeyColumn.type, typedPrimaryKey, index++)
                    newIds.forEach { id ->
                        preparedStatement.putColumn(
                            idColumnSet.type,
                            id.toTypedValue(idColumnSet.type),
                            index++
                        )
                    }
                    preparedStatement.putColumn(primaryKeyColumn.type, typedPrimaryKey, index)
                }

                preparedStatement.executeUpdate()
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
                        preparedStatement.putColumn(columnSet.type, parameters[index].map(columnSet), index + 1)
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
                                    item.first.type, item.second?.second.map(item.first), index + 1
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
                    val currentReferenceColumn = columnSet?.reference?.foreignKey

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
                val currentReferenceColumn = columnSet?.reference?.foreignKey

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
    private fun AdminJdbcTable.createGetAllReferencesQuery(): String {
        return createBasicReference()
    }

    private fun AdminJdbcTable.createBasicReference(): String {
        val columns = getDisplayFormat()?.extractTextInCurlyBraces().orEmpty()
        val selectColumns = mutableSetOf<String>()
        val joins = mutableListOf<String>()


        val primaryKey = getPrimaryKey()
        selectColumns.add("${getTableName()}.$primaryKey AS ${getTableName()}_$primaryKey")

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
                        when (reference) {
                            is Reference.OneToOne -> {
                                val joinColumn = reference.foreignKey
                                currentColumn = nextColumn ?: referenceColumn

                                joins.add("LEFT JOIN $joinTable ON $currentTable.$referenceColumn = $joinTable.$joinColumn")
                            }

                            is Reference.ManyToOne -> {
                                val joinColumn = reference.foreignKey
                                currentColumn = nextColumn ?: referenceColumn

                                joins.add("LEFT JOIN $joinTable ON $currentTable.$referenceColumn = $joinTable.$joinColumn")
                            }

                            else -> Unit
                        }
                        currentTable = joinTable
                    } else if (i == path.lastIndex) {
                        currentColumn = referenceColumn
                    }
                }

                if (currentColumn.isNotEmpty()) {
                    selectColumns.add("$currentTable.$currentColumn AS ${path.joinToString("_")}")
                }
            } else {
                if (column != primaryKey && column != getPrimaryKey()) {
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
     * Generates an SQL query string to fetch data for the given dashboard section.
     * It includes selected columns, the primary key, and optional ordering and limiting clauses.
     * @param columns The list of ColumnSet objects representing selected table columns.
     * @param primaryKey The primary key column to ensure uniqueness in selection.
     * @return A formatted SQL query string.
     */
    fun ListDashboardSection.createGetDataQuery(columns: List<ColumnSet>, primaryKey: String): String = buildString {
        val orderField = orderQuery?.substringBeforeLast(" ")?.trim()
        append("SELECT ")

        val columnNames = columns.map { it.columnName }.plus(orderField).plus(primaryKey).filterNotNull().distinct()
        append(columnNames.joinToString(", "))

        append(" FROM ").append(tableName)

        orderQuery?.let { append(" ORDER BY ").append(it) }
        limitCount?.let { append(" LIMIT ?") }
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

    private fun createUpdateReferenceQuery(reference: Reference.ManyToMany, newIds: List<String>) =
        buildString {
            append("DELETE FROM ${reference.joinTable} WHERE ${reference.leftPrimaryKey} = ?")
            if (newIds.isNotEmpty()) {
                append(" AND ${reference.rightPrimaryKey} NOT IN (${newIds.joinToString { "?" }});")
            } else {
                append(";")
            }

            if (newIds.isNotEmpty()) {
                append("INSERT INTO ${reference.joinTable} (${reference.leftPrimaryKey}, ${reference.rightPrimaryKey}) ")
                append("SELECT ?, new_ids.${reference.rightPrimaryKey} FROM (VALUES ")
                append(newIds.joinToString { "(?)" })
                append(") AS new_ids(${reference.rightPrimaryKey}) ")
                append("WHERE NOT EXISTS (SELECT 1 FROM ${reference.joinTable} ")
                append("WHERE ${reference.leftPrimaryKey} = ? AND ${reference.rightPrimaryKey} = new_ids.${reference.rightPrimaryKey});")
            }
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