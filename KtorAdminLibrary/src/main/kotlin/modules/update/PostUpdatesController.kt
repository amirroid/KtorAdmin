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
import repository.JdbcQueriesRepository
import panels.AdminJdbcTable
import panels.AdminPanel
import panels.findWithPluralName
import panels.getAllAllowToShowColumns


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


internal suspend fun RoutingContext.handleUpdateRequest(tables: List<AdminPanel>) {
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
    when (table) {
        is AdminJdbcTable -> updateData(pluralName, primaryKey, table)
    }
}

private suspend fun RoutingContext.updateData(
    pluralName: String?, primaryKey: String, table: AdminJdbcTable
) {
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