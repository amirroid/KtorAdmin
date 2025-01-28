package modules.update

import utils.notFound
import utils.serverError
import configuration.DynamicConfiguration
import converters.toEvents
import converters.toTableValues
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.ColumnSet
import panels.*
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import response.onError
import response.onSuccess


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
    when (panel) {
        is AdminJdbcTable -> updateData(pluralName, primaryKey, panel, panels)
        is AdminMongoCollection -> updateData(pluralName, primaryKey, panel)
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
        println("PARAMETERS : $parameters")

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
            call.serverError("Failed to update $pluralName\nReason: ${it.message}")
        }
    }.onError { errors, values ->
        call.handleJdbcEditView(primaryKey, table, panels, errors, values)
    }
}

private suspend fun RoutingContext.updateData(
    pluralName: String?, primaryKey: String, panel: AdminMongoCollection
) {
    val fields = panel.getAllAllowToShowFieldsInUpsert()

    val parametersData = call.receiveMultipart().toTableValues(panel)
    val parameters = parametersData.map { it?.first }
    val fieldsWithParameter = fields.mapIndexed { index, field ->
        field to parameters.getOrNull(index)
    }.toMap()
    println("PARAMETERS : $parametersData")
    kotlin.runCatching {
        val changedDataAndId = MongoClientRepository.updateChangedData(panel, fieldsWithParameter, primaryKey)
        call.respondRedirect("/admin/$pluralName")
    }.onFailure {
        call.serverError("Failed to update $pluralName\nReason: ${it.message}")
    }
}