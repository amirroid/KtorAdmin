package modules.confirmation

import configuration.DynamicConfiguration
import converters.toTableValues
import csrf.CSRF_TOKEN_FIELD_NAME
import csrf.CsrfManager
import flash.REQUEST_ID
import flash.REQUEST_ID_FORM
import flash.setFlashSessionsAndRedirect
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.util.toMap
import models.ColumnSet
import models.PanelGroup
import models.events.ColumnEvent
import models.response.updateSelectedReferences
import modules.update.handleNoSqlEditView
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import panels.getAllAllowToShowColumnsInUpsert
import repository.JdbcQueriesRepository
import response.ErrorResponse
import response.onError
import response.onInvalidateRequest
import response.onSuccess
import utils.badRequest
import utils.invalidateRequest
import utils.respondBack
import utils.serverError
import validators.Validators
import validators.checkHasRole

internal suspend fun RoutingContext.handleSaveConfirmation(
    panels: List<AdminPanel>,
) {
    val field = call.parameters["field"]
    val pluralName = call.parameters["pluralName"]
    val primaryKey = call.parameters["primaryKey"]

    val panel = panels.find { it.getPluralName() == pluralName }

    when {
        panel == null || !panel.isShowInAdminPanel() ->
            call.respondText(
                "No table or collection found with plural name: $pluralName",
                status = HttpStatusCode.NotFound
            )

        primaryKey == null ->
            call.respondText("Primary key is missing for: $pluralName", status = HttpStatusCode.BadRequest)

        else -> {
            call.checkHasRole(panel) {
                val parameters = call.receiveParameters()
                val csrfToken = parameters[CSRF_TOKEN_FIELD_NAME]
                if (!CsrfManager.validateToken(csrfToken)) {
                    return@checkHasRole call.invalidateRequest()
                }
                when (panel) {
                    is AdminJdbcTable -> {
                        val column = panel.getAllColumns().firstOrNull { it.columnName == field }
                        if (column != null) {
                            updateData(
                                pluralName = pluralName!!,
                                columnSet = column,
                                table = panel,
                                params = parameters.toMap(),
                                primaryKey = primaryKey
                            )
                        } else {
                            badRequest("Invalid column name: $field in table $pluralName")
                        }
                    }

                    is AdminMongoCollection -> {
                        val mongoField = panel.getAllFields().firstOrNull { it.fieldName == field }
                        if (mongoField != null) {
//                            handleNoSqlEditView(primaryKey, panel, panelGroups = panelGroups)
                        } else {
                            badRequest("Invalid field name: $field in collection $pluralName")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun RoutingContext.updateData(
    pluralName: String,
    columnSet: ColumnSet,
    table: AdminJdbcTable,
    params: Map<String, List<String>>,
    primaryKey: String
) {
    val value = params[columnSet.columnName]?.firstOrNull()
    val confirmationColumnName = "${columnSet.columnName}.confirmation"
    val confirmValue = params[confirmationColumnName]?.firstOrNull()
    val requestId = params[REQUEST_ID_FORM]?.firstOrNull()
    val validateColumn = Validators.validateColumnParameter(table, columnSet, value, primaryKey)
    val isSameValues = value == confirmValue

    if (isSameValues && validateColumn == null) {
        kotlin.runCatching {
            JdbcQueriesRepository.updateAColumn(
                table = table,
                columnSet = columnSet,
                value = value,
                primaryKey = primaryKey
            )
            DynamicConfiguration.currentEventListener?.onUpdateJdbcData(
                table.getTableName(),
                primaryKey,
                listOf(
                    ColumnEvent(true, columnSet, value)
                )
            )
            call.respondBack(pluralName)
        }.onFailure {
            call.serverError("Failed to update $pluralName. Reason: ${it.localizedMessage}", it)
        }
    } else {
        val errors = mutableListOf<ErrorResponse>()
        if (validateColumn != null) {
            errors.add(ErrorResponse(confirmationColumnName, listOf(validateColumn)))
        }
        if (!isSameValues) {
            errors.add(ErrorResponse(confirmationColumnName, listOf("The confirmation value does not match.")))
        }
        val values = mutableMapOf(
            columnSet.columnName to value,
            confirmationColumnName to confirmValue,
        )

        call.setFlashSessionsAndRedirect(requestId, errors, values)
    }
}