package modules

import annotations.errors.badRequest
import annotations.errors.notFound
import annotations.errors.serverError
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import models.ColumnSet
import repository.JdbcQueriesRepository
import repository.FileRepository
import utils.AdminTable
import utils.allIndexed
import utils.findWithPluralName
import utils.getAllAllowToShowColumns

fun Routing.configureSavesRouting(tables: List<AdminTable>) {
    route("/admin/") {
        post("{pluralName}/add") {
            handleAddRequest(tables)
        }

        post("{pluralName}/{primaryKey}") {
            handleUpdateRequest(tables)
        }
    }
}

private fun List<ColumnSet>.validateParameters(parameters: List<String?>) = allIndexed { index, columnSet ->
    columnSet.nullable || parameters[index] != null
}


suspend fun RoutingContext.handleAddRequest(tables: List<AdminTable>) {
    val pluralName = call.parameters["pluralName"]
    val table = tables.findWithPluralName(pluralName)
    if (table == null) {
        call.respondText { "No table found with plural name: $pluralName" }
        return
    }

    val parameters = call.receiveMultipart().toTableValues(table)
    val columns = table.getAllAllowToShowColumns()

    // Validate parameters
    val isValidParameters = columns.validateParameters(parameters)
    if (isValidParameters) {
        kotlin.runCatching {
            JdbcQueriesRepository.insertData(table, parameters)
            call.respondRedirect("/admin/$pluralName")
        }.onFailure {
            call.badRequest("Failed to insert $pluralName\nReason: ${it.message}")
        }
    } else {
        call.badRequest("Invalid parameters for $pluralName: $parameters")
    }
}

suspend fun RoutingContext.handleUpdateRequest(tables: List<AdminTable>) {
    val pluralName = call.parameters["pluralName"]
    val primaryKey = call.parameters["primaryKey"]

    if (pluralName == null || primaryKey == null) {
        call.notFound("Invalid table or primary key")
        return
    }
    val table = tables.findWithPluralName(pluralName)

    if (table == null) {
        call.notFound("No table found with plural name: $pluralName")
        return
    }

    val parameters = call.receiveMultipart().toTableValues(table)

    kotlin.runCatching {
        JdbcQueriesRepository.updateChangedData(table, parameters, primaryKey)
        call.respondRedirect("/admin/$pluralName")
    }.onFailure {
        call.serverError("Failed to update $pluralName\nReason: ${it.message}")
    }
}

fun Parameters.toTableValues(table: AdminTable) = table.getAllAllowToShowColumns().map {
    get(it.columnName)
}

suspend fun MultiPartData.toTableValues(table: AdminTable): List<String?> {
    val items = mutableMapOf<String, String?>()
    val columns = table.getAllAllowToShowColumns()

    // Process each part of the multipart request
    forEachPart { partData ->
        val name = partData.name
        val column = columns.firstOrNull { it.columnName == name }

        if (column != null && name != null) {
            when (partData) {
                is PartData.FormItem -> items[name] = partData.value
                is PartData.FileItem -> {
                    val fileName = FileRepository.uploadFile(column.uploadTarget!!, partData)
                    items[name] = fileName
                }

                else -> Unit
            }
            partData.dispose()
        }
    }

    // Return values corresponding to the columns
    return columns.map { items[it.columnName] }
}