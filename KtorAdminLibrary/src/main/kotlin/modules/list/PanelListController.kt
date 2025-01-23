package modules.list

import annotations.errors.badRequest
import annotations.errors.notFound
import configuration.DynamicConfiguration
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import repository.JdbcQueriesRepository
import tables.AdminJdbcTable
import tables.AdminPanel
import tables.getAllAllowToShowColumns
import utils.Constants

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
            is AdminJdbcTable -> handleJdbcList(panel, searchParameter, currentPage, pluralName)
        }
    }
}


private suspend fun ApplicationCall.handleJdbcList(
    table: AdminJdbcTable,
    searchParameter: String?,
    currentPage: Int?,
    pluralName: String?
) {
    val data = JdbcQueriesRepository.getAllData(table, searchParameter, currentPage)
    val maxPages = JdbcQueriesRepository.getCount(table, searchParameter).let {
        val calculatedValue = it / DynamicConfiguration.maxItemsInPage
        if (it % DynamicConfiguration.maxItemsInPage == 0) {
            calculatedValue
        } else calculatedValue.plus(1)
    }
    val hasSearchColumn = table.getSearchColumns().isNotEmpty()
    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/table_list.vm", model = mapOf(
                "columnNames" to table.getAllAllowToShowColumns().map { it.columnName },
                "rows" to data,
                "pluralName" to pluralName.orEmpty().replaceFirstChar { it.uppercaseChar() },
                "hasSearchColumn" to hasSearchColumn,
                "currentPage" to (currentPage?.plus(1) ?: 1),
                "maxPages" to maxPages
            )
        )
    )
}