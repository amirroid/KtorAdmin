package ir.amirroid.ktoradmin.modules.update

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.csrf.CSRF_TOKEN_FIELD_NAME
import ir.amirroid.ktoradmin.csrf.CSRF_TOKEN_HEADER_NAME
import ir.amirroid.ktoradmin.csrf.CsrfManager
import ir.amirroid.ktoradmin.models.events.ColumnEvent
import ir.amirroid.ktoradmin.models.events.FieldEvent
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.panels.findWithPluralName
import ir.amirroid.ktoradmin.panels.hasEditAction
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository
import ir.amirroid.ktoradmin.repository.MongoClientRepository
import ir.amirroid.ktoradmin.translator.translator
import ir.amirroid.ktoradmin.validators.Validators
import ir.amirroid.ktoradmin.validators.checkHasRole

internal suspend fun RoutingContext.handlePatchRequest(panels: List<AdminPanel>) {
    val pluralName = call.parameters["pluralName"]
    val primaryKey = call.parameters["primaryKey"]

    if (pluralName == null || primaryKey == null) {
        call.respondJson(
            HttpStatusCode.BadRequest,
            mapOf("status" to "error", "message" to "Invalid table or primary key"),
        )
        return
    }

    val panel = panels.findWithPluralName(pluralName)
    if (panel == null || !panel.isShowInAdminPanel()) {
        call.respondJson(
            HttpStatusCode.NotFound,
            mapOf("status" to "error", "message" to "No table found with plural name: $pluralName"),
        )
        return
    }

    call.checkHasRole(panel) {
        if (!panel.hasEditAction) {
            call.respondJson(
                HttpStatusCode.BadRequest,
                mapOf("status" to "error", "message" to "Edit action is disabled"),
            )
            return@checkHasRole
        }

        val requestData = call.extractPatchData()

        val csrfToken =
            call.request.headers[CSRF_TOKEN_HEADER_NAME]
                ?: call.request.queryParameters[CSRF_TOKEN_FIELD_NAME]
                ?: requestData.csrfToken

        if (!CsrfManager.validateToken(csrfToken)) {
            call.respondJson(
                HttpStatusCode.Forbidden,
                mapOf("status" to "error", "message" to "Invalid CSRF token"),
            )
            return@checkHasRole
        }

        val fieldName = requestData.fieldName
        if (fieldName.isNullOrBlank()) {
            call.respondJson(
                HttpStatusCode.BadRequest,
                mapOf("status" to "error", "message" to "Field name is required"),
            )
            return@checkHasRole
        }

        val rawValue = requestData.value

        runCatching {
            when (panel) {
                is AdminJdbcTable -> patchJdbcData(pluralName, primaryKey, panel, fieldName, rawValue)
                is AdminMongoCollection -> patchMongoData(pluralName, primaryKey, panel, fieldName, rawValue)
            }
        }.onFailure { cause ->
            call.respondJson(
                HttpStatusCode.InternalServerError,
                mapOf("status" to "error", "message" to (cause.localizedMessage ?: "Failed to update record")),
            )
        }
    }
}

private suspend fun RoutingContext.patchJdbcData(
    pluralName: String,
    primaryKey: String,
    table: AdminJdbcTable,
    fieldName: String,
    value: String?,
) {
    val column = table.getAllColumns().firstOrNull { it.columnName == fieldName }
    if (column == null) {
        call.respondJson(
            HttpStatusCode.BadRequest,
            mapOf("status" to "error", "message" to "Invalid column name: $fieldName in table $pluralName"),
        )
        return
    }

    if (!column.isInlineEditable) {
        call.respondJson(
            HttpStatusCode.BadRequest,
            mapOf("status" to "error", "message" to "Column $fieldName cannot be edited inline"),
        )
        return
    }

    val currentTranslator = call.translator
    val validationError =
        Validators.validateColumnParameter(
            table = table,
            columnSet = column,
            value = value,
            primaryKey = primaryKey,
            translator = currentTranslator,
        )

    if (validationError != null) {
        call.respondJson(
            HttpStatusCode.BadRequest,
            mapOf("status" to "error", "message" to validationError),
        )
        return
    }

    JdbcQueriesRepository.updateAColumn(
        table = table,
        columnSet = column,
        value = value,
        primaryKey = primaryKey,
    )

    DynamicConfiguration.currentEventListener?.onUpdateJdbcData(
        tableName = table.getTableName(),
        objectPrimaryKey = primaryKey,
        events = listOf(ColumnEvent(true, column, value)),
    )

    call.respondJson(
        HttpStatusCode.OK,
        mapOf(
            "status" to "success",
            "field" to column.columnName,
            "value" to value,
            "displayValue" to (value ?: ""),
        ),
    )
}

