package formatters

import models.types.ColumnType
import java.sql.Timestamp
import java.time.format.DateTimeFormatter

internal fun Any?.formatToDisplayInTable(columnType: ColumnType): String {
    return when {
        this is Timestamp && columnType == ColumnType.DATETIME -> {
            val formatter = DateTimeFormatter.ofPattern("dd EEE yyyy - HH:mm:ss")
            toLocalDateTime().format(formatter)
        }

        this is Timestamp && columnType == ColumnType.DATE -> {
            val formatter = DateTimeFormatter.ofPattern("dd EEE yyyy")
            toLocalDateTime().format(formatter)
        }

        this is ByteArray && columnType == ColumnType.BINARY -> "Byte array object"
        this == null -> "N/A"
        else -> toString()
    }
}