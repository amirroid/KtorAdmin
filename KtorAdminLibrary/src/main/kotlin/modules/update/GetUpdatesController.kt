package modules.update

import utils.notFound
import utils.serverError
import getters.getReferencesItems
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import models.ColumnSet
import models.field.FieldSet
import models.types.ColumnType
import models.types.FieldType
import panels.*
import repository.FileRepository
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import utils.Constants


internal suspend fun ApplicationCall.handleEditItem(panels: List<AdminPanel>) {
    val pluralName = parameters["pluralName"]
    val primaryKey = parameters["primaryKey"]
    val panel = panels.find { it.getPluralName() == pluralName }
    when {
        panel == null -> respondText { "No table found with plural name: $pluralName" }
        primaryKey == null -> respondText { "No primary key found: $pluralName" }
        else -> {
            when (panel) {
                is AdminJdbcTable -> handleJdbcEditView(primaryKey, panel, panels)
                is AdminMongoCollection -> handleNoSqlEditView(primaryKey, panel, panels)
            }
        }
    }
}

private suspend fun ApplicationCall.handleJdbcEditView(
    primaryKey: String,
    table: AdminJdbcTable,
    tables: List<AdminPanel>,
) {
    val data = JdbcQueriesRepository.getData(table, primaryKey)
    if (data == null) {
        notFound("No data found with primary key: $primaryKey")
    } else {
        runCatching {
            val columns = table.getAllAllowToShowColumns()
            val referencesItems = getReferencesItems(tables.filterIsInstance<AdminJdbcTable>(), columns)
            val values = columns.mapIndexed { index, column ->
                column.columnName to data[index]?.let { item ->
                    handlePreviewValue(
                        column,
                        item,
                        this
                    )
                }
            }.toMap()
            println("VALUES $values")
            respond(
                VelocityContent(
                    "${Constants.TEMPLATES_PREFIX_PATH}/upsert_admin.vm", model = mapOf(
                        "columns" to columns,
                        "tableName" to table.getTableName(),
                        "values" to values,
                        "singularTableName" to table.getSingularName()
                            .replaceFirstChar { it.uppercaseChar() },
                        "references" to referencesItems
                    )
                )
            )
        }.onFailure {
            serverError("Error: ${it.message}")
        }
    }
}


private suspend fun ApplicationCall.handleNoSqlEditView(
    primaryKey: String,
    panel: AdminMongoCollection,
    panels: List<AdminPanel>,
) {
    val data = MongoClientRepository.getData(panel, primaryKey)
    if (data == null) {
        notFound("No data found with primary key: $primaryKey")
    } else {
        runCatching {
            val fields = panel.getAllAllowToShowFieldsInUpsert()
//            val referencesItems = getReferencesItems(tables.filterIsInstance<AdminJdbcTable>(), columns)
            val values = fields.mapIndexed { index, field ->
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
                        "singularTableName" to panel.getSingularName()
                            .replaceFirstChar { it.uppercaseChar() },
//                        "references" to referencesItems,
                        "collectionName" to panel.getCollectionName(),
                        "singularName" to panel.getSingularName().replaceFirstChar { it.uppercaseChar() },
                    )
                )
            )
        }.onFailure {
            serverError("Error: ${it.message}")
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
