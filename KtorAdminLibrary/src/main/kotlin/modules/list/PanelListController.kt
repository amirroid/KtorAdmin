package modules.list

import utils.badRequest
import utils.notFound
import configuration.DynamicConfiguration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import models.ColumnSet
import models.filters.FilterTypes
import models.filters.FiltersData
import models.types.ColumnType
import panels.*
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import utils.Constants
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal suspend fun ApplicationCall.handlePanelList(tables: List<AdminPanel>) {
    val pluralName = parameters["pluralName"]
    val searchParameter = parameters["search"]?.takeIf { it.isNotEmpty() }
    val currentPage = runCatching {
        parameters["page"]?.toInt()?.minus(1) ?: 0
    }.onFailure { cause ->
        badRequest(cause.message ?: "")
    }.getOrNull()

    val panel = tables.find { it.getPluralName() == pluralName }
    if (panel == null) {
        notFound("No table found with plural name: $pluralName")
    } else {
        when (panel) {
            is AdminJdbcTable -> handleJdbcList(panel, tables, searchParameter, currentPage, pluralName, parameters)
            is AdminMongoCollection -> handleNoSqlList(
                panel, pluralName, currentPage
            )
        }
    }
}

private suspend fun ApplicationCall.handleJdbcList(
    table: AdminJdbcTable,
    tables: List<AdminPanel>,
    searchParameter: String?,
    currentPage: Int?,
    pluralName: String?,
    parameters: Parameters
) {
    val jdbcTables = tables.filterIsInstance<AdminJdbcTable>()
    val hasSearchColumn = table.getSearchColumns().isNotEmpty()

    // Prepare filters data
    val filtersData = findFiltersData(table, jdbcTables)

    // Extract actual filters
    val filters = extractFilters(table, jdbcTables, parameters)

    // Fetch data
    val data = JdbcQueriesRepository.getAllData(table, searchParameter, currentPage, filters)
    val maxPages = JdbcQueriesRepository.getCount(table, searchParameter, filters).let {
        val calculatedValue = it / DynamicConfiguration.maxItemsInPage
        if (it % DynamicConfiguration.maxItemsInPage == 0) {
            calculatedValue
        } else calculatedValue.plus(1)
    }

    // Respond with Velocity template
    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/table_list.vm",
            model = mapOf(
                "columnNames" to table.getAllAllowToShowColumns().map { it.columnName },
                "rows" to data,
                "pluralName" to pluralName.orEmpty().replaceFirstChar { it.uppercaseChar() },
                "hasSearchColumn" to hasSearchColumn,
                "currentPage" to (currentPage?.plus(1) ?: 1),
                "maxPages" to maxPages,
                "filtersData" to filtersData
            )
        )
    )
}

private suspend fun ApplicationCall.handleNoSqlList(
    panel: AdminMongoCollection,
    pluralName: String?,
    currentPage: Int?,
) {
    // Fetch data
    val data = MongoClientRepository.getAllData(panel)

    // Respond with Velocity template
    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/table_list.vm",
            model = mapOf(
                "columnNames" to panel.getAllAllowToShowFields().map { it.fieldName },
                "rows" to data,
                "pluralName" to pluralName?.replaceFirstChar { it.uppercaseChar() }.orEmpty(),
                "hasSearchColumn" to false,
                "currentPage" to (currentPage?.plus(1) ?: 1),
                "maxPages" to 1,
                "filtersData" to emptyList<FiltersData>()
            )
        )
    )
}

private fun findFiltersData(
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

            columnSet?.type == ColumnType.ENUMERATION -> FiltersData(
                paramName = columnSet.columnName,
                type = FilterTypes.ENUMERATION,
                values = columnSet.enumerationValues
            )

            columnSet?.reference != null -> {
                val referenceTable = jdbcTables.find {
                    it.getTableName() == columnSet.reference.tableName
                } ?: throw IllegalArgumentException("Reference table not found for ${columnSet.reference.tableName}")

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

private fun extractFilters(
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