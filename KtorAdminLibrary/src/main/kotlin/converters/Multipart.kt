package converters

import getters.toTypedValue
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.toList
import kotlinx.io.readByteArray
import models.ColumnSet
import models.types.ColumnType
import models.events.FileEvent
import models.types.FieldType
import repository.FileRepository
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.getAllAllowToShowColumns
import panels.getAllAllowToShowFieldsInUpsert
import response.ErrorResponse
import response.Response
import validators.Validators

private fun ByteArray.toBinaryString(): String {
    return joinToString("") { "%02x".format(it) }
}

internal suspend fun MultiPartData.toTableValues(
    table: AdminJdbcTable,
    initialData: List<String?>? = null
): Response<List<Pair<String, Any?>?>> {
    val items = mutableMapOf<String, Pair<String, Any?>?>()
    val columns = table.getAllAllowToShowColumns()

    val fileBytes = mutableMapOf<ColumnSet, Pair<String?, ByteArray>>()

    val errors = mutableListOf<ErrorResponse?>()
    forEachPart { part ->
        val name = part.name
        val column = columns.firstOrNull { it.columnName == name }
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
                    if (itemErrors == null) {
                        items[name] = part.value.let { it to it.toTypedValue(column.type) }
                    }
                }

                else -> Unit
            }
        }
    }
    val errorsNotNull = errors.filterNotNull()
    if (errorsNotNull.isNotEmpty()) {
        return Response.Error(
            errorsNotNull,
            columns.associateWith { items[it.columnName]?.first }.mapKeys { it.key.columnName }
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

internal suspend fun MultiPartData.toTableValues(table: AdminMongoCollection): List<Pair<String, Any?>?> {
    val items = mutableMapOf<String, Pair<String, Any?>?>()
    val fields = table.getAllAllowToShowFieldsInUpsert()

    // Process each part of the multipart request
    forEachPart { partData ->
        val name = partData.name
        val field = fields.firstOrNull { it.fieldName == name }

        if (field != null && name != null) {
            when (partData) {
                is PartData.FormItem -> {
                    items[name] = partData.value.let { it to it.toTypedValue(field.type) }
                }

                is PartData.FileItem -> {
                    val targetColumn = table.getAllFields().firstOrNull { it.fieldName == name }
                    when (targetColumn?.type) {
                        FieldType.File -> {
                            val fileData = FileRepository.uploadFile(
                                field.uploadTarget!!,
                                partData.provider().readRemaining().readByteArray(),
                                partData.originalFileName
                            )?.let {
                                it.first to FileEvent(
                                    fileName = it.first,
                                    bytes = it.second
                                )
                            }
                            items[name] = fileData
                        }

                        else -> Unit
                    }
                }

                else -> Unit
            }
            partData.dispose()
        }
    }

    // Return values corresponding to the columns
    return fields.map { items[it.fieldName] }
}