package modules

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import models.toTableGroups
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
            call.respond(
                VelocityContent(
                    "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel.vm",
                    model = mutableMapOf(
                        "tableGroups" to tableGroups,
                    )
                )
            )
        }
        route("/{pluralName}") {
            get {
                val pluralName = call.parameters["pluralName"]
                val table = tables.find { it.getPluralName() == pluralName }
                if (table == null) {
                    call.respondText { "No table found with plural name: $pluralName" }
                } else {
                    val data = JdbcQueriesRepository.getAllData(table)
                    call.respond(
                        VelocityContent(
                            "${Constants.TEMPLATES_PREFIX_PATH}/table_list.vm", model = mapOf(
                                "columnNames" to table.getAllAllowToShowColumns().map { it.columnName },
                                "rows" to data,
                                "pluralName" to pluralName.orEmpty().replaceFirstChar { it.uppercaseChar() }
                            )
                        )
                    )
                }
            }
            get("add") {
                val pluralName = call.parameters["pluralName"]
                val table = tables.find { it.getPluralName() == pluralName }
                if (table == null) {
                    call.respondText { "No table found with plural name: $pluralName" }
                } else {
                    call.respond(
                        VelocityContent(
                            "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin.vm", model = mapOf(
                                "columns" to table.getAllAllowToShowColumns(),
                                "tableName" to table.getTableName(),
                                "method" to "post"
                            )
                        )
                    )
                }
            }
            get("/{primaryKey}") {
                val pluralName = call.parameters["pluralName"]
                val primaryKey = call.parameters["primaryKey"]
                val table = tables.find { it.getPluralName() == pluralName }
                when {
                    table == null -> call.respondText { "No table found with plural name: $pluralName" }
                    primaryKey == null -> call.respondText { "No primary key found: $pluralName" }
                    else -> {
                        val data = JdbcQueriesRepository.getData(table, primaryKey)
                        if (data == null) {
                            call.respondText { "No data found with primary key: $primaryKey" }
                        } else {
                            val columns = table.getAllAllowToShowColumns()
                            call.respond(
                                VelocityContent(
                                    "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin.vm", model = mapOf(
                                        "columns" to columns,
                                        "tableName" to table.getTableName(),
                                        "values" to columns.mapIndexed { index, column ->
                                            column.columnName to data[index]
                                        }.toMap()
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}