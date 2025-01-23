package modules.add

import annotations.errors.badRequest
import annotations.errors.notFound
import getters.getReferencesItems
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import tables.AdminJdbcTable
import tables.AdminPanel
import tables.getAllAllowToShowColumns
import utils.Constants

internal suspend fun ApplicationCall.handleAddNewItem(tables: List<AdminPanel>) {
    val pluralName = parameters["pluralName"]
    val panel = tables.find { it.getPluralName() == pluralName }
    if (panel == null) {
        notFound("No table found with plural name: $pluralName")
    } else {
        when (panel) {
            is AdminJdbcTable -> handleJdbcAddView(table = panel, tables = tables)
        }
    }
}

private suspend fun ApplicationCall.handleJdbcAddView(
    table: AdminJdbcTable,
    tables: List<AdminPanel>,
) {
    runCatching {
        val columns = table.getAllAllowToShowColumns()
        val referencesItems = getReferencesItems(tables.filterIsInstance<AdminJdbcTable>(), columns)
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin.vm", model = mapOf(
                    "columns" to columns,
                    "tableName" to table.getTableName(),
                    "method" to "post",
                    "singularTableName" to table.getSingularName().capitalize(),
                    "references" to referencesItems
                )
            )
        )
    }.onFailure {
        badRequest("Error: ${it.message}")
    }
}