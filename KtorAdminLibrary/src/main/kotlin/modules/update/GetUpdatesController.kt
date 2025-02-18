package modules.update

import authentication.KtorAdminPrincipal
import configuration.DynamicConfiguration
import csrf.CsrfManager
import flash.getFlashDataAndClear
import flash.getRequestId
import utils.notFound
import utils.serverError
import getters.getReferencesItems
import getters.getSelectedReferencesItems
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import models.ColumnSet
import models.PanelGroup
import models.field.FieldSet
import models.types.ColumnType
import models.types.FieldType
import panels.*
import repository.FileRepository
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import response.ErrorResponse
import response.toMap
import utils.Constants
import utils.addCommonUpsertModels
import validators.checkHasRole
import kotlin.collections.toMap


internal suspend fun ApplicationCall.handleEditItem(
    panels: List<AdminPanel>,
    panelGroups: List<PanelGroup>
) {
    val pluralName = parameters["pluralName"]
    val primaryKey = parameters["primaryKey"]
    val panel = panels.find { it.getPluralName() == pluralName }
    when {
        panel == null -> respondText { "No table found with plural name: $pluralName" }
        primaryKey == null -> respondText { "No primary key found: $pluralName" }
        else -> {
            checkHasRole(panel) {
                when (panel) {
                    is AdminJdbcTable -> handleJdbcEditView(primaryKey, panel, panels, panelGroups = panelGroups)
                    is AdminMongoCollection -> handleNoSqlEditView(primaryKey, panel, panelGroups = panelGroups)
                }
            }
        }
    }
}

internal suspend fun ApplicationCall.handleJdbcEditView(
    primaryKey: String,
    table: AdminJdbcTable,
    panels: List<AdminPanel>,
    panelGroups: List<PanelGroup> = emptyList(),
) {
    val data = JdbcQueriesRepository.getData(table, primaryKey)
    if (data == null) {
        notFound("No data found with primary key: $primaryKey")
    } else {
        runCatching {
            val user = principal<KtorAdminPrincipal>()!!
            val columns = table.getAllAllowToShowColumnsInUpsert()
            val panelColumnsList = table.getAllAllowToShowColumnsInUpsertView()
            val referencesItems = getReferencesItems(panels.filterIsInstance<AdminJdbcTable>(), panelColumnsList)
            val selectedReferences =
                getSelectedReferencesItems(table, panels.filterIsInstance<AdminJdbcTable>(), primaryKey)
            val requestId = getRequestId()
            val valuesWithErrors = getFlashDataAndClear(requestId)
            val errorValues = valuesWithErrors.first
            val errors = valuesWithErrors.second ?: emptyList()
            val values = errorValues?.takeIf { it.isNotEmpty() } ?: columns.mapIndexed { index, column ->
                column.columnName to data[index]?.let { item ->
                    handlePreviewValue(
                        column,
                        item,
                        this
                    )
                }
            }.toMap()
            respond(
                VelocityContent(
                    "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_upsert.vm", model = mapOf(
                        "columns" to panelColumnsList,
                        "tableName" to table.getTableName(),
                        "primaryKey" to primaryKey,
                        "canDownload" to DynamicConfiguration.canDownloadDataAsPdf,
                        "values" to values,
                        "singularTableName" to table.getSingularName()
                            .replaceFirstChar { it.uppercaseChar() },
                        "references" to referencesItems,
                        "selectedReferences" to selectedReferences,
                        "pluralNameBase" to table.getPluralName(),
                        "errors" to errors.toMap(),
                        "csrfToken" to CsrfManager.generateToken(),
                        "panelGroups" to panelGroups,
                        "currentPanel" to table.getPluralName(),
                        "username" to user.name,
                        "isUpdate" to true,
                        "requestId" to requestId,
                        "hasAction" to table.hasEditAction
                    ).addCommonUpsertModels(table)
                )
            )
        }.onFailure {
            serverError("Error: ${it.message}", it)
        }
    }
}


internal suspend fun ApplicationCall.handleNoSqlEditView(
    primaryKey: String,
    panel: AdminMongoCollection,
    panelGroups: List<PanelGroup>
) {
    val data = MongoClientRepository.getData(panel, primaryKey)
    if (data == null) {
        notFound("No data found with primary key: $primaryKey")
    } else {
        runCatching {
            val user = principal<KtorAdminPrincipal>()!!
            val fields = panel.getAllAllowToShowFieldsInUpsert()
            val requestId = getRequestId()
            val valuesWithErrors = getFlashDataAndClear(requestId)
            val errors = valuesWithErrors.second ?: emptyList()
            val errorValues = valuesWithErrors.first
            val values = errorValues?.takeIf { it.isNotEmpty() } ?: fields.mapIndexed { index, field ->
                field.fieldName to data[index]?.let { item ->
                    handlePreviewValue(
                        field,
                        item,
                        this
                    )
                }
            }.toMap()
            respond(
                VelocityContent(
                    "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_no_sql_upsert.vm", model = mapOf(
                        "fields" to fields,
                        "values" to values,
                        "errors" to errors.toMap(),
                        "collectionName" to panel.getCollectionName(),
                        "singularName" to panel.getSingularName().replaceFirstChar { it.uppercaseChar() },
                        "csrfToken" to CsrfManager.generateToken(),
                        "panelGroups" to panelGroups,
                        "currentPanel" to panel.getPluralName(),
                        "username" to user.name,
                        "isUpdate" to true,
                        "requestId" to requestId,
                        "hasAction" to panel.hasEditAction,
                        "canDownload" to DynamicConfiguration.canDownloadDataAsPdf,
                    )
                )
            )
        }.onFailure {
            serverError("Error: ${it.message}", it)
        }
    }
}

suspend fun handlePreviewValue(columnSet: ColumnSet, value: String, call: ApplicationCall) = when (columnSet.type) {
    ColumnType.FILE -> FileRepository.generateMediaUrl(columnSet.uploadTarget!!, value, call)
    else -> value
}

suspend fun handlePreviewValue(fieldSet: FieldSet, value: String, call: ApplicationCall) = when (fieldSet.type) {
    FieldType.File -> FileRepository.generateMediaUrl(fieldSet.uploadTarget!!, value, call)
    else -> value
}
