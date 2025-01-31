package modules.add

import csrf.CsrfManager
import utils.badRequest
import utils.notFound
import getters.getReferencesItems
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import panels.*
import response.ErrorResponse
import response.toMap
import utils.Constants
import validators.checkHasRole

internal suspend fun ApplicationCall.handleAddNewItem(panels: List<AdminPanel>) {
    val pluralName = parameters["pluralName"]
    val panel = panels.find { it.getPluralName() == pluralName }
    if (panel == null) {
        notFound("No table found with plural name: $pluralName")
    } else {
        checkHasRole(panel) {
            when (panel) {
                is AdminJdbcTable -> handleJdbcAddView(table = panel, panels = panels)
                is AdminMongoCollection -> handleNoSqlAddView(panel = panel)
            }
        }
    }
}

internal suspend fun ApplicationCall.handleJdbcAddView(
    table: AdminJdbcTable,
    panels: List<AdminPanel>,
    errors: List<ErrorResponse> = emptyList(),
    values: Map<String, String?> = emptyMap()
) {
    runCatching {
        val columns = table.getAllAllowToShowColumnsInUpsert()
        val referencesItems = getReferencesItems(panels.filterIsInstance<AdminJdbcTable>(), columns)
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin.vm", model = mapOf(
                    "columns" to columns,
                    "tableName" to table.getTableName(),
                    "singularTableName" to table.getSingularName().replaceFirstChar { it.uppercaseChar() },
                    "references" to referencesItems,
                    "errors" to errors.toMap(),
                    "values" to values,
                    "csrfToken" to CsrfManager.generateToken()
                )
            )
        )
    }.onFailure {
        badRequest("Error: ${it.message}", it)
    }
}

internal suspend fun ApplicationCall.handleNoSqlAddView(
    panel: AdminMongoCollection,
//    panels: List<AdminPanel>,
    values: Map<String, String?> = emptyMap(),
    errors: List<ErrorResponse> = emptyList()
) {
    runCatching {
        val fields = panel.getAllAllowToShowFieldsInUpsert()
//        val referencesItems = getReferencesItems(tables.filterIsInstance<AdminJdbcTable>(), columns)
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/no_sql_upsert_admin.vm", model = mapOf(
                    "fields" to fields,
                    "singularTableName" to panel.getSingularName()
                        .replaceFirstChar { it.uppercaseChar() },
                    "collectionName" to panel.getCollectionName(),
                    "singularName" to panel.getSingularName().replaceFirstChar { it.uppercaseChar() },
                    "values" to values,
                    "errors" to errors.toMap(),
                    "csrfToken" to CsrfManager.generateToken()
                )
            )
        )
    }.onFailure {
        badRequest("Error: ${it.message}", it)
    }
}