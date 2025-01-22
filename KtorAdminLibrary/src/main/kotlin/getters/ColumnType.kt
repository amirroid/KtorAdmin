package getters

import models.ColumnType


import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal fun String.toTypedValue(columnType: ColumnType): Any {
    return when (columnType) {
        ColumnType.INTEGER -> this.toIntOrNull() ?: this
        ColumnType.UINTEGER -> this.toUIntOrNull() ?: this
        ColumnType.SHORT -> this.toShortOrNull() ?: this
        ColumnType.USHORT -> this.toUShortOrNull() ?: this
        ColumnType.LONG -> this.toLongOrNull() ?: this
        ColumnType.ULONG -> this.toULongOrNull() ?: this
        ColumnType.DOUBLE -> this.toDoubleOrNull() ?: this
        ColumnType.FLOAT -> this.toFloatOrNull() ?: this
        ColumnType.BIG_DECIMAL -> this.toBigDecimalOrNull() ?: this
        ColumnType.CHAR -> this.singleOrNull() ?: this
        ColumnType.BOOLEAN -> this.toBooleanStrictOrNull() ?: this
        ColumnType.DATE -> this.toLocalDate() ?: this
        ColumnType.DATETIME -> this.toLocalDateTime() ?: this
        else -> this
    }
}

/**
 * Extension function to convert a String to LocalDate.
 */
internal fun String.toLocalDate(): LocalDate? {
    return try {
        LocalDate.parse(this, DateTimeFormatter.ISO_DATE)
    } catch (e: DateTimeParseException) {
        null
    }
}

/**
 * Extension function to convert a String to LocalDateTime.
 */
internal fun String.toLocalDateTime(): LocalDateTime? {
    return try {
        LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: DateTimeParseException) {
        null
    }
}
