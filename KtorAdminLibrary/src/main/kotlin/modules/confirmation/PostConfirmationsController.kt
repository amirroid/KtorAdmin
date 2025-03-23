package modules.confirmation

import configuration.DynamicConfiguration
import csrf.CSRF_TOKEN_FIELD_NAME
import csrf.CsrfManager
import flash.REQUEST_ID_FORM
import flash.setFlashSessionsAndRedirect
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import models.ColumnSet
import models.events.ColumnEvent
import models.events.FieldEvent
import models.field.FieldSet
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import panels.hasEditAction
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import response.ErrorResponse
import translator.translator
import utils.badRequest
import utils.invalidateRequest
import utils.respondBack
import utils.serverError
import validators.Validators
import validators.checkHasRole

/**
 * Handles saving confirmation data for a given field in an admin panel.
 *
 * @param panels The list of available admin panels.
 */
internal suspend fun RoutingContext.handleSaveConfirmation(panels: List<AdminPanel>) {
    val field = call.parameters["field"]
    val pluralName = call.parameters["pluralName"]
    val primaryKey = call.parameters["primaryKey"]

    val panel = panels.find { it.getPluralName() == pluralName }

    when {
        // Panel not found or not visible in the admin panel
        panel == null || !panel.isShowInAdminPanel() ->
            call.respondText(
                "No table or collection found with plural name: $pluralName",
                status = HttpStatusCode.NotFound
            )

        // Primary key is missing
        primaryKey == null ->
            call.respondText("Primary key is missing for: $pluralName", status = HttpStatusCode.BadRequest)

        else -> {
            if (!panel.hasEditAction) {
                call.badRequest("Edit action is disabled")
                return
            }

            call.checkHasRole(panel) {
                val parameters = call.receiveParameters()
                val csrfToken = parameters[CSRF_TOKEN_FIELD_NAME]

                // Validate CSRF token
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
                            call.badRequest("Invalid column name: $field in table $pluralName")
                        }
                    }

                    is AdminMongoCollection -> {
                        val mongoField = panel.getAllFields().firstOrNull { it.fieldName == field }
                        if (mongoField != null) {
                            updateFieldData(
                                pluralName = pluralName!!,
                                fieldSet = mongoField,
                                panel = panel,
                                params = parameters.toMap(),
                                primaryKey = primaryKey
                            )
                        } else {
                            call.badRequest("Invalid field name: $field in collection $pluralName")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Updates a column value in the database and handles validation.
 *
 * @param pluralName The plural name of the table.
 * @param columnSet The column to be updated.
 * @param table The table containing the column.
 * @param params The parameters received in the request.
 * @param primaryKey The primary key of the record to update.
 */
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

    // Validate the column value
    val currentTranslator = call.translator
    val validateColumn =
        Validators.validateColumnParameter(table, columnSet, value, primaryKey, translator = currentTranslator)
    val isSameValues = value == confirmValue

    if (isSameValues && validateColumn == null) {
        kotlin.runCatching {
            // Update the column value in the database
            JdbcQueriesRepository.updateAColumn(
                table = table,
                columnSet = columnSet,
                value = value,
                primaryKey = primaryKey
            )

            // Notify event listeners about the update
            DynamicConfiguration.currentEventListener?.onUpdateJdbcData(
                table.getTableName(),
                primaryKey,
                listOf(ColumnEvent(true, columnSet, value))
            )

            // Redirect back after successful update
            call.respondBack(pluralName)
        }.onFailure {
            call.serverError("Failed to update $pluralName. Reason: ${it.localizedMessage}", it)
        }
    } else {
        // Collect validation errors
        val errors = mutableListOf<ErrorResponse>()
        if (validateColumn != null) {
            errors.add(ErrorResponse(confirmationColumnName, listOf(validateColumn)))
        }
        if (!isSameValues) {
            errors.add(ErrorResponse(confirmationColumnName, listOf("The confirmation value does not match.")))
        }

        // Store input values for potential correction
        val values = mutableMapOf(
            columnSet.columnName to value,
            confirmationColumnName to confirmValue
        )

        // Redirect with validation errors
        call.setFlashSessionsAndRedirect(requestId, errors, values)
    }
}

private suspend fun RoutingContext.updateFieldData(
    pluralName: String,
    fieldSet: FieldSet,
    panel: AdminMongoCollection,
    params: Map<String, List<String>>,
    primaryKey: String
) {
    val value = params[fieldSet.fieldName]?.firstOrNull()
    val confirmationColumnName = "${fieldSet.fieldName}.confirmation"
    val confirmValue = params[confirmationColumnName]?.firstOrNull()
    val requestId = params[REQUEST_ID_FORM]?.firstOrNull()

    // Validate the field value
    val currentTranslator = call.translator
    val validateField = Validators.validateFieldParameter(fieldSet, value, translator = currentTranslator)
    val isSameValues = value == confirmValue

    if (isSameValues && validateField == null) {
        kotlin.runCatching {
            // Update the field value in the database
            MongoClientRepository.updateConfirmation(
                panel = panel,
                primaryKey = primaryKey,
                field = fieldSet,
                value = value,
            )

            // Notify event listeners about the update
            DynamicConfiguration.currentEventListener?.onUpdateMongoData(
                panel.getCollectionName(),
                primaryKey,
                listOf(FieldEvent(true, fieldSet, value))
            )

            // Redirect back after successful update
            call.respondBack(pluralName)
        }.onFailure {
            call.serverError("Failed to update $pluralName. Reason: ${it.localizedMessage}", it)
        }
    } else {
        // Collect validation errors
        val errors = mutableListOf<ErrorResponse>()
        if (validateField != null) {
            errors.add(ErrorResponse(confirmationColumnName, listOf(validateField)))
        }
        if (!isSameValues) {
            errors.add(ErrorResponse(confirmationColumnName, listOf("The confirmation value does not match.")))
        }

        // Store input values for potential correction
        val values = mutableMapOf(
            fieldSet.fieldName.orEmpty() to value,
            confirmationColumnName to confirmValue
        )

        // Redirect with validation errors
        call.setFlashSessionsAndRedirect(requestId, errors, values)
    }
}