package modules.add

import utils.badRequest
import configuration.DynamicConfiguration
import converters.toEvents
import converters.toFieldEvents
import converters.toTableValues
import flash.setFlashSessionsAndRedirect
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import models.ColumnSet
import models.field.FieldSet
import modules.update.handleNoSqlEditView
import panels.*
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import response.onError
import response.onInvalidateRequest
import response.onSuccess
import utils.invalidateRequest
import utils.serverError
import validators.checkHasRole
import validators.validateFieldsParameters
import validators.validateParameters

private suspend fun onInsert(
    tableName: String,
    objectPrimaryKey: String,
    columnSets: List<ColumnSet>,
    parametersData: List<Pair<String, Any?>?>
) {
    DynamicConfiguration.currentEventListener?.onInsertJdbcData(
        tableName = tableName,
        objectPrimaryKey = objectPrimaryKey,
        events = columnSets.toEvents(parametersData.map { it?.second })
    )
}

private suspend fun onMongoInsert(
    collectionName: String,
    objectPrimaryKey: String?,
    fieldSets: List<FieldSet>,
    parametersData: List<Pair<String, Any?>?>
) {
    DynamicConfiguration.currentEventListener?.onInsertMongoData(
        collectionName = collectionName,
        objectPrimaryKey = objectPrimaryKey ?: return,
        events = fieldSets.toFieldEvents(parametersData.map { it?.second })
    )
}

internal suspend fun RoutingContext.handleAddRequest(panels: List<AdminPanel>) {
    val pluralName = call.parameters["pluralName"]
    val panel = panels.findWithPluralName(pluralName)
    if (panel == null) {
        call.respondText { "No table found with plural name: $pluralName" }
        return
    }
    call.checkHasRole(panel) {
        when (panel) {
            is AdminJdbcTable -> insertData(pluralName, panel, panels)
            is AdminMongoCollection -> insertData(pluralName, panel)
        }
    }
}

private suspend fun RoutingContext.insertData(pluralName: String?, table: AdminJdbcTable, panels: List<AdminPanel>) {
    val parametersDataResponse = call.receiveMultipart().toTableValues(table)
    parametersDataResponse.onSuccess { parametersData ->
        val parameters = parametersData.map { it?.first }
        val parametersClasses = parametersData.map { it?.second }
        val columns = table.getAllAllowToShowColumnsInUpsert()

        // Validate parameters
        val isValidParameters = columns.validateParameters(parameters)
        if (isValidParameters) {
            kotlin.runCatching {
                val id = JdbcQueriesRepository.insertData(table, parametersClasses)
                onInsert(
                    tableName = table.getTableName(),
                    columnSets = columns,
                    objectPrimaryKey = id.toString(),
                    parametersData = parametersData
                )
                call.respondRedirect("/admin/$pluralName")
            }.onFailure {
                call.badRequest("Failed to insert $pluralName\nReason: ${it.message}", it)
            }
        } else {
            call.badRequest("Invalid parameters for $pluralName: $parameters")
        }
    }.onError { requestId, errors, values ->
        call.setFlashSessionsAndRedirect(requestId, errors, values)
    }.onInvalidateRequest {
        call.invalidateRequest()
    }
}

private suspend fun RoutingContext.insertData(pluralName: String?, panel: AdminMongoCollection) {
    val parametersDataResponse = call.receiveMultipart().toTableValues(panel)
    parametersDataResponse.onSuccess { parametersData ->
        val parameters = parametersData.map { it?.first }
        val fields = panel.getAllAllowToShowFieldsInUpsert()
        val fieldsWithParameter = fields.mapIndexed { index, field ->
            field to parameters.getOrNull(index)
        }.toMap()
        // Validate parameters
        val isValidParameters = fields.validateFieldsParameters(parameters)
        if (isValidParameters) {
            kotlin.runCatching {
                val id = MongoClientRepository.insertData(fieldsWithParameter, panel)
                onMongoInsert(
                    collectionName = panel.getCollectionName(),
                    objectPrimaryKey = id,
                    fieldSets = fields,
                    parametersData = parametersData
                )
                call.respondRedirect("/admin/$pluralName")
            }.onFailure {
                call.badRequest("Failed to insert $pluralName\nReason: ${it.message}", it)
            }
        } else {
            call.badRequest("Invalid parameters for $pluralName: $parameters")
        }
    }.onError { errors, values ->
        call.handleNoSqlAddView(
            panel = panel,
            errors = errors,
            values = values
        )
    }.onInvalidateRequest {
        call.invalidateRequest()
    }
}