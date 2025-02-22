package modules.confirmation

import authentication.KtorAdminPrincipal
import csrf.CsrfManager
import flash.getFlashDataAndClear
import flash.getRequestId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import models.ColumnSet
import models.PanelGroup
import modules.update.handleNoSqlEditView
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import panels.hasEditAction
import response.toMap
import utils.Constants
import utils.badRequest
import utils.serverError
import validators.checkHasRole

internal suspend fun ApplicationCall.handleGetConfirmation(
    panels: List<AdminPanel>,
    panelGroups: List<PanelGroup>
) {
    val field = parameters["field"]
    val pluralName = parameters["pluralName"]
    val primaryKey = parameters["primaryKey"]

    val panel = panels.find { it.getPluralName() == pluralName }

    when {
        panel == null || !panel.isShowInAdminPanel() ->
            respondText("No table or collection found with plural name: $pluralName", status = HttpStatusCode.NotFound)

        primaryKey == null ->
            respondText("Primary key is missing for: $pluralName", status = HttpStatusCode.BadRequest)

        else -> {
            checkHasRole(panel) {
                when (panel) {
                    is AdminJdbcTable -> {
                        val column = panel.getAllColumns().firstOrNull { it.columnName == field }
                        if (column != null) {
                            handleJdbcConfirmationEditView(column, primaryKey, panel, panelGroups = panelGroups)
                        } else {
                            badRequest("Invalid column name: $field in table $pluralName")
                        }
                    }

                    is AdminMongoCollection -> {
                        val mongoField = panel.getAllFields().firstOrNull { it.fieldName == field }
                        if (mongoField != null) {
                            handleNoSqlEditView(primaryKey, panel, panelGroups = panelGroups)
                        } else {
                            badRequest("Invalid field name: $field in collection $pluralName")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun ApplicationCall.handleJdbcConfirmationEditView(
    column: ColumnSet,
    primaryKey: String,
    table: AdminJdbcTable,
    panelGroups: List<PanelGroup> = emptyList(),
) {
    runCatching {
        val username = principal<KtorAdminPrincipal>()?.name
        val requestId = getRequestId()
        val valuesWithErrors = getFlashDataAndClear(requestId)
        val errorValues = valuesWithErrors.first
        val errors = valuesWithErrors.second ?: emptyList()
        val values = errorValues?.takeIf { it.isNotEmpty() } ?: emptyMap()
        val columns = listOf(
            column.copy(verboseName = "New ${column.verboseName}"),
            column.copy(
                columnName = column.columnName + ".confirmation",
                verboseName = "Confirm new ${column.verboseName}"
            ),
        )
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_upsert.vm", model = mutableMapOf(
                    "columns" to columns,
                    "tableName" to table.getTableName(),
                    "primaryKey" to primaryKey,
                    "canDownload" to false,
                    "values" to values,
                    "singularTableName" to table.getSingularName()
                        .replaceFirstChar { it.uppercaseChar() },
                    "pluralNameBase" to table.getPluralName(),
                    "errors" to errors.toMap(),
                    "csrfToken" to CsrfManager.generateToken(),
                    "panelGroups" to panelGroups,
                    "currentPanel" to table.getPluralName(),
                    "isUpdate" to false,
                    "requestId" to requestId,
                    "hasAction" to table.hasEditAction,
                    "title" to "Reset ${column.verboseName}",
                    "callMethod" to "application/x-www-form-urlencoded"
                ).apply {
                    username?.let { put("username", it) }
                }
            )
        )
    }.onFailure {
        serverError("Error: ${it.message}", it)
    }
}