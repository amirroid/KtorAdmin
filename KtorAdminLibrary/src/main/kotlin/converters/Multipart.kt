package converters

import getters.toTypedValue
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import models.types.ColumnType
import models.events.FileEvent
import models.types.FieldType
import repository.FileRepository
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.getAllAllowToShowColumns
import panels.getAllAllowToShowFieldsInUpsert

internal suspend fun MultiPartData.toTableValues(table: AdminJdbcTable): List<Pair<String, Any?>?> {
    val items = mutableMapOf<String, Pair<String, Any?>?>()
    val columns = table.getAllAllowToShowColumns()

    // Process each part of the multipart request
    forEachPart { partData ->
        val name = partData.name
        val column = columns.firstOrNull { it.columnName == name }

        if (column != null && name != null) {
            when (partData) {
                is PartData.FormItem -> items[name] = partData.value.let { it to it.toTypedValue(column.type) }
                is PartData.FileItem -> {
                    val targetColumn = table.getAllColumns().firstOrNull { it.columnName == name }
                    when (targetColumn?.type) {
                        ColumnType.FILE -> {
                            val fileData = FileRepository.uploadFile(column.uploadTarget!!, partData)?.let {
                                it.first to FileEvent(
                                    fileName = it.first,
                                    bytes = it.second
                                )
                            }
                            items[name] = fileData
                        }

                        ColumnType.BINARY -> {
                            items[name] =
                                partData.provider().readRemaining().readByteArray().let {
                                    it.decodeToString() to it
                                }
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
    return columns.map { items[it.columnName] }
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
                is PartData.FormItem -> items[name] = partData.value.let { it to it.toTypedValue(field.type) }
                is PartData.FileItem -> {
                    val targetColumn = table.getAllFields().firstOrNull { it.fieldName == name }
                    when (targetColumn?.type) {
                        FieldType.File -> {
                            val fileData = FileRepository.uploadFile(field.uploadTarget!!, partData)?.let {
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