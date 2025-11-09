package ir.amirroid.ktoradmin.modules.add

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.converters.toEvents
import ir.amirroid.ktoradmin.converters.toFieldEvents
import ir.amirroid.ktoradmin.converters.toTableValues
import ir.amirroid.ktoradmin.flash.setFlashSessionsAndRedirect
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.field.FieldSet
import ir.amirroid.ktoradmin.models.response.updateSelectedReferences
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.panels.findWithPluralName
import ir.amirroid.ktoradmin.panels.getAllAllowToShowColumnsInUpsert
import ir.amirroid.ktoradmin.panels.getAllAllowToShowFieldsInUpsert
import ir.amirroid.ktoradmin.panels.hasAddAction
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository
import ir.amirroid.ktoradmin.repository.MongoClientRepository
import ir.amirroid.ktoradmin.response.onError
import ir.amirroid.ktoradmin.response.onInvalidateRequest
import ir.amirroid.ktoradmin.response.onSuccess
import ir.amirroid.ktoradmin.translator.translator
import ir.amirroid.ktoradmin.utils.badRequest
import ir.amirroid.ktoradmin.utils.invalidateRequest
import ir.amirroid.ktoradmin.utils.respondBack
import ir.amirroid.ktoradmin.validators.checkHasRole
import ir.amirroid.ktoradmin.validators.validateFieldsParameters
import ir.amirroid.ktoradmin.validators.validateParameters

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
    if (panel == null || panel.isShowInAdminPanel().not()) {
        call.respondText { "No table found with plural name: $pluralName" }
        return
    }
    if (panel.hasAddAction.not()) {
        call.badRequest("Add action is disabled")
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
    val currentTranslator = call.translator
    val parametersDataResponse = call.receiveMultipart().toTableValues(table, translator = currentTranslator)
    val tables = panels.filterIsInstance<AdminJdbcTable>()
    parametersDataResponse.onSuccess { parametersData ->
        val parameters = parametersData.values.map { it?.first }
        val parametersClasses = parametersData.values.map { it?.second }
        val columns = table.getAllAllowToShowColumnsInUpsert()

        // Validate parameters
        val isValidParameters = columns.validateParameters(parameters)
        if (isValidParameters) {
            runCatching {
                val id = JdbcQueriesRepository.insertData(table, parametersClasses)
                parametersData.updateSelectedReferences(table, tables, id.toString())
                onInsert(
                    tableName = table.getTableName(),
                    columnSets = columns,
                    objectPrimaryKey = id.toString(),
                    parametersData = parametersData.values
                )
                call.respondBack(pluralName)
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
    val currentTranslator = call.translator
    val parametersDataResponse = call.receiveMultipart().toTableValues(panel, translator = currentTranslator)
    parametersDataResponse.onSuccess { parametersData ->
        val parameters = parametersData.map { it?.first }
        val fields = panel.getAllAllowToShowFieldsInUpsert()
        val fieldsWithParameterObject = fields.mapIndexed { index, field ->
            field to parametersData.getOrNull(index)?.second
        }.toMap()
        // Validate parameters
        val isValidParameters = fields.validateFieldsParameters(parameters)
        if (isValidParameters) {
            runCatching {
                val id = MongoClientRepository.insertData(fieldsWithParameterObject, panel)
                onMongoInsert(
                    collectionName = panel.getCollectionName(),
                    objectPrimaryKey = id,
                    fieldSets = fields,
                    parametersData = parametersData
                )
                call.respondBack(pluralName)
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