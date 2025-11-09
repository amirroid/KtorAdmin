package ir.amirroid.ktoradmin.modules.list

import ir.amirroid.ktoradmin.authentication.KtorAdminPrincipal
import com.mongodb.client.model.Filters
import ir.amirroid.ktoradmin.utils.badRequest
import ir.amirroid.ktoradmin.utils.notFound
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.csrf.CsrfManager
import ir.amirroid.ktoradmin.filters.JdbcFilters
import ir.amirroid.ktoradmin.filters.MongoFilters
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import ir.amirroid.ktoradmin.models.PanelGroup
import ir.amirroid.ktoradmin.models.order.Order
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.panels.getAllAllowToShowColumns
import ir.amirroid.ktoradmin.panels.getAllAllowToShowFields
import ir.amirroid.ktoradmin.panels.getAllCustomActions
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository
import ir.amirroid.ktoradmin.repository.MongoClientRepository
import ir.amirroid.ktoradmin.translator.KtorAdminTranslator
import ir.amirroid.ktoradmin.translator.translator
import ir.amirroid.ktoradmin.utils.Constants
import ir.amirroid.ktoradmin.utils.addCommonModels
import ir.amirroid.ktoradmin.utils.serverError
import ir.amirroid.ktoradmin.validators.checkHasRole
import org.bson.conversions.Bson

// Base interface for common panel functionality
private interface PanelHandler {
    suspend fun handleList(
        call: ApplicationCall,
        panel: AdminPanel,
        pluralName: String,
        currentPage: Int?,
        searchParameter: String?,
        panelGroups: List<PanelGroup>
    )


    fun calculateMaxPages(
        totalCount: Long
    ): Long {
        val pages = totalCount / DynamicConfiguration.maxItemsInPage
        return if (totalCount % DynamicConfiguration.maxItemsInPage == 0L) pages else pages + 1
    }
}

internal suspend fun ApplicationCall.handlePanelList(tables: List<AdminPanel>, panelGroups: List<PanelGroup>) {
    val pluralName = parameters["pluralName"]
    val searchParameter = parameters["search"]?.takeIf { it.isNotEmpty() }
    val currentPage = getValidatedCurrentPage()

    val panel = tables.find { it.getPluralName() == pluralName }

    when {
        panel == null || panel.isShowInAdminPanel().not() -> notFound("No table found with plural name: $pluralName")
        else -> checkHasRole(panel) {
            runCatching {
                val handler = when (panel) {
                    is AdminJdbcTable -> JdbcPanelHandler(tables.filterIsInstance<AdminJdbcTable>())
                    is AdminMongoCollection -> MongoPanelHandler()
                    else -> throw IllegalArgumentException("Unsupported panel type")
                }

                handler.handleList(this, panel, pluralName!!, currentPage, searchParameter, panelGroups)
            }.onFailure {
                serverError(it.message ?: "", it)
            }
        }
    }
}

private suspend fun ApplicationCall.getValidatedCurrentPage(): Int? {
    return runCatching {
        parameters["page"]?.toInt()?.minus(1) ?: 0
    }.onFailure { cause ->
        badRequest(cause.message ?: "", cause)
    }.getOrNull()
}

private class JdbcPanelHandler(private val jdbcTables: List<AdminJdbcTable>) : PanelHandler {
    override suspend fun handleList(
        call: ApplicationCall,
        panel: AdminPanel,
        pluralName: String,
        currentPage: Int?,
        searchParameter: String?,
        panelGroups: List<PanelGroup>
    ) {
        panel as AdminJdbcTable

        val order = call.getColumnOrder(panel)
        val filtersData = JdbcFilters.findFiltersData(panel, jdbcTables)
        val filters = JdbcFilters.extractFilters(panel, jdbcTables, call.parameters)

        val totalCount = JdbcQueriesRepository.getCount(panel, searchParameter, filters)
        val data = JdbcQueriesRepository.getAllData(panel, jdbcTables, searchParameter, currentPage, filters, order)
        val maxPages = calculateMaxPages(totalCount)

        call.respondWithTemplate(
            panel = panel,
            data = data,
            pluralName = pluralName,
            maxPages = maxPages,
            currentPage = currentPage,
            filtersData = filtersData,
            order = order,
            panelGroups = panelGroups,
            hasSearch = panel.getSearches().isNotEmpty(),
            count = totalCount
        )
    }
}

private class MongoPanelHandler : PanelHandler {
    override suspend fun handleList(
        call: ApplicationCall,
        panel: AdminPanel,
        pluralName: String,
        currentPage: Int?,
        searchParameter: String?,
        panelGroups: List<PanelGroup>
    ) {
        panel as AdminMongoCollection

        val searchFilters = createSearchFilters(panel, searchParameter)
        val fieldFilters = MongoFilters.extractMongoFilters(panel, call.parameters)
        val combinedFilters = combineFilters(searchFilters, fieldFilters)

        val order = call.getFieldOrder(panel)
        val filtersData = MongoFilters.findFiltersData(panel)

        val data = MongoClientRepository.getAllData(
            panel,
            (currentPage ?: 1) - 1,
            filters = combinedFilters,
            order = order
        )

        val totalCount = MongoClientRepository.getCount(panel, combinedFilters)

        call.respondWithTemplate(
            panel = panel,
            data = data,
            pluralName = pluralName,
            maxPages = calculateMaxPages(totalCount),
            currentPage = currentPage,
            filtersData = filtersData,
            order = order,
            panelGroups = panelGroups,
            hasSearch = panel.getSearches().isNotEmpty(),
            count = totalCount
        )
    }

    private fun createSearchFilters(panel: AdminMongoCollection, searchParameter: String?): Bson {
        return if (searchParameter != null && panel.getSearches().isNotEmpty()) {
            Filters.or(panel.getSearches().map { Filters.regex(it, ".*$searchParameter.*", "i") })
        } else Filters.empty()
    }

    private fun combineFilters(searchFilters: Bson, fieldFilters: Bson): Bson {
        return when {
            fieldFilters == Filters.empty() && searchFilters == Filters.empty() -> Filters.empty()
            fieldFilters == Filters.empty() -> searchFilters
            searchFilters == Filters.empty() -> fieldFilters
            else -> Filters.and(fieldFilters, searchFilters)
        }
    }
}

private suspend fun ApplicationCall.getColumnOrder(table: AdminJdbcTable): Order? {
    val orderDirection = parameters["orderDirection"]?.takeIf { it.isNotEmpty() }
    validateOrderDirection(orderDirection)

    return parameters["order"]?.takeIf { it.isNotEmpty() }?.let { orderColumn ->
        if (orderColumn !in table.getAllColumns().map { it.columnName }) {
            badRequest("The column '$orderColumn' specified in the order does not exist in the table. Please provide a valid column name for ordering.")
        }
        Order(orderColumn, orderDirection ?: "ASC")
    } ?: table.getDefaultOrder()?.let {
        if (orderDirection == null) it else it.copy(direction = orderDirection)
    }
}

private suspend fun ApplicationCall.getFieldOrder(table: AdminMongoCollection): Order? {
    val orderDirection = parameters["orderDirection"]?.takeIf { it.isNotEmpty() } ?: "ASC"
    if (orderDirection.lowercase() !in listOf("asc", "desc")) {
        badRequest("Invalid order direction '$orderDirection'. Valid values are 'asc' or 'desc'.")
    }

    return parameters["order"]?.takeIf { it.isNotEmpty() }?.let { orderField ->
        if (orderField !in table.getAllFields().map { it.fieldName }) {
            badRequest("The column '$orderField' specified in the order does not exist in the collection. Please provide a valid field name for ordering.")
        }
        Order(orderField, orderDirection)
    } ?: table.getDefaultOrder()
}

private suspend fun ApplicationCall.validateOrderDirection(orderDirection: String?) {
    if (orderDirection?.lowercase() !in listOf("asc", "desc", null)) {
        badRequest("Invalid order direction '$orderDirection'. Valid values are 'asc' or 'desc'.")
    }
}

private suspend fun ApplicationCall.respondWithTemplate(
    panel: AdminPanel,
    data: Any,
    pluralName: String,
    maxPages: Long,
    currentPage: Int?,
    filtersData: Any,
    order: Order?,
    panelGroups: List<PanelGroup>,
    hasSearch: Boolean,
    count: Long,
) {
    val user = principal<KtorAdminPrincipal>()
    val model = buildTemplateModel(
        panel = panel,
        data = data,
        pluralName = pluralName,
        maxPages = maxPages,
        currentPage = currentPage,
        filtersData = filtersData,
        order = order,
        panelGroups = panelGroups,
        hasSearch = hasSearch,
        username = user?.name,
        count = count,
        applicationCall = this
    )

    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_list.vm",
            model = model
        )
    )
}

private suspend fun buildTemplateModel(
    panel: AdminPanel,
    data: Any,
    pluralName: String,
    maxPages: Long,
    currentPage: Int?,
    filtersData: Any,
    order: Order?,
    panelGroups: List<PanelGroup>,
    hasSearch: Boolean,
    username: String?,
    count: Long,
    applicationCall: ApplicationCall
): Map<String, Any> {
    val translator = applicationCall.translator
    return mutableMapOf(
        "fields" to when (panel) {
            is AdminJdbcTable -> panel.getAllAllowToShowColumns()
            is AdminMongoCollection -> panel.getAllAllowToShowFields()
            else -> emptyList<Any>()
        },
        "rows" to data,
        "pluralName" to pluralName.replaceFirstChar { it.uppercaseChar() },
        "pluralNameBase" to pluralName,
        "hasSearch" to hasSearch,
        "currentPage" to (currentPage?.plus(1) ?: 1),
        "maxPages" to maxPages,
        "filtersData" to filtersData,
        "actions" to panel.getAllCustomActions(deleteActionDisplayText = translator.translate("delete_selected_items")),
        "csrfToken" to CsrfManager.generateToken(),
        "panelGroups" to panelGroups,
        "currentPanel" to panel.getPluralName(),
        "canDownload" to DynamicConfiguration.canDownloadDataAsCsv,
        "count" to count,
        "count_text" to if (count == 0L) translator.translate(KtorAdminTranslator.Keys.EMPTY) else translator.translate(
            KtorAdminTranslator.Keys.ITEMS,
            mapOf("count" to count.toString())
        )
    ).apply {
        order?.let {
            put("order", it.copy(direction = it.direction.lowercase()))
        }
        username?.let { put("username", it) }
    }.addCommonModels(panel, panelGroups, applicationCall = applicationCall)
}