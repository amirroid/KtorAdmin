package modules

import annotations.errors.badRequest
import annotations.errors.notFound
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import models.*
import models.ReferenceItem
import models.TableGroup
import models.toTableGroups
import repository.FileRepository
import repository.JdbcQueriesRepository
import utils.AdminTable
import utils.Constants
import utils.getAllAllowToShowColumns

fun Application.configureRouting(tables: List<AdminTable>) {
    routing {
        staticResources("/static", "static")
        configureGetRouting(tables)
        configureSavesRouting(tables)
    }
}

fun Routing.configureGetRouting(tables: List<AdminTable>) {
    val tableGroups = tables.toTableGroups()
    route("/admin") {
        get {
            call.renderAdminPanel(tableGroups)
        }
        route("/{pluralName}") {
            get {
                call.handleTableList(tables)
            }
            get("add") {
                call.handleAddNewItem(tables)
            }
            get("/{primaryKey}") {
                call.handleEditItem(tables)
            }
        }
    }
}

private suspend fun ApplicationCall.renderAdminPanel(tableGroups: List<TableGroup>) {
    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel.vm",
            model = mutableMapOf("tableGroups" to tableGroups)
        )
    )
}

private suspend fun ApplicationCall.handleTableList(tables: List<AdminTable>) {
    val pluralName = parameters["pluralName"]
    val table = tables.find { it.getPluralName() == pluralName }
    if (table == null) {
        notFound("No table found with plural name: $pluralName")
    } else {
        val data = JdbcQueriesRepository.getAllData(table)
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/table_list.vm", model = mapOf(
                    "columnNames" to table.getAllAllowToShowColumns().map { it.columnName },
                    "rows" to data,
                    "pluralName" to pluralName.orEmpty().capitalize()
                )
            )
        )
    }
}

private suspend fun ApplicationCall.handleAddNewItem(tables: List<AdminTable>) {
    val pluralName = parameters["pluralName"]
    val table = tables.find { it.getPluralName() == pluralName }
    if (table == null) {
        notFound("No table found with plural name: $pluralName")
    } else {
        runCatching {
            val columns = table.getAllAllowToShowColumns()
            val referencesItems = getReferencesItems(tables, columns)
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
}

private suspend fun ApplicationCall.handleEditItem(tables: List<AdminTable>) {
    val pluralName = parameters["pluralName"]
    val primaryKey = parameters["primaryKey"]
    val table = tables.find { it.getPluralName() == pluralName }
    when {
        table == null -> respondText { "No table found with plural name: $pluralName" }
        primaryKey == null -> respondText { "No primary key found: $pluralName" }
        else -> {
            val data = JdbcQueriesRepository.getData(table, primaryKey)
            if (data == null) {
                notFound("No data found with primary key: $primaryKey")
            } else {
                runCatching {
                    val columns = table.getAllAllowToShowColumns()
                    val referencesItems = getReferencesItems(tables, columns)
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
                                "singularTableName" to table.getSingularName().replaceFirstChar { it.uppercaseChar() },
                                "references" to referencesItems
                            )
                        )
                    )
                }.onFailure {
                    badRequest("Error: ${it.message}")
                }
            }
        }
    }
}

private fun getReferencesItems(
    tables: List<AdminTable>,
    columns: List<ColumnSet>
): Map<ColumnSet, List<ReferenceItem>> {
    val columnsWithReferences = columns.filter { it.reference != null }
    if (columnsWithReferences.any { column -> tables.none { it.getTableName() == column.reference!!.tableName } }) {
        throw IllegalArgumentException("Error: Some referenced tables do not exist or are not defined in the current schema.")
    }
    return columnsWithReferences.associateWith { column ->
        JdbcQueriesRepository.getAllReferences(
            table = tables.first { it.getTableName() == column.reference!!.tableName },
            referenceColumn = column.reference!!.columnName
        )
    }
}


fun handlePreviewValue(columnSet: ColumnSet, value: String, call: ApplicationCall) = when (columnSet.type) {
    ColumnType.FILE -> FileRepository.generateMediaUrl(columnSet.uploadTarget!!, value, call)
    else -> value
}
