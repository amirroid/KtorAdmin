package ir.amirroid.ktoradmin.modules.add

import ir.amirroid.ktoradmin.authentication.KtorAdminPrincipal
import ir.amirroid.ktoradmin.csrf.CsrfManager
import ir.amirroid.ktoradmin.flash.getFlashDataAndClear
import ir.amirroid.ktoradmin.flash.getRequestId
import ir.amirroid.ktoradmin.utils.badRequest
import ir.amirroid.ktoradmin.utils.notFound
import ir.amirroid.ktoradmin.getters.getReferencesItems
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import ir.amirroid.ktoradmin.models.PanelGroup
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.panels.getAllAllowToShowColumnsInUpsertView
import ir.amirroid.ktoradmin.panels.getAllAllowToShowFieldsInUpsert
import ir.amirroid.ktoradmin.panels.hasAddAction
import ir.amirroid.ktoradmin.response.toMap
import ir.amirroid.ktoradmin.translator.KtorAdminTranslator
import ir.amirroid.ktoradmin.translator.translator
import ir.amirroid.ktoradmin.utils.Constants
import ir.amirroid.ktoradmin.utils.addCommonModels
import ir.amirroid.ktoradmin.utils.addCommonUpsertModels
import ir.amirroid.ktoradmin.utils.serverError
import ir.amirroid.ktoradmin.validators.checkHasRole

internal suspend fun ApplicationCall.handleAddNewItem(panels: List<AdminPanel>, panelGroups: List<PanelGroup>) {
    val pluralName = parameters["pluralName"]
    val panel = panels.find { it.getPluralName() == pluralName }
    if (panel == null || panel.isShowInAdminPanel().not()) {
        notFound("No table found with plural name: $pluralName")
    } else {
        checkHasRole(panel) {
            when (panel) {
                is AdminJdbcTable -> handleJdbcAddView(table = panel, panels = panels, panelGroups = panelGroups)
                is AdminMongoCollection -> handleNoSqlAddView(panel = panel, panelGroups = panelGroups)
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
    val translator = translator
    runCatching {
        val username = principal<KtorAdminPrincipal>()?.name
        val columns = table.getAllAllowToShowColumnsInUpsertView()
        val referencesItems = getReferencesItems(panels.filterIsInstance<AdminJdbcTable>(), columns)
        val singularTableName = table.getSingularName().replaceFirstChar { it.uppercaseChar() }
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_upsert.vm", model = mapOf(
                    "columns" to columns,
                    "tableName" to table.getTableName(),
                    "singularTableName" to singularTableName,
                    "references" to referencesItems,
                    "errors" to (valuesWithErrors.second?.toMap() ?: emptyMap()),
                    "values" to (valuesWithErrors.first ?: emptyMap()),
                    "csrfToken" to CsrfManager.generateToken(),
                    "panelGroups" to panelGroups,
                    "currentPanel" to table.getPluralName(),
                    "isUpdate" to false,
                    "requestId" to requestId,
                    "hasAction" to table.hasAddAction,
                    "title" to translator.translate(
                        KtorAdminTranslator.Keys.ADD_NEW_ITEM,
                        mapOf("name" to singularTableName)
                    )
                ).addCommonUpsertModels(table, username).toMutableMap()
                    .addCommonModels(table, panelGroups, applicationCall = this)
            )
        )
    }.onFailure {
        badRequest("Error: ${it.message}", it)
    }
}

internal suspend fun ApplicationCall.handleNoSqlAddView(
    panel: AdminMongoCollection, panelGroups: List<PanelGroup>
) {
    runCatching {
        val username = principal<KtorAdminPrincipal>()?.name
        val fields = panel.getAllAllowToShowFieldsInUpsert()
        val requestId = getRequestId()
        val valuesWithErrors = getFlashDataAndClear(requestId)
        val errors = valuesWithErrors.second ?: emptyList()
        val errorValues = valuesWithErrors.first
        val values = errorValues?.takeIf { it.isNotEmpty() } ?: emptyMap()
        val singularTableName = panel.getSingularName().replaceFirstChar { it.uppercaseChar() }
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_no_sql_upsert.vm", model = mutableMapOf(
                    "fields" to fields,
                    "collectionName" to panel.getCollectionName(),
                    "singularName" to singularTableName,
                    "values" to values,
                    "errors" to errors.toMap(),
                    "csrfToken" to CsrfManager.generateToken(),
                    "requestId" to requestId,
                    "hasAction" to panel.hasAddAction,
                    "panelGroups" to panelGroups,
                    "currentPanel" to panel.getPluralName(),
                    "title" to translator.translate(
                        KtorAdminTranslator.Keys.ADD_NEW_ITEM,
                        mapOf("name" to singularTableName)
                    )
                ).addCommonUpsertModels(panel, username).toMutableMap()
                    .addCommonModels(panel, panelGroups, applicationCall = this)
            )
        )
    }.onFailure {
        serverError("Error: ${it.message}", it)
    }
}