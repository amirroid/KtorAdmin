package modules.add

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

internal suspend fun ApplicationCall.handleAddNewItem(tables: List<AdminPanel>) {
    val pluralName = parameters["pluralName"]
    val panel = tables.find { it.getPluralName() == pluralName }
    if (panel == null) {
        notFound("No table found with plural name: $pluralName")
    } else {
        when (panel) {
            is AdminJdbcTable -> handleJdbcAddView(table = panel, panels = tables)
            is AdminMongoCollection -> handleNoSqlAddView(panel = panel)
        }
    }
}

internal suspend fun ApplicationCall.handleJdbcAddView(
    table: AdminJdbcTable,
    panels: List<AdminPanel>,
    errors: List<ErrorResponse> = emptyList()
) {
    runCatching {
        val columns = table.getAllAllowToShowColumns()
        val referencesItems = getReferencesItems(panels.filterIsInstance<AdminJdbcTable>(), columns)
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin.vm", model = mapOf(
                    "columns" to columns,
                    "tableName" to table.getTableName(),
                    "singularTableName" to table.getSingularName().replaceFirstChar { it.uppercaseChar() },
                    "references" to referencesItems,
                    "errors" to errors.toMap()
                )
            )
        )
    }.onFailure {
        badRequest("Error: ${it.message}")
    }
}

private suspend fun ApplicationCall.handleNoSqlAddView(
    panel: AdminMongoCollection,
//    panels: List<AdminPanel>,
) {
    runCatching {
        val fields = panel.getAllAllowToShowFieldsInUpsert()
//        val referencesItems = getReferencesItems(tables.filterIsInstance<AdminJdbcTable>(), columns)
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin2.vm", model = mapOf(
                    "fields" to fields,
                    "singularTableName" to panel.getSingularName()
                        .replaceFirstChar { it.uppercaseChar() },
                    "collectionName" to panel.getCollectionName(),
                    "singularName" to panel.getSingularName().replaceFirstChar { it.uppercaseChar() },
                )
            )
        )
    }.onFailure {
        badRequest("Error: ${it.message}")
    }
}