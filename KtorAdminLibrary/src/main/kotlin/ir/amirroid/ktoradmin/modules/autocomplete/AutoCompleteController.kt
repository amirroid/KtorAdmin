package ir.amirroid.ktoradmin.modules.autocomplete

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.models.common.tableName
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.panels.findWithPluralName
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository
import ir.amirroid.ktoradmin.utils.withAuthenticate
import kotlinx.serialization.Serializable

@Serializable
data class AutoCompleteRequest(
    val search: String = "",
    val page: Int = 0,
)

@Serializable
data class AutoCompleteResponse(
    val items: List<AutoCompleteItem>,
    val totalCount: Int,
)

@Serializable
data class AutoCompleteItem(
    val key: String,
    val label: String,
)

/**
 * Configures the autocomplete routing endpoints.
 * This provides a generic autocomplete endpoint for foreign key fields
 * annotated with @AutoComplete.
 *
 * Route: POST /{adminPath}/autocomplete/{tableName}/{columnName}
 *
 * The columnName parameter is required to resolve the specific ColumnSet
 * and its autoCompleteSearchFields from metadata.
 *
 * Resolution logic:
 * 1. Find owner table (e.g., "tasks") from tableName
 * 2. Find ColumnSet (e.g., "user_id") from columnName
 * 3. Get referenced table (e.g., "users") from columnSet.reference
 * 4. Search against the referenced table using autoCompleteSearchFields
 */
internal fun Routing.configureAutoCompleteRouting(
    panels: List<AdminPanel>,
    authenticateName: String? = null,
) {
    withAuthenticate(authenticateName) {
        route("/${DynamicConfiguration.adminPath}/autocomplete") {
            post("/{tableName}/{columnName}") {
                val tableName = call.parameters["tableName"] ?: ""
                val columnName = call.parameters["columnName"] ?: ""

                val ownerTable = panels.findWithPluralName(tableName) as? AdminJdbcTable
                if (ownerTable == null) {
                    call.respondText { "Table not found: $tableName" }
                    return@post
                }

                val columnSet = ownerTable.getAllColumns().find { it.columnName == columnName }
                if (columnSet == null || !columnSet.hasAutoComplete) {
                    call.respondText { "Autocomplete field not found: $columnName" }
                    return@post
                }

                val reference = columnSet.reference
                if (reference == null || (reference !is Reference.ManyToOne && reference !is Reference.OneToOne)) {
                    call.respondText { "Invalid reference type for autocomplete field: $columnName" }
                    return@post
                }

                val referencedTableName = reference.tableName
                val referencedTable =
                    panels.find { panel ->
                        panel is AdminJdbcTable && panel.getTableName() == referencedTableName
                    } as? AdminJdbcTable

                if (referencedTable == null) {
                    call.respondText { "Referenced table not found: $referencedTableName" }
                    return@post
                }

                val request = call.receive<AutoCompleteRequest>()
                val search = request.search.takeIf { it.isNotBlank() }
                val page = request.page.coerceAtLeast(0)

                val pageSize = DynamicConfiguration.autocompletePageSize

                val searchFields = columnSet.autoCompleteSearchFields

                val results =
                    JdbcQueriesRepository.searchReferences(
                        table = referencedTable,
                        search = search,
                        page = page,
                        pageSize = pageSize,
                        searchFields = searchFields,
                    )

                call.respond(results)
            }
        }
    }
}
