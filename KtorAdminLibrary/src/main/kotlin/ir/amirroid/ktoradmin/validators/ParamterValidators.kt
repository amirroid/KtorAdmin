package ir.amirroid.ktoradmin.validators

import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.field.FieldSet
import ir.amirroid.ktoradmin.utils.allIndexed

internal fun List<ColumnSet>.validateParameters(parameters: List<String?>): Boolean = allIndexed { index, columnSet ->
    columnSet.nullable || parameters[index] != null
}

internal fun List<FieldSet>.validateFieldsParameters(parameters: List<String?>): Boolean =
    allIndexed { index, fieldSet ->
        fieldSet.nullable || parameters[index] != null
    }