package modules.update

import utils.notFound
import utils.serverError
import configuration.DynamicConfiguration
import converters.toEvents
import converters.toFieldEvents
import converters.toTableValues
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.ColumnSet
import models.field.FieldSet
import panels.*
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import response.onError
import response.onSuccess
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
    if (panel == null) {
        call.notFound("No table found with plural name: $pluralName")
        return
    }
    call.checkHasRole(panel) {
        when (panel) {
            is AdminJdbcTable -> updateData(pluralName, primaryKey, panel, panels)
            is AdminMongoCollection -> updateData(pluralName, primaryKey, panel, panels)
        }
    }
}

private suspend fun RoutingContext.updateData(
    pluralName: String?, primaryKey: String, table: AdminJdbcTable, panels: List<AdminPanel>
) {
    val columns = table.getAllAllowToShowColumnsInUpsert()
    val initialData = JdbcQueriesRepository.getData(table, primaryKey)
    val parametersDataResponse = call.receiveMultipart().toTableValues(table, initialData)
    parametersDataResponse.onSuccess { parametersData ->
        val parameters = parametersData.map { it?.first }

        kotlin.runCatching {
            val changedDataAndId = JdbcQueriesRepository.updateChangedData(table, parameters, primaryKey, initialData)
            onUpdate(
                tableName = table.getTableName(),
                objectPrimaryKey = changedDataAndId?.first?.toString() ?: primaryKey,
                changedColumns = changedDataAndId?.second ?: emptyList(),
                columnSets = columns,
                parametersData = parametersData
            )
            call.respondRedirect("/admin/$pluralName")
        }.onFailure {
            call.serverError("Failed to update $pluralName\nReason: ${it.message}", it)
        }
    }.onError { errors, values ->
        call.handleJdbcEditView(primaryKey, table, panels, errors, values)
    }
}

private suspend fun RoutingContext.updateData(
    pluralName: String?, primaryKey: String, panel: AdminMongoCollection, panels: List<AdminPanel>
) {
    val initialData = MongoClientRepository.getData(panel, primaryKey)
    val fields = panel.getAllAllowToShowFieldsInUpsert()

    val parametersDataResponse = call.receiveMultipart().toTableValues(panel, initialData)
    parametersDataResponse.onSuccess { parametersData ->
        val parameters = parametersData.map { it?.first }
        val fieldsWithParameter = fields.mapIndexed { index, field ->
            field to parameters.getOrNull(index)
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
            call.respondRedirect("/admin/$pluralName")
        }.onFailure {
            call.serverError("Failed to update $pluralName\nReason: ${it.message}", throwable = it)
        }
    }.onError { errors, values ->
        call.handleNoSqlEditView(
            primaryKey = primaryKey,
            panel = panel,
            panels = panels,
            errors = errors,
            errorValues = values
        )
    }
}