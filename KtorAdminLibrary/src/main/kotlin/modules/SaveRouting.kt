package modules

import annotations.errors.badRequest
import annotations.errors.notFound
import annotations.errors.serverError
import configuration.DynamicConfiguration
import getters.toTypedValue
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import models.events.ColumnEvent
import models.ColumnSet
import models.ColumnType
import models.events.FileEvent
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


fun List<ColumnSet>.toEvents(parameters: List<Any?>, changedColumns: List<String>? = null) =
    mapIndexed { index, columnSet ->
        ColumnEvent(
            changedColumns == null || changedColumns.any { column -> column == columnSet.columnName },
            columnSet,
            parameters[index]
        )
    }

private suspend fun onInsert(
    tableName: String,
    objectPrimaryKey: String,
    columnSets: List<ColumnSet>,
    parametersData: List<Pair<String, Any?>?>
) {
    DynamicConfiguration.currentEventListener?.onInsertData(
        tableName = tableName,
        objectPrimaryKey = objectPrimaryKey,
        events = columnSets.toEvents(parametersData.map { it?.second })
    )
}

private suspend fun onUpdate(
    tableName: String,
    objectPrimaryKey: String,
    changedColumns: List<String>,
    columnSets: List<ColumnSet>,
    parametersData: List<Pair<String, Any?>?>
) {
    DynamicConfiguration.currentEventListener?.onUpdateData(
        tableName = tableName,
        objectPrimaryKey = objectPrimaryKey,
        events = columnSets.toEvents(parametersData.map { it?.second }, changedColumns = changedColumns)
    )
}

suspend fun RoutingContext.handleAddRequest(tables: List<AdminTable>) {
    val pluralName = call.parameters["pluralName"]
    val table = tables.findWithPluralName(pluralName)
    if (table == null) {
        call.respondText { "No table found with plural name: $pluralName" }
        return
    }

    val parametersData = call.receiveMultipart().toTableValues(table)
    val parameters = parametersData.map { it?.first }
    val columns = table.getAllAllowToShowColumns()

    // Validate parameters
    val isValidParameters = columns.validateParameters(parameters)
    if (isValidParameters) {
        kotlin.runCatching {
            val id = JdbcQueriesRepository.insertData(table, parameters)
            if (id != null) {
                onInsert(
                    tableName = table.getTableName(),
                    columnSets = columns,
                    objectPrimaryKey = id.toString(),
                    parametersData = parametersData
                )
            }
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
    val columns = table.getAllAllowToShowColumns()

    val parametersData = call.receiveMultipart().toTableValues(table)
    val parameters = parametersData.map { it?.first }

    kotlin.runCatching {
        val changedDataAndId = JdbcQueriesRepository.updateChangedData(table, parameters, primaryKey)
        onUpdate(
            tableName = table.getTableName(),
            objectPrimaryKey = changedDataAndId?.first?.toString() ?: primaryKey,
            changedColumns = changedDataAndId?.second ?: emptyList(),
            columnSets = columns,
            parametersData = parametersData
        )
        call.respondRedirect("/admin/$pluralName")
    }.onFailure {
        call.serverError("Failed to update $pluralName\nReason: ${it.message}")
    }
}

fun Parameters.toTableValues(table: AdminTable) = table.getAllAllowToShowColumns().map {
    get(it.columnName)
}

suspend fun MultiPartData.toTableValues(table: AdminTable): List<Pair<String, Any?>?> {
    val items = mutableMapOf<String, Pair<String, Any?>?>()
    val columns = table.getAllAllowToShowColumns()

    // Process each part of the multipart request
    forEachPart { partData ->
        val name = partData.name
        val column = columns.firstOrNull { it.columnName == name }

        if (column != null && name != null) {
            when (partData) {
                is PartData.FormItem -> items[name] = partData.value.let { it to it.toTypedValue(column.type) }
                is PartData.FileItem -> {
                    val targetColumn = table.getAllColumns().firstOrNull { it.columnName == name }
                    when (targetColumn?.type) {
                        ColumnType.FILE -> {
                            val fileData = FileRepository.uploadFile(column.uploadTarget!!, partData)?.let {
                                it.first to FileEvent(
                                    fileName = it.first,
                                    bytes = it.second
                                )
                            }
                            items[name] = fileData
                        }

                        ColumnType.BINARY -> {
                            items[name] =
                                partData.provider().readRemaining().readByteArray().let {
                                    it.decodeToString() to it
                                }
                        }

                        else -> Unit
                    }
                }

                else -> Unit
            }
            partData.dispose()
        }
    }

    // Return values corresponding to the columns
    return columns.map { items[it.columnName] }
}