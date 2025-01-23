package validators

import models.ColumnSet
import utils.allIndexed

internal fun List<ColumnSet>.validateParameters(parameters: List<String?>) = allIndexed { index, columnSet ->
    columnSet.nullable || parameters[index] != null
}