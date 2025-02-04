package modules.add

import authentication.KtorAdminPrincipal
import csrf.CsrfManager
import flash.KtorFlashHelper
import flash.getFlashDataAndClear
import flash.getRequestId
import utils.badRequest
import utils.notFound
import getters.getReferencesItems
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import io.ktor.util.AttributeKey
import models.PanelGroup
import panels.*
import response.ErrorResponse
import response.toMap
import utils.Constants
import validators.checkHasRole
import kotlin.math.log

internal suspend fun ApplicationCall.handleAddNewItem(panels: List<AdminPanel>, panelGroups: List<PanelGroup>) {
    val pluralName = parameters["pluralName"]
    val panel = panels.find { it.getPluralName() == pluralName }
    if (panel == null) {
        notFound("No table found with plural name: $pluralName")
    } else {
        checkHasRole(panel) {
            when (panel) {
                is AdminJdbcTable -> handleJdbcAddView(table = panel, panels = panels, panelGroups = panelGroups)
                is AdminMongoCollection -> handleNoSqlAddView(panel = panel)
            }
        }
    }
}

internal suspend fun ApplicationCall.handleJdbcAddView(
    table: AdminJdbcTable,
    panels: List<AdminPanel>,
    panelGroups: List<PanelGroup> = emptyList(),
) {
    val requestId = getRequestId()
    val valuesWithErrors = getFlashDataAndClear(requestId)
    runCatching {
        val user = principal<KtorAdminPrincipal>()!!
        val columns = table.getAllAllowToShowColumnsInUpsert()
        val referencesItems = getReferencesItems(panels.filterIsInstance<AdminJdbcTable>(), columns)
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_upsert.vm", model = mapOf(
                    "columns" to columns,
                    "tableName" to table.getTableName(),
                    "singularTableName" to table.getSingularName().replaceFirstChar { it.uppercaseChar() },
                    "references" to referencesItems,
                    "errors" to (valuesWithErrors.second?.toMap() ?: emptyMap()),
                    "values" to (valuesWithErrors.first ?: emptyMap()),
                    "csrfToken" to CsrfManager.generateToken(),
                    "panelGroups" to panelGroups,
                    "currentPanel" to table.getPluralName(),
                    "username" to user.name,
                    "isUpdate" to false,
                    "requestId" to requestId,
                )
            )
        )
    }.onFailure {
        badRequest("Error: ${it.message}", it)
    }
}

internal suspend fun ApplicationCall.handleNoSqlAddView(
    panel: AdminMongoCollection,
    values: Map<String, String?> = emptyMap(),
    errors: List<ErrorResponse> = emptyList()
) {
    runCatching {
        val fields = panel.getAllAllowToShowFieldsInUpsert()
//        val referencesItems = getReferencesItems(tables.filterIsInstance<AdminJdbcTable>(), columns)
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin2.vm", model = mapOf(
                    "fields" to fields,
                    "singularTableName" to panel.getSingularName()
                        .replaceFirstChar { it.uppercaseChar() },
                    "collectionName" to panel.getCollectionName(),
                    "singularName" to panel.getSingularName().replaceFirstChar { it.uppercaseChar() },
                    "values" to values,
                    "errors" to errors.toMap(),
                    "csrfToken" to CsrfManager.generateToken()
                )
            )
        )
    }.onFailure {
        badRequest("Error: ${it.message}", it)
    }
}