package converters

import csrf.CsrfManager
import flash.REQUEST_ID
import flash.REQUEST_ID_FORM
import getters.toTypedValue
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.toList
import kotlinx.io.readByteArray
import models.ColumnSet
import models.types.ColumnType
import models.events.FileEvent
import models.field.FieldSet
import models.types.FieldType
import panels.*
import repository.FileRepository
import response.ErrorResponse
import response.Response
import validators.Validators

private fun ByteArray.toBinaryString(): String {
    return joinToString("") { "%02x".format(it) }
}

private const val CSRF_TOKEN_FIELD_NAME = "_csrf"

fun checkCsrfToken(csrfToken: String?): Boolean {
    return CsrfManager.validateToken(csrfToken)
}

internal suspend fun MultiPartData.toTableValues(
    table: AdminJdbcTable,
    initialData: List<String?>? = null
): Response<List<Pair<String, Any?>?>> {
    val items = mutableMapOf<String, Pair<String, Any?>?>()
    val columns = table.getAllAllowToShowColumnsInUpsert()

    val fileBytes = mutableMapOf<ColumnSet, Pair<String?, ByteArray>>()
    var requestId: String? = null
    val errors = mutableListOf<ErrorResponse?>()
    var isInvalidRequest = false
    forEachPart { part ->
        val name = part.name
        val column = columns.firstOrNull { it.columnName == name }
        if (name == CSRF_TOKEN_FIELD_NAME && part is PartData.FormItem) {
            val token = part.value
            part.dispose()
            if (!checkCsrfToken(token)) {
                isInvalidRequest = true
                return@forEachPart
            }
        }
        if (name == REQUEST_ID_FORM && part is PartData.FormItem) {
            requestId = part.value
            part.dispose()
        }
        if (column != null && name != null) {
            when (part) {
                is PartData.FileItem -> {
                    val bytes = part.provider().readRemaining().readByteArray()
                    val fileName = part.originalFileName?.takeIf { it.isNotEmpty() }
                    val partSize = bytes.size.toLong()
                    when (column.type) {
                        ColumnType.FILE -> {
                            fileBytes[column] = fileName to bytes
                            val itemErrors = listOfNotNull(
                                Validators.validateMimeType(fileName, column.limits),
                                Validators.validateBytesSize(
                                    partSize,
                                    column.limits
                                ),
                            ).plus(
                                Validators.validateColumnParameter(
                                    column,
                                    fileName ?: initialData?.get(columns.indexOf(column))
                                )?.let {
                                    listOf(it)
                                } ?: emptyList()
                            ).takeIf { it.isNotEmpty() }
                                ?.let {
                                    ErrorResponse(column.columnName, it)
                                }
                            errors += itemErrors
                        }

                        ColumnType.BINARY -> {
                            val anotherErrors =
                                Validators.validateColumnParameter(
                                    column,
                                    fileName ?: initialData?.get(columns.indexOf(column))
                                )?.let {
                                    listOf(it)
                                } ?: emptyList()
                            val itemErrors = Validators.validateBytesSize(partSize, column.limits)
                                ?.let { ErrorResponse(column.columnName, listOf(it) + anotherErrors) }
                            errors += itemErrors
                            if (itemErrors == null) {
                                items[name] = bytes.let { it.toBinaryString() to it }
                            }
                        }

                        else -> Unit
                    }
                }

                is PartData.FormItem -> {
                    val itemErrors = Validators.validateColumnParameter(column, part.value)
                        ?.let { ErrorResponse(column.columnName, listOf(it)) }
                    errors += itemErrors
                    items[name] = part.value.let { it to it.toTypedValue(column.type) }
                }

                else -> Unit
            }
        }
        part.dispose()
    }
    if (isInvalidRequest) return Response.InvalidRequest
    val errorsNotNull = errors.filterNotNull()
    if (errorsNotNull.isNotEmpty()) {
        return Response.Error(
            errorsNotNull,
            columns.associateWith { items[it.columnName]?.first }.mapKeys { it.key.columnName },
            requestId
        )
    }
    fileBytes.forEach { (column, pair) ->
        val fileData = FileRepository.uploadFile(column.uploadTarget!!, pair.second, pair.first)
            ?.let {
                it.first to FileEvent(
                    fileName = it.first,
                    bytes = it.second
                )
            }
        items[column.columnName] = fileData
    }
    // Return values corresponding to the columns
    return Response.Success(columns.map { items[it.columnName] })
}

internal suspend fun MultiPartData.toTableValues(
    table: AdminMongoCollection,
    initialData: List<String?>? = null
): Response<List<Pair<String, Any?>?>> {
    val items = mutableMapOf<String, Pair<String, Any?>?>()
    val fields = table.getAllAllowToShowFieldsInUpsert()

    val fileBytes = mutableMapOf<FieldSet, Pair<String?, ByteArray>>()

    val errors = mutableListOf<ErrorResponse?>()
    var isInvalidRequest = false
    forEachPart { part ->
        val name = part.name
        if (name == CSRF_TOKEN_FIELD_NAME && part is PartData.FormItem) {
            val token = part.value
            part.dispose()
            if (!checkCsrfToken(token)) {
                isInvalidRequest = true
                return@forEachPart
            }
        }
        val field = fields.firstOrNull { it.fieldName == name }
        if (field != null && name != null) {
            when (part) {
                is PartData.FileItem -> {
                    val bytes = part.provider().readRemaining().readByteArray()
                    val fileName = part.originalFileName?.takeIf { it.isNotEmpty() }
                    val partSize = bytes.size.toLong()
                    when (field.type) {
                        FieldType.File -> {
                            fileBytes[field] = fileName to bytes
                            val itemErrors = listOfNotNull(
                                Validators.validateMimeType(fileName, field.limits),
                                Validators.validateBytesSize(
                                    partSize,
                                    field.limits
                                ),
                            ).plus(
                                Validators.validateFieldParameter(
                                    field,
                                    fileName ?: initialData?.get(fields.indexOf(field))
                                )?.let {
                                    listOf(it)
                                } ?: emptyList()
                            ).takeIf { it.isNotEmpty() }
                                ?.let {
                                    ErrorResponse(field.fieldName.toString(), it)
                                }
                            errors += itemErrors
                        }

                        else -> Unit
                    }
                }

                is PartData.FormItem -> {
                    val itemErrors = Validators.validateFieldParameter(field, part.value)
                        ?.let { ErrorResponse(field.fieldName.toString(), listOf(it)) }
                    errors += itemErrors
                    items[name] = part.value.let { it to it.toTypedValue(field.type) }
                }

                else -> Unit
            }
        }
        part.dispose()
    }
    if (isInvalidRequest) return Response.InvalidRequest
    val errorsNotNull = errors.filterNotNull()
    if (errorsNotNull.isNotEmpty()) {
        return Response.Error(
            errorsNotNull,
            fields.associateWith { items[it.fieldName.toString()]?.first }.mapKeys { it.key.fieldName.toString() }
        )
    }
    fileBytes.forEach { (field, pair) ->
        val fileData = FileRepository.uploadFile(field.uploadTarget!!, pair.second, pair.first)
            ?.let {
                it.first to FileEvent(
                    fileName = it.first,
                    bytes = it.second
                )
            }
        items[field.fieldName.toString()] = fileData
    }
    // Return values corresponding to the columns
    return Response.Success(fields.map { items[it.fieldName] })
}