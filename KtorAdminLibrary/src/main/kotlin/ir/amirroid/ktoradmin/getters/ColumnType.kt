package ir.amirroid.ktoradmin.getters


import ir.amirroid.ktoradmin.models.events.FileEvent
import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.utils.Constants
import java.math.BigDecimal
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

internal fun String.toTypedValue(columnType: ColumnType): Any? {
    return when (columnType) {
        ColumnType.INTEGER -> this.toIntOrNull()
        ColumnType.UINTEGER -> this.toUIntOrNull()
        ColumnType.SHORT -> this.toShortOrNull()
        ColumnType.USHORT -> this.toUShortOrNull()
        ColumnType.LONG -> this.toLongOrNull()
        ColumnType.ULONG -> this.toULongOrNull()
        ColumnType.DOUBLE -> this.toDoubleOrNull()
        ColumnType.FLOAT -> this.toFloatOrNull()
        ColumnType.BIG_DECIMAL -> this.toBigDecimalOrNull()
        ColumnType.CHAR -> this.singleOrNull()
        ColumnType.BOOLEAN -> this.toBoolean()
        ColumnType.DATE -> this.toLocalDate()
        ColumnType.DATETIME -> this.toLocalDateTime()
        ColumnType.TIMESTAMP_WITH_TIMEZONE -> toOffsetDateTime()
        else -> this
    }
}

internal fun String.toTypedValueNullable(columnType: ColumnType): Any? {
    return when (columnType) {
        ColumnType.INTEGER -> toIntOrNull()
        ColumnType.UINTEGER -> toUIntOrNull()
        ColumnType.SHORT -> toShortOrNull()
        ColumnType.USHORT -> toUShortOrNull()
        ColumnType.LONG -> toLongOrNull()
        ColumnType.ULONG -> toULongOrNull()
        ColumnType.DOUBLE -> toDoubleOrNull()
        ColumnType.FLOAT -> toFloatOrNull()
        ColumnType.BIG_DECIMAL -> toBigDecimalOrNull()
        ColumnType.CHAR -> singleOrNull()
        ColumnType.BOOLEAN -> toBoolean()
        ColumnType.DATE -> toLocalDate()
        ColumnType.DATETIME -> toLocalDateTime()
        ColumnType.TIMESTAMP_WITH_TIMEZONE -> toOffsetDateTime()
        else -> this
    }
}

internal fun String.toBoolean(): Boolean? {
    return when (this) {
        Constants.TRUE_FORM -> true
        Constants.FALSE_FORM -> false
        else -> null
    }
}

internal fun PreparedStatement.putColumn(columnType: ColumnType, value: Any?, index: Int) {
    if (value == null) {
        when (columnType) {
            ColumnType.INTEGER -> this.setNull(index, Types.INTEGER)
            ColumnType.UINTEGER -> this.setNull(
                index,
                Types.INTEGER
            ) // For unsigned integers, use INTEGER
            ColumnType.SHORT -> this.setNull(index, Types.SMALLINT)
            ColumnType.USHORT -> this.setNull(
                index,
                Types.SMALLINT
            ) // For unsigned short, use SMALLINT
            ColumnType.LONG -> this.setNull(index, Types.BIGINT)
            ColumnType.ULONG -> this.setNull(index, Types.BIGINT) // For unsigned long, use BIGINT
            ColumnType.FLOAT -> this.setNull(index, Types.FLOAT)
            ColumnType.DOUBLE -> this.setNull(index, Types.DOUBLE)
            ColumnType.BIG_DECIMAL -> this.setNull(index, Types.DECIMAL)
            ColumnType.STRING, ColumnType.CHAR, ColumnType.FILE -> this.setNull(
                index,
                Types.VARCHAR
            )

            ColumnType.BOOLEAN -> this.setNull(index, Types.BOOLEAN)
            ColumnType.DATE -> this.setNull(index, Types.DATE)
            ColumnType.DATETIME -> this.setNull(index, Types.TIMESTAMP)
            ColumnType.TIMESTAMP_WITH_TIMEZONE -> this.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE)
            ColumnType.BINARY -> this.setNull(index, Types.BINARY)
            else -> throw IllegalArgumentException("Unsupported column type for null value: $columnType")
        }
        return
    }

    when (value) {
        is Boolean -> {
            if (columnType == ColumnType.BOOLEAN) this.setBoolean(index, value)
        }

        is Int -> {
            if (columnType == ColumnType.INTEGER) this.setInt(index, value)
        }

        is UInt -> {
            if (columnType == ColumnType.UINTEGER) this.setInt(
                index,
                value.toInt()
            ) // UInt doesn't have a direct set function
        }

        is Short -> {
            if (columnType == ColumnType.SHORT) this.setShort(index, value)
        }

        is UShort -> {
            if (columnType == ColumnType.USHORT) this.setShort(
                index,
                value.toShort()
            ) // UShort doesn't have a direct set function
        }

        is Long -> {
            if (columnType == ColumnType.LONG) this.setLong(index, value)
        }

        is ULong -> {
            if (columnType == ColumnType.ULONG) this.setLong(
                index,
                value.toLong()
            ) // ULong doesn't have a direct set function
        }

        is Float -> {
            if (columnType == ColumnType.FLOAT) this.setFloat(index, value)
        }

        is Double -> {
            if (columnType == ColumnType.DOUBLE) this.setDouble(index, value)
        }

        is String -> {
            if (columnType == ColumnType.STRING || columnType == ColumnType.CHAR || columnType == ColumnType.ENUMERATION) this.setString(
                index,
                value
            )
        }

        is FileEvent -> {
            if (columnType == ColumnType.FILE) this.setString(
                index,
                value.fileName
            )
        }

        is BigDecimal -> {
            if (columnType == ColumnType.BIG_DECIMAL) this.setBigDecimal(index, value)
        }

        is Date -> {
            if (columnType == ColumnType.DATE) this.setDate(index, value)
        }

        is Timestamp -> {
            if (columnType == ColumnType.DATETIME) this.setTimestamp(index, value)
        }

        is LocalDate -> {
            if (columnType == ColumnType.DATE) this.setDate(index, Date.valueOf(value))
        }

        is LocalDateTime -> {
            if (columnType == ColumnType.DATETIME) this.setTimestamp(
                index,
                Timestamp.valueOf(value)
            )
        }

        is OffsetDateTime -> {
            if (columnType == ColumnType.TIMESTAMP_WITH_TIMEZONE) this.setTimestamp(
                index,
                Timestamp.valueOf(value.toLocalDateTime())
            )
        }

        is ByteArray -> {
            if (columnType == ColumnType.BINARY) this.setBytes(index, value)
        }

        else -> Unit
    }
}