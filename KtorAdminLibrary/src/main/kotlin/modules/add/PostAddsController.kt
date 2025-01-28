package modules.add

import utils.badRequest
import configuration.DynamicConfiguration
import converters.toEvents
import converters.toTableValues
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import models.ColumnSet
import panels.*
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import response.onError
import response.onSuccess
import validators.validateParameters

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

internal suspend fun RoutingContext.handleAddRequest(panels: List<AdminPanel>) {
    val pluralName = call.parameters["pluralName"]
    val panel = panels.findWithPluralName(pluralName)
    if (panel == null) {
        call.respondText { "No table found with plural name: $pluralName" }
        return
    }

    when (panel) {
        is AdminJdbcTable -> insertData(pluralName, panel, panels)
        is AdminMongoCollection -> insertData(pluralName, panel)
    }
}

private suspend fun RoutingContext.insertData(pluralName: String?, table: AdminJdbcTable, panels: List<AdminPanel>) {
    val parametersDataResponse = call.receiveMultipart().toTableValues(table)
    parametersDataResponse.onSuccess { parametersData ->
        val parameters = parametersData.map { it?.first }
        val columns = table.getAllAllowToShowFieldsInUpsert()

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
    }.onError { errors, values ->
        call.handleJdbcAddView(
            table = table,
            panels = panels,
            errors = errors,
            values = values
        )
    }
}

private suspend fun RoutingContext.insertData(pluralName: String?, panel: AdminMongoCollection) {
    val parametersData = call.receiveMultipart().toTableValues(panel)
    val parameters = parametersData.map { it?.first }
    val fields = panel.getAllAllowToShowFieldsInUpsert()
    val fieldsWithParameter = fields.mapIndexed { index, field ->
        field to parameters.getOrNull(index)
    }.toMap()
//    // Validate parameters
//    val isValidParameters = columns.validateParameters(parameters)
//    if (isValidParameters) {
    kotlin.runCatching {
        val id = MongoClientRepository.insertData(fieldsWithParameter, panel)
        call.respondRedirect("/admin/$pluralName")
    }.onFailure {
        call.badRequest("Failed to insert $pluralName\nReason: ${it.message}")
    }
//    } else {
//        call.badRequest("Invalid parameters for $pluralName: $parameters")
//    }
}