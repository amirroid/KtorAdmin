package modules.update

import authentication.KtorAdminPrincipal
import csrf.CsrfManager
import flash.getFlashDataAndClear
import flash.getRequestId
import utils.notFound
import utils.serverError
import getters.getReferencesItems
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
import validators.checkHasRole


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
                    is AdminMongoCollection -> handleNoSqlEditView(primaryKey, panel, panels)
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
            val referencesItems = getReferencesItems(panels.filterIsInstance<AdminJdbcTable>(), columns)
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
                        "columns" to columns,
                        "tableName" to table.getTableName(),
                        "values" to values,
                        "singularTableName" to table.getSingularName()
                            .replaceFirstChar { it.uppercaseChar() },
                        "references" to referencesItems,
                        "errors" to errors.toMap(),
                        "csrfToken" to CsrfManager.generateToken(),
                        "panelGroups" to panelGroups,
                        "currentPanel" to table.getPluralName(),
                        "username" to user.name,
                        "isUpdate" to true,
                        "requestId" to requestId,
                        "hasAction" to table.hasEditAction
                    )
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
    panels: List<AdminPanel>,
    errors: List<ErrorResponse> = emptyList(),
    errorValues: Map<String, String?> = emptyMap()
) {
    val data = MongoClientRepository.getData(panel, primaryKey)
    if (data == null) {
        notFound("No data found with primary key: $primaryKey")
    } else {
        runCatching {
            val fields = panel.getAllAllowToShowFieldsInUpsert()
//            val referencesItems = getReferencesItems(tables.filterIsInstance<AdminJdbcTable>(), columns)
            val values = errorValues.takeIf { it.isNotEmpty() } ?: fields.mapIndexed { index, field ->
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
                    "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin2.vm", model = mapOf(
                        "fields" to fields,
                        "values" to values,
                        "errors" to errors.toMap(),
                        "singularTableName" to panel.getSingularName()
                            .replaceFirstChar { it.uppercaseChar() },
//                        "references" to referencesItems,
                        "collectionName" to panel.getCollectionName(),
                        "singularName" to panel.getSingularName().replaceFirstChar { it.uppercaseChar() },
                        "csrfToken" to CsrfManager.generateToken()
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
