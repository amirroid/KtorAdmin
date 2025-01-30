package modules.list

import com.mongodb.client.model.Filters
import utils.badRequest
import utils.notFound
import configuration.DynamicConfiguration
import filters.JdbcFilters
import filters.MongoFilters
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import models.order.Order
import panels.*
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import utils.Constants
import utils.serverError
import validators.checkHasRole

internal suspend fun ApplicationCall.handlePanelList(tables: List<AdminPanel>) {
    val pluralName = parameters["pluralName"]
    val searchParameter = parameters["search"]?.takeIf { it.isNotEmpty() }
    val currentPage = runCatching {
        parameters["page"]?.toInt()?.minus(1) ?: 0
    }.onFailure { cause ->
        badRequest(cause.message ?: "", cause)
    }.getOrNull()

    val panel = tables.find { it.getPluralName() == pluralName }
    if (panel == null) {
        notFound("No table found with plural name: $pluralName")
    } else {
        checkHasRole(panel) {
            kotlin.runCatching {
                when (panel) {
                    is AdminJdbcTable -> handleJdbcList(
                        panel,
                        tables,
                        searchParameter,
                        currentPage,
                        pluralName,
                        parameters
                    )

                    is AdminMongoCollection -> handleNoSqlList(
                        panel, pluralName, currentPage, searchParameter
                    )
                }
            }.onFailure {
                serverError(it.message ?: "", it)
            }
        }
    }
}

private suspend fun ApplicationCall.getColumnOrder(table: AdminJdbcTable): Order? {
    val orderDirection = parameters["orderDirection"]?.takeIf { it.isNotEmpty() } ?: "ASC"
    if (orderDirection.lowercase() !in listOf("asc", "desc")) {
        badRequest("Invalid order direction '$orderDirection'. Valid values are 'asc' or 'desc'.")
    }
    return parameters["order"]?.takeIf { it.isNotEmpty() }?.let {
        if (it !in table.getAllColumns().map { column -> column.columnName }) {
            badRequest("The column '$it' specified in the order does not exist in the table. Please provide a valid column name for ordering.")
        }
        Order(it, orderDirection)
    } ?: table.getDefaultOrder()
}

private suspend fun ApplicationCall.getFieldOrder(table: AdminMongoCollection): Order? {
    val orderDirection = parameters["orderDirection"]?.takeIf { it.isNotEmpty() } ?: "ASC"
    if (orderDirection.lowercase() !in listOf("asc", "desc")) {
        badRequest("Invalid order direction '$orderDirection'. Valid values are 'asc' or 'desc'.")
    }
    return parameters["order"]?.takeIf { it.isNotEmpty() }?.let {
        if (it !in table.getAllFields().map { field -> field.fieldName }) {
            badRequest("The column '$it' specified in the order does not exist in the collection. Please provide a valid field name for ordering.")
        }
        Order(it, orderDirection)
    } ?: table.getDefaultOrder()
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
    val hasSearchColumn = table.getSearches().isNotEmpty()

    val order = getColumnOrder(table)

    // Prepare filters data
    val filtersData = JdbcFilters.findFiltersData(table, jdbcTables)

    // Extract actual filters
    val filters = JdbcFilters.extractFilters(table, jdbcTables, parameters)

    // Fetch data
    val data = JdbcQueriesRepository.getAllData(table, searchParameter, currentPage, filters, order)
    val maxPages = JdbcQueriesRepository.getCount(table, searchParameter, filters).let {
        val calculatedValue = it / DynamicConfiguration.maxItemsInPage
        if (it % DynamicConfiguration.maxItemsInPage == 0) {
            calculatedValue
        } else calculatedValue.plus(1)
    }

    // Respond with Velocity template
    val model = mutableMapOf(
        "columnNames" to table.getAllAllowToShowColumns().map { it.columnName },
        "rows" to data,
        "pluralName" to pluralName.orEmpty().replaceFirstChar { it.uppercaseChar() },
        "hasSearch" to hasSearchColumn,
        "currentPage" to (currentPage?.plus(1) ?: 1),
        "maxPages" to maxPages,
        "filtersData" to filtersData,
        "actions" to table.getAllCustomActions()
    ).apply {
        order?.let {
            put("order", it.copy(direction = it.direction.lowercase()))
        }
    }.toMap()
    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/table_list.vm",
            model = model
        )
    )
}

private suspend fun ApplicationCall.handleNoSqlList(
    panel: AdminMongoCollection,
    pluralName: String?,
    currentPage: Int?,
    searchParameter: String?,
) {
    val searchFilters = if (searchParameter != null && panel.getSearches().isNotEmpty()) {
        Filters.or(
            panel.getSearches().map { Filters.regex(it, ".*$searchParameter.*", "i") },
        )
    } else Filters.empty()

    // Prepare filters data
    val filtersData = MongoFilters.findFiltersData(panel)

    val order = getFieldOrder(panel)


    val fieldFilters = MongoFilters.extractMongoFilters(panel, parameters)
    val filters = when {
        fieldFilters == Filters.empty() && searchFilters == Filters.empty() -> Filters.empty()
        fieldFilters == Filters.empty() -> searchFilters
        searchFilters == Filters.empty() -> fieldFilters
        else -> Filters.and(fieldFilters, searchFilters)
    }

    // Fetch data
    val data = MongoClientRepository.getAllData(
        panel,
        (currentPage ?: 1) - 1,
        filters = filters,
        order = order
    )

    val maxPages = MongoClientRepository.getTotalPages(panel, filters)
    val hasSearch = panel.getSearches().isNotEmpty()

    // Respond with Velocity template
    val model = mutableMapOf(
        "columnNames" to panel.getAllAllowToShowFields().map { it.fieldName },
        "rows" to data,
        "pluralName" to pluralName?.replaceFirstChar { it.uppercaseChar() }.orEmpty(),
        "hasSearch" to hasSearch,
        "currentPage" to (currentPage?.plus(1) ?: 1),
        "maxPages" to maxPages,
        "filtersData" to filtersData,
        "actions" to panel.getAllCustomActions()
    ).apply {
        order?.let {
            put("order", it.copy(direction = it.direction.lowercase()))
        }
    }.toMap()
    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/table_list.vm",
            model = model
        )
    )
}

