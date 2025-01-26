package filters

import io.ktor.http.*
import models.ColumnSet
import models.filters.FilterTypes
import models.filters.FiltersData
import models.types.ColumnType
import panels.AdminJdbcTable
import repository.JdbcQueriesRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object JdbcFilters {
    fun findFiltersData(
        table: AdminJdbcTable,
        jdbcTables: List<AdminJdbcTable>
    ): List<FiltersData> {
        return table.getFilterColumns().map { filterColumn ->
            val filterTable = if (filterColumn.contains(".")) {
                val paths = filterColumn.split(".")
                jdbcTables.find { it.getTableName() == paths[paths.size - 2] }
            } else table

            val columnSet = if (filterColumn.contains(".")) {
                val paths = filterColumn.split(".")
                filterTable?.getAllColumns()?.find { it.columnName == paths.last() }
            } else filterTable?.getAllColumns()?.find { it.columnName == filterColumn }

            when {
                columnSet?.type == ColumnType.DATE -> FiltersData(
                    paramName = columnSet.columnName,
                    type = FilterTypes.DATE
                )

                columnSet?.type == ColumnType.DATETIME -> FiltersData(
                    paramName = columnSet.columnName,
                    type = FilterTypes.DATETIME
                )

                columnSet?.type == ColumnType.BOOLEAN -> FiltersData(
                    paramName = columnSet.columnName,
                    type = FilterTypes.BOOLEAN
                )

                columnSet?.type == ColumnType.ENUMERATION -> FiltersData(
                    paramName = columnSet.columnName,
                    type = FilterTypes.ENUMERATION,
                    values = columnSet.enumerationValues
                )

                columnSet?.reference != null -> {
                    val referenceTable = jdbcTables.find {
                        it.getTableName() == columnSet.reference.tableName
                    }
                        ?: throw IllegalArgumentException("Reference table not found for ${columnSet.reference.tableName}")

                    FiltersData(
                        paramName = columnSet.columnName,
                        type = FilterTypes.REFERENCE,
                        values = JdbcQueriesRepository.getAllReferences(referenceTable, columnSet.reference.columnName)
                    )
                }

                else -> throw IllegalArgumentException("Filters are currently supported only for types: DATE, DATETIME, ENUMERATION, and REFERENCE")
            }
        }
    }

    fun extractFilters(
        table: AdminJdbcTable,
        jdbcTables: List<AdminJdbcTable>,
        parameters: Parameters
    ): MutableList<Pair<ColumnSet, String>> {
        val filters = mutableListOf<Pair<ColumnSet, String>>()

        table.getFilterColumns().forEach { filterColumn ->
            val filterTable = if (filterColumn.contains(".")) {
                val paths = filterColumn.split(".")
                jdbcTables.find { it.getTableName() == paths[paths.size - 2] }
            } else table

            val columnSet = if (filterColumn.contains(".")) {
                val paths = filterColumn.split(".")
                filterTable?.getAllColumns()?.find { it.columnName == paths.last() }
            } else filterTable?.getAllColumns()?.find { it.columnName == filterColumn }

            when {
                columnSet?.type == ColumnType.DATE || columnSet?.type == ColumnType.DATETIME -> {
                    handleDateTimeFilter(columnSet, parameters, filters)
                }

                columnSet?.type == ColumnType.ENUMERATION -> {
                    parameters[columnSet.columnName]?.let { value ->
                        filters.add(Pair(columnSet, "= '$value'"))
                    }
                }

                columnSet?.reference != null -> {
                    parameters[columnSet.columnName]?.let { refValue ->
                        filters.add(Pair(columnSet, "= '$refValue'"))
                    }
                }
            }
        }

        return filters
    }

    private fun handleDateTimeFilter(
        columnSet: ColumnSet?,
        parameters: Parameters,
        filters: MutableList<Pair<ColumnSet, String>>
    ) {
        if (columnSet == null) return

        val startParamName = "${columnSet.columnName}-start"
        val endParamName = "${columnSet.columnName}-end"

        if (parameters.contains(startParamName)) {
            parameters[startParamName]?.let { startValue ->
                val startTimestamp = Instant.ofEpochMilli(startValue.toLong())
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                filters.add(Pair(columnSet, ">= '$startTimestamp'"))
            }
        }

        if (parameters.contains(endParamName)) {
            parameters[endParamName]?.let { endValue ->
                val endTimestamp = Instant.ofEpochMilli(endValue.toLong())
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                filters.add(Pair(columnSet, "<= '$endTimestamp'"))
            }
        }
    }
}