private suspend fun RoutingContext.patchMongoData(
    pluralName: String,
    primaryKey: String,
    panel: AdminMongoCollection,
    fieldName: String,
    value: String?,
) {
    val field = panel.getAllFields().firstOrNull { it.fieldName == fieldName }
    if (field == null) {
        call.respondJson(
            HttpStatusCode.BadRequest,
            mapOf("status" to "error", "message" to "Invalid field name: $fieldName in collection $pluralName"),
        )
        return
    }

    if (!field.isInlineEditable) {
        call.respondJson(
            HttpStatusCode.BadRequest,
            mapOf("status" to "error", "message" to "Field $fieldName cannot be edited inline"),
        )
        return
    }

    val currentTranslator = call.translator
    val validationError =
        Validators.validateFieldParameter(
            fieldSet = field,
            value = value,
            translator = currentTranslator,
        )

    if (validationError != null) {
        call.respondJson(
            HttpStatusCode.BadRequest,
            mapOf("status" to "error", "message" to validationError),
        )
        return
    }

    MongoClientRepository.updateAField(
        panel = panel,
        primaryKey = primaryKey,
        field = field,
        value = value,
    )

    DynamicConfiguration.currentEventListener?.onUpdateMongoData(
        collectionName = panel.getCollectionName(),
        objectPrimaryKey = primaryKey,
        events = listOf(FieldEvent(true, field, value)),
    )

    call.respondJson(
        HttpStatusCode.OK,
        mapOf(
            "status" to "success",
            "field" to (field.fieldName ?: ""),
            "value" to value,
            "displayValue" to (value ?: ""),
        ),
    )
}

private data class PatchRequestData(
    val fieldName: String?,
    val value: String?,
    val csrfToken: String?,
)

private suspend fun ApplicationCall.extractPatchData(): PatchRequestData {
    val contentType = request.contentType()
    if (contentType.match(ContentType.Application.Json)) {
        val rawText = receiveText()
        return parseJsonPatchData(rawText)
    }

    return runCatching {
        val parameters = receiveParameters()
        val csrf = parameters[CSRF_TOKEN_FIELD_NAME]
        var field = parameters["field"] ?: parameters["fieldName"] ?: parameters["columnName"]
        var value = parameters["value"]

        if (field == null) {
            val nonCsrfEntry = parameters.entries().firstOrNull { it.key != CSRF_TOKEN_FIELD_NAME }
            if (nonCsrfEntry != null) {
                field = nonCsrfEntry.key
                value = nonCsrfEntry.value.firstOrNull()
            }
        }
        PatchRequestData(fieldName = field, value = value, csrfToken = csrf)
    }.getOrElse {
        PatchRequestData(null, null, null)
    }
}

private fun parseJsonPatchData(json: String): PatchRequestData {
    val trimmed = json.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
        return PatchRequestData(null, null, null)
    }

    val content = trimmed.substring(1, trimmed.length - 1)
    val map = mutableMapOf<String, String?>()

    val regex = "\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"\\s*:\\s*(?:\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"|([^,\\}\\s]+)|null)".toRegex()
    for (match in regex.findAll(content)) {
        val key = unescapeJson(match.groupValues[1])
        val strValue = match.groups[2]?.value
        val rawValue = match.groups[3]?.value
        val value = when {
            strValue != null -> unescapeJson(strValue)
            rawValue != null && rawValue != "null" -> rawValue
            else -> null
        }
        map[key] = value
    }

    val csrfToken = map[CSRF_TOKEN_FIELD_NAME] ?: map["csrfToken"]
    var field = map["field"] ?: map["fieldName"] ?: map["columnName"]
    var value = map["value"]

    if (field == null) {
        val candidate = map.entries.firstOrNull {
            it.key != CSRF_TOKEN_FIELD_NAME && it.key != "csrfToken"
        }
        if (candidate != null) {
            field = candidate.key
            value = candidate.value
        }
    }

    return PatchRequestData(fieldName = field, value = value, csrfToken = csrfToken)
}

private fun unescapeJson(input: String): String =
    input.replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")

private fun escapeJson(str: String): String =
    str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

private suspend fun ApplicationCall.respondJson(
    status: HttpStatusCode,
    map: Map<String, Any?>,
) {
    val jsonEntries =
        map.entries.joinToString(",") { (k, v) ->
            val formattedVal =
                when (v) {
                    null -> "null"
                    is Boolean -> v.toString()
                    is Number -> v.toString()
                    else -> "\"${escapeJson(v.toString())}\""
                }
            "\"${escapeJson(k)}\":$formattedVal"
        }
    respondText("{$jsonEntries}", contentType = ContentType.Application.Json, status = status)
}
