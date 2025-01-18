package modules

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import repository.JdbcQueriesRepository
import utils.AdminTable
import utils.Constants
import utils.getAllAllowToShowColumns


fun Routing.configureSavesRouting(tables: List<AdminTable>) {
    route("/admin/") {
        post("{pluralName}/add") {
            val pluralName = call.parameters["pluralName"]
            val table = tables.find { it.getPluralName() == pluralName }
            if (table == null) {
                call.respondText { "No table found with plural name: $pluralName" }
            } else {
                val parameters = call.receiveParameters().toTableValues(table)
                if (parameters.any { it == null }) {
                    call.respondText { "No parameters found for $pluralName: $parameters" }
                } else {
                    kotlin.runCatching {
                        JdbcQueriesRepository.insertData(
                            table = table,
                            parameters = parameters.filterNotNull()
                        )
                        call.respondRedirect("/admin/$pluralName")
                    }.onFailure {
                        call.respondText { "Failed to insert $pluralName\nReason: ${it.message}" }
                    }
                }
            }
        }
        post("{pluralName}/{primaryKey}") {
            val pluralName = call.parameters["pluralName"]
            val primaryKey = call.parameters["primaryKey"]
            println(pluralName)
            val table = tables.find { it.getPluralName() == pluralName }
            when {
                table == null -> call.respondText { "No table found with plural name: $pluralName" }
                primaryKey == null -> call.respondText { "No primary key found: $pluralName" }
                else -> {
                    val parameters = call.receiveParameters().toTableValues(table)
                    if (parameters.any { it == null }) {
                        call.respondText { "No parameters found for $pluralName: $parameters" }
                    } else {
                        println(parameters)
                        kotlin.runCatching {
                            JdbcQueriesRepository.updateData(
                                table = table,
                                parameters = parameters.filterNotNull(),
                                primaryKey = primaryKey
                            )
                            call.respondRedirect("/admin/$pluralName")
                        }.onFailure {
                            call.respondText { "Failed to insert $pluralName\nReason: ${it.message}" }
                        }
                    }
                }
            }
        }
    }
}


fun Parameters.toTableValues(table: AdminTable) = table.getAllAllowToShowColumns().map {
    get(it.columnName)
}