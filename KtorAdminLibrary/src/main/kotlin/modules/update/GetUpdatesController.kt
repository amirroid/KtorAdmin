package modules.update

import annotations.errors.notFound
import annotations.errors.serverError
import getters.getReferencesItems
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import models.ColumnSet
import models.ColumnType
import repository.FileRepository
import repository.JdbcQueriesRepository
import tables.AdminJdbcTable
import tables.AdminPanel
import tables.getAllAllowToShowColumns
import utils.Constants


internal suspend fun ApplicationCall.handleEditItem(tables: List<AdminPanel>) {
    val pluralName = parameters["pluralName"]
    val primaryKey = parameters["primaryKey"]
    val panel = tables.find { it.getPluralName() == pluralName }
    when {
        panel == null -> respondText { "No table found with plural name: $pluralName" }
        primaryKey == null -> respondText { "No primary key found: $pluralName" }
        else -> {
            when (panel) {
                is AdminJdbcTable -> handleJdbcEditView(primaryKey, panel, tables)
            }
        }
    }
}

private suspend fun ApplicationCall.handleJdbcEditView(
    primaryKey: String,
    table: AdminJdbcTable,
    tables: List<AdminPanel>,
) {
    val data = JdbcQueriesRepository.getData(table, primaryKey)
    if (data == null) {
        notFound("No data found with primary key: $primaryKey")
    } else {
        runCatching {
            val columns = table.getAllAllowToShowColumns()
            val referencesItems = getReferencesItems(tables.filterIsInstance<AdminJdbcTable>(), columns)
            val values = columns.mapIndexed { index, column ->
                column.columnName to data[index]?.let { item ->
                    handlePreviewValue(
                        column,
                        item,
                        this
                    )
                }
            }.toMap()
            println("VALUES $values")
            respond(
                VelocityContent(
                    "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin.vm", model = mapOf(
                        "columns" to columns,
                        "tableName" to table.getTableName(),
                        "values" to values,
                        "singularTableName" to table.getSingularName()
                            .replaceFirstChar { it.uppercaseChar() },
                        "references" to referencesItems
                    )
                )
            )
        }.onFailure {
            serverError("Error: ${it.message}")
        }
    }
}

suspend fun handlePreviewValue(columnSet: ColumnSet, value: String, call: ApplicationCall) = when (columnSet.type) {
    ColumnType.FILE -> FileRepository.generateMediaUrl(columnSet.uploadTarget!!, value, call)
    else -> value
}
