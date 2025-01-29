package validators

import models.ColumnSet
import models.field.FieldSet
import models.types.ColumnType
import utils.Constants
import utils.allIndexed

internal fun List<ColumnSet>.validateParameters(parameters: List<String?>): Boolean = allIndexed { index, columnSet ->
    columnSet.nullable || parameters[index] != null
}

internal fun List<FieldSet>.validateFieldsParameters(parameters: List<String?>): Boolean = allIndexed { index, fieldSet ->
    fieldSet.nullable || parameters[index] != null
}