package modules.list

import utils.badRequest
import utils.notFound
import configuration.DynamicConfiguration
import filters.JdbcFilters
import filters.MongoFilters
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
    val filtersData = JdbcFilters.findFiltersData(table, jdbcTables)

    // Extract actual filters
    val filters = JdbcFilters.extractFilters(table, jdbcTables, parameters)

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
    // Prepare filters data
    val filtersData = MongoFilters.findFiltersData(panel)


    // Fetch data
    val data = MongoClientRepository.getAllData(
        panel,
        (currentPage ?: 1) - 1,
        filters = MongoFilters.extractMongoFilters(panel, parameters)
    )

    val maxPages = MongoClientRepository.getTotalPages(panel)

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
                "maxPages" to maxPages,
                "filtersData" to filtersData
            )
        )
    )
}

