package validators

import models.ColumnSet
import models.types.ColumnType
import utils.Constants
import utils.allIndexed

internal fun List<ColumnSet>.validateParameters(parameters: List<String?>) = allIndexed { index, columnSet ->
    columnSet.nullable || parameters[index] != null
}