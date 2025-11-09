package ir.amirroid.ktoradmin.converters

import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.events.ColumnEvent
import ir.amirroid.ktoradmin.models.events.FieldEvent
import ir.amirroid.ktoradmin.models.field.FieldSet

fun List<ColumnSet>.toEvents(parameters: List<Any?>, changedColumns: List<String>? = null) =
    mapIndexed { index, columnSet ->
        ColumnEvent(
            changedColumns == null || changedColumns.any { column -> column == columnSet.columnName },
            columnSet,
            parameters[index]
        )
    }

fun List<FieldSet>.toFieldEvents(parameters: List<Any?>, changedFields: List<String>? = null) =
    mapIndexed { index, fieldSet ->
        FieldEvent(
            changedFields == null || changedFields.any { field -> field == fieldSet.fieldName },
            fieldSet,
            parameters[index]
        )
    }