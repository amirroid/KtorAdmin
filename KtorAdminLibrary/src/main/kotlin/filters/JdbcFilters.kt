package filters

import configuration.DynamicConfiguration
import getters.toTypedValue
import io.ktor.http.Parameters
import models.ColumnSet
import models.common.Reference
import models.common.tableName
import models.filters.FilterTypes
import models.filters.FiltersData
import models.types.ColumnType
import panels.AdminJdbcTable
import repository.JdbcQueriesRepository
import utils.Constants
import java.time.Instant
import java.time.ZoneId

/**
 * Handles JDBC filtering operations for database queries.
 * Provides functionality to create filter data and extract filter conditions.
 */
internal object JdbcFilters {
    /**
     * Creates filter data definitions for a given table and its related tables.
     *
     * @param table The main table to create filters for
     * @param jdbcTables List of all available JDBC tables
     * @return List of filter data definitions
     */
    fun findFiltersData(
        table: AdminJdbcTable,
        jdbcTables: List<AdminJdbcTable>
    ): List<FiltersData> {
        return table.getFilters().map { filterColumn ->
            // Parse the filter column and find corresponding table
            val (_, columnSet) = resolveTableAndColumn(filterColumn, table, jdbcTables)

            createFilterData(columnSet, jdbcTables)
        }
    }

    /**
     * Extracts filter conditions from request parameters.
     *
     * @param table The main table to extract filters for
     * @param jdbcTables List of all available JDBC tables
     * @param parameters HTTP request parameters containing filter values
     * @return List of filter conditions as Triple(Column, Operator, Value)
     */
    fun extractFilters(
        table: AdminJdbcTable,
        jdbcTables: List<AdminJdbcTable>,
        parameters: Parameters
    ): MutableList<Triple<ColumnSet, String, Any?>> {
        val filters = mutableListOf<Triple<ColumnSet, String, Any?>>()

        table.getFilters().forEach { filterColumn ->
            val (_, columnSet) = resolveTableAndColumn(filterColumn, table, jdbcTables)

            when {
                isDateTimeColumn(columnSet) -> handleDateTimeFilter(columnSet, parameters, filters)
                isBooleanOrEnumColumn(columnSet) -> handleSimpleFilter(columnSet!!, parameters, filters)
                isReferenceColumn(columnSet) -> handleReferenceFilter(columnSet!!, parameters, filters)
            }
        }

        return filters
    }

    /**
     * Resolves the table and column for a given filter column path.
     */
    private fun resolveTableAndColumn(
        filterColumn: String,
        table: AdminJdbcTable,
        jdbcTables: List<AdminJdbcTable>
    ): Pair<AdminJdbcTable?, ColumnSet?> {
        val isNestedColumn = filterColumn.contains(".")

        val filterTable = if (isNestedColumn) {
            val paths = filterColumn.split(".")
            jdbcTables.find { it.getTableName() == paths[paths.size - 2] }
        } else table

        val columnSet = if (isNestedColumn) {
            val paths = filterColumn.split(".")
            filterTable?.getAllColumns()?.find { it.columnName == paths.last() }
        } else filterTable?.getAllColumns()?.find { it.columnName == filterColumn }

        return Pair(filterTable, columnSet)
    }

    /**
     * Creates appropriate filter data based on column type.
     */
    private fun createFilterData(
        columnSet: ColumnSet?,
        jdbcTables: List<AdminJdbcTable>
    ): FiltersData {
        return when {
            columnSet?.type == ColumnType.DATE -> createDateFilter(columnSet)
            columnSet?.type == ColumnType.DATETIME -> createDateTimeFilter(columnSet)
            columnSet?.type == ColumnType.TIMESTAMP_WITH_TIMEZONE -> createDateTimeFilter(columnSet)
            columnSet?.type == ColumnType.BOOLEAN -> createBooleanFilter(columnSet)
            columnSet?.type == ColumnType.ENUMERATION -> createEnumerationFilter(columnSet)
            isReferenceColumn(columnSet) -> createReferenceFilter(columnSet!!, jdbcTables)
            else -> throw IllegalArgumentException(
                "Filters are currently supported only for types: DATE, DATETIME, ENUMERATION, and REFERENCE"
            )
        }
    }

    private fun createDateFilter(columnSet: ColumnSet) = FiltersData(
        paramName = columnSet.columnName,
        verboseName = columnSet.verboseName,
        type = FilterTypes.DATE
    )

    private fun createDateTimeFilter(columnSet: ColumnSet) = FiltersData(
        paramName = columnSet.columnName,
        verboseName = columnSet.verboseName,
        type = FilterTypes.DATETIME
    )

    private fun createBooleanFilter(columnSet: ColumnSet) = FiltersData(
        paramName = columnSet.columnName,
        verboseName = columnSet.verboseName,
        type = FilterTypes.BOOLEAN
    )

    private fun createEnumerationFilter(columnSet: ColumnSet) = FiltersData(
        paramName = columnSet.columnName,
        verboseName = columnSet.verboseName,
        type = FilterTypes.ENUMERATION,
        values = columnSet.enumerationValues
    )

    private fun createReferenceFilter(
        columnSet: ColumnSet,
        jdbcTables: List<AdminJdbcTable>
    ): FiltersData {
        val referenceTable = jdbcTables.find { it.getTableName() == columnSet.reference?.tableName }
            ?: throw IllegalArgumentException("Reference table not found for ${columnSet.reference?.tableName}")

        return FiltersData(
            paramName = columnSet.columnName,
            verboseName = columnSet.verboseName,
            type = FilterTypes.REFERENCE,
            values = JdbcQueriesRepository.getAllReferences(referenceTable)
        )
    }

    /**
     * Handles datetime range filter extraction.
     */
    private fun handleDateTimeFilter(
        columnSet: ColumnSet?,
        parameters: Parameters,
        filters: MutableList<Triple<ColumnSet, String, Any?>>
    ) {
        if (columnSet == null) return

        handleDateTimeRange(columnSet, parameters, filters, "-start", ">=")
        handleDateTimeRange(columnSet, parameters, filters, "-end", "<=")
    }

    private fun handleDateTimeRange(
        columnSet: ColumnSet,
        parameters: Parameters,
        filters: MutableList<Triple<ColumnSet, String, Any?>>,
        suffix: String,
        operator: String
    ) {
        val paramName = "${columnSet.columnName}$suffix"

        if (parameters.contains(Constants.FILTERS_PREFIX + paramName)) {
            parameters[Constants.FILTERS_PREFIX + paramName]?.let { value ->
                val timestamp = Instant.ofEpochMilli(value.toLong())
                    .atZone(DynamicConfiguration.timeZone)

                val convertedValue = when (columnSet.type) {
                    ColumnType.DATETIME -> timestamp.toLocalDateTime()
                    ColumnType.DATE -> timestamp.toLocalDate()
                    ColumnType.TIMESTAMP_WITH_TIMEZONE -> timestamp.toOffsetDateTime()
                    else -> return@let
                }

                filters.add(Triple(columnSet, operator, convertedValue))
            }
        }
    }

    /**
     * Handles simple equality filters for boolean and enum types.
     */
    private fun handleSimpleFilter(
        columnSet: ColumnSet,
        parameters: Parameters,
        filters: MutableList<Triple<ColumnSet, String, Any?>>
    ) {
        parameters[Constants.FILTERS_PREFIX + columnSet.columnName]?.let { value ->
            filters.add(Triple(columnSet, "= ", value.toTypedValue(columnSet.type)))
        }
    }

    /**
     * Handles reference type filters.
     */
    private fun handleReferenceFilter(
        columnSet: ColumnSet,
        parameters: Parameters,
        filters: MutableList<Triple<ColumnSet, String, Any?>>
    ) {
        parameters[Constants.FILTERS_PREFIX + columnSet.columnName]?.let { refValue ->
            filters.add(Triple(columnSet, "= ", refValue.toTypedValue(columnSet.type)))
        }
    }

    // Helper functions for type checking
    private fun isDateTimeColumn(columnSet: ColumnSet?) =
        columnSet?.type == ColumnType.DATE || columnSet?.type == ColumnType.DATETIME || columnSet?.type == ColumnType.TIMESTAMP_WITH_TIMEZONE

    private fun isBooleanOrEnumColumn(columnSet: ColumnSet?) =
        columnSet?.type == ColumnType.BOOLEAN || columnSet?.type == ColumnType.ENUMERATION

    private fun isReferenceColumn(columnSet: ColumnSet?) =
        columnSet?.reference is Reference.OneToOne || columnSet?.reference is Reference.ManyToOne
}