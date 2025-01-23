package converters

import models.ColumnSet
import models.events.ColumnEvent

fun List<ColumnSet>.toEvents(parameters: List<Any?>, changedColumns: List<String>? = null) =
    mapIndexed { index, columnSet ->
        ColumnEvent(
            changedColumns == null || changedColumns.any { column -> column == columnSet.columnName },
            columnSet,
            parameters[index]
        )
    }