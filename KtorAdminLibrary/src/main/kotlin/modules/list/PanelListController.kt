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
import repository.JdbcQueriesRepository
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import panels.getAllAllowToShowColumns
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
            is AdminMongoCollection -> respondText { "Coming soon..." }
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
    val filtersData = table.getFilterColumns().map {
        val filterTable = if (it.contains(".")) it.split(".").let { paths ->
            jdbcTables.find { table -> table.getTableName() == paths[paths.size - 2] }
        } else table
        val columnSet = if (it.contains(".")) it.split(".").let { paths ->
            filterTable?.getAllColumns()
                ?.find { column -> column.columnName == paths.last() }
        } else filterTable?.getAllColumns()?.find { column -> column.columnName == it }
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
                val referenceTable = jdbcTables.find { table -> table.getTableName() == columnSet.reference.tableName }
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
    val filters = mutableListOf<Pair<ColumnSet, String>>()
    table.getFilterColumns().forEach {
        val filterTable = if (it.contains(".")) it.split(".").let { paths ->
            jdbcTables.find { table -> table.getTableName() == paths[paths.size - 2] }
        } else table

        val columnSet = if (it.contains(".")) it.split(".").let { paths ->
            filterTable?.getAllColumns()
                ?.find { column -> column.columnName == paths.last() }
        } else filterTable?.getAllColumns()?.find { column -> column.columnName == it }

        when {
            columnSet?.type == ColumnType.DATE -> {
                if (parameters.contains("${columnSet.columnName}-start")) {
                    val startDate = parameters["${columnSet.columnName}-start"]
                    startDate?.let {
                        val startTimestamp = Instant.ofEpochMilli(it.toLong()).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        filters.add(Pair(columnSet, ">= '$startTimestamp'"))
                    }
                }
                if (parameters.contains("${columnSet.columnName}-end")) {
                    val endDate = parameters["${columnSet.columnName}-end"]
                    endDate?.let {
                        val endTimestamp = Instant.ofEpochMilli(it.toLong()).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        filters.add(Pair(columnSet, "<= '$endTimestamp'"))
                    }
                }
            }

            columnSet?.type == ColumnType.DATETIME -> {
                if (parameters.contains("${columnSet.columnName}-start")) {
                    val startDateTime = parameters["${columnSet.columnName}-start"]
                    startDateTime?.let {
                        val startTimestamp = Instant.ofEpochMilli(it.toLong()).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        filters.add(Pair(columnSet, ">= '$startTimestamp'"))
                    }
                }
                if (parameters.contains("${columnSet.columnName}-end")) {
                    val endDateTime = parameters["${columnSet.columnName}-end"]
                    endDateTime?.let {
                        val endTimestamp = Instant.ofEpochMilli(it.toLong()).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        filters.add(Pair(columnSet, "<= '$endTimestamp'"))
                    }
                }
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

    println("Filters: $filters")

    val data = JdbcQueriesRepository.getAllData(table, searchParameter, currentPage, filters)
    val maxPages = JdbcQueriesRepository.getCount(table, searchParameter, filters).let {
        val calculatedValue = it / DynamicConfiguration.maxItemsInPage
        if (it % DynamicConfiguration.maxItemsInPage == 0) {
            calculatedValue
        } else calculatedValue.plus(1)
    }

    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/table_list.vm", model = mapOf(
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