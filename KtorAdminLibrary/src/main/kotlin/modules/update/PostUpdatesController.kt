package modules.update

import configuration.DynamicConfiguration
import converters.toEvents
import converters.toFieldEvents
import converters.toTableValues
import flash.setFlashSessionsAndRedirect
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.ColumnSet
import models.field.FieldSet
import models.response.updateSelectedReferences
import panels.*
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import response.onError
import response.onInvalidateRequest
import response.onSuccess
import translator.translator
import utils.badRequest
import utils.invalidateRequest
import utils.notFound
import utils.respondBack
import utils.serverError
import validators.checkHasRole


private suspend fun onUpdate(
    tableName: String,
    objectPrimaryKey: String,
    changedColumns: List<String>,
    columnSets: List<ColumnSet>,
    parametersData: List<Pair<String, Any?>?>
) {
    DynamicConfiguration.currentEventListener?.onUpdateJdbcData(
        tableName = tableName,
        objectPrimaryKey = objectPrimaryKey,
        events = columnSets.toEvents(parametersData.map { it?.second }, changedColumns = changedColumns)
    )
}


private suspend fun onMongoUpdate(
    collectionName: String,
    objectPrimaryKey: String,
    changedFields: List<String>,
    fieldSets: List<FieldSet>,
    parametersData: List<Pair<String, Any?>?>
) {
    DynamicConfiguration.currentEventListener?.onUpdateMongoData(
        collectionName = collectionName,
        objectPrimaryKey = objectPrimaryKey,
        events = fieldSets.toFieldEvents(parametersData.map { it?.second }, changedFields = changedFields)
    )
}


internal suspend fun RoutingContext.handleUpdateRequest(panels: List<AdminPanel>) {
    val pluralName = call.parameters["pluralName"]
    val primaryKey = call.parameters["primaryKey"]

    if (pluralName == null || primaryKey == null) {
        call.notFound("Invalid table or primary key")
        return
    }
    val panel = panels.findWithPluralName(pluralName)
    if (panel == null || panel.isShowInAdminPanel().not()) {
        call.notFound("No table found with plural name: $pluralName")
        return
    }
    if (panel.hasEditAction.not()) {
        call.badRequest("Edit action is disabled")
        return
    }
    call.checkHasRole(panel) {
        kotlin.runCatching {
            when (panel) {
                is AdminJdbcTable -> updateData(pluralName, primaryKey, panel, panels)
                is AdminMongoCollection -> updateData(pluralName, primaryKey, panel, panels)
            }
        }.onFailure {
            serverError(it.message.orEmpty(), it)
        }
    }
}


private suspend fun RoutingContext.updateData(
    pluralName: String?, primaryKey: String, table: AdminJdbcTable, panels: List<AdminPanel>
) {
    val tables = panels.filterIsInstance<AdminJdbcTable>()
    val columns = table.getAllAllowToShowColumnsInUpsert()
    val initialData = JdbcQueriesRepository.getData(table, primaryKey)
    val currentTranslator = call.translator
    val parametersDataResponse =
        call.receiveMultipart().toTableValues(table, initialData, primaryKey, translator = currentTranslator)
    parametersDataResponse.onSuccess { parametersData ->
        kotlin.runCatching {
            val changedDataAndId =
                JdbcQueriesRepository.updateChangedData(table, parametersData.values, primaryKey, initialData)
            parametersData.updateSelectedReferences(table, tables, primaryKey)
            onUpdate(
                tableName = table.getTableName(),
                objectPrimaryKey = changedDataAndId?.first?.toString() ?: primaryKey,
                changedColumns = changedDataAndId?.second ?: emptyList(),
                columnSets = columns,
                parametersData = parametersData.values
            )
            call.respondBack(pluralName)
        }.onFailure {
            call.serverError("Failed to update $pluralName\nReason: ${it.message}", it)
        }
    }.onError { requestId, errors, values ->
        call.setFlashSessionsAndRedirect(requestId, errors, values)
    }.onInvalidateRequest {
        call.invalidateRequest()
    }
}

private suspend fun RoutingContext.updateData(
    pluralName: String?, primaryKey: String, panel: AdminMongoCollection, panels: List<AdminPanel>
) {
    val initialData = MongoClientRepository.getData(panel, primaryKey)
    val fields = panel.getAllAllowToShowFieldsInUpsert()

    val currentTranslator = call.translator
    val parametersDataResponse =
        call.receiveMultipart().toTableValues(panel, initialData, translator = currentTranslator)
    parametersDataResponse.onSuccess { parametersData ->
        val fieldsWithParameter = fields.mapIndexed { index, field ->
            field to parametersData.getOrNull(index)
        }.toMap()
        kotlin.runCatching {
            val changedDataAndId =
                MongoClientRepository.updateChangedData(panel, fieldsWithParameter, primaryKey, initialData)
            onMongoUpdate(
                collectionName = panel.getCollectionName(),
                objectPrimaryKey = changedDataAndId?.first ?: primaryKey,
                changedFields = changedDataAndId?.second ?: emptyList(),
                fieldSets = fields,
                parametersData = parametersData
            )
            call.respondBack(pluralName)
        }.onFailure {
            call.serverError("Failed to update $pluralName\nReason: ${it.message}", throwable = it)
        }
    }.onError { requestId, errors, values ->
        call.setFlashSessionsAndRedirect(requestId, errors, values)
    }.onInvalidateRequest {
        call.invalidateRequest()
    }
}