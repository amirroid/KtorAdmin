package formatters

import configuration.DynamicConfiguration
import io.ktor.server.util.toLocalDateTime
import models.ColumnSet
import models.types.ColumnType
import models.types.FieldType
import utils.Constants
import java.sql.ResultSet
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

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


internal fun Any?.formatToDisplayInCollection(fieldType: FieldType): String {
    return when {
        this is Date -> {
            if (fieldType == FieldType.Date) {
                val formatter = SimpleDateFormat("dd EEE yyyy")
                formatter.format(this) ?: "N/A"
            } else {
                val formatter = SimpleDateFormat("dd EEE yyyy - HH:mm:ss")
                formatter.format(this) ?: "N/A"
            }
        }

        this == null -> "N/A"
        else -> toString()
    }
}

internal fun Any?.formatToDisplayInUpsert(columnType: ColumnType): String {
    return when {
        this is Timestamp && columnType == ColumnType.DATETIME -> {
            val formatter = DateTimeFormatter.ofPattern(Constants.LOCAL_DATETIME_FORMAT)
            toLocalDateTime().format(formatter)
        }

        this is Timestamp && columnType == ColumnType.DATE -> {
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            toLocalDateTime().format(formatter)
        }

        this is ByteArray && columnType == ColumnType.BINARY -> "Byte array object"
        this == null -> "N/A"
        else -> toString()
    }
}

internal fun ResultSet.getTypedValue(type: ColumnType, name: String): Any? = when (type) {
    ColumnType.LONG, ColumnType.ULONG -> getLong(name)
    ColumnType.INTEGER, ColumnType.UINTEGER -> getInt(name)
    ColumnType.SHORT, ColumnType.USHORT -> getShort(name)
    ColumnType.FLOAT -> getFloat(name)
    ColumnType.DOUBLE -> getDouble(name)

    else -> getObject(name)
}

internal inline fun <reified T> T?.map(columnSet: ColumnSet): T? {
    if (columnSet.valueMapper == null) return this
    val valueMapper = columnSet.valueMapper.let { key ->
        DynamicConfiguration.valueMappers.firstOrNull { it.key == key }
    } ?: throw IllegalStateException("ValueMapper '${columnSet.valueMapper}' is not registered.")

    val mappedData = valueMapper.map(this)
    if (mappedData != null && mappedData !is T) {
        throw IllegalStateException("ValueMapper '${columnSet.valueMapper}' returned an incompatible type. Expected: ${T::class}, but got: ${mappedData::class}.")
    }
    return mappedData
}

internal inline fun <reified T> T?.restore(columnSet: ColumnSet): T? {
    if (columnSet.valueMapper == null) return this
    val valueMapper = columnSet.valueMapper.let { key ->
        DynamicConfiguration.valueMappers.firstOrNull { it.key == key }
    } ?: throw IllegalStateException("ValueMapper '${columnSet.valueMapper}' is not registered.")

    val restoredData = valueMapper.restore(this)
    if (restoredData != null && restoredData !is T) {
        throw IllegalStateException("ValueMapper '${columnSet.valueMapper}' returned an incompatible type. Expected: ${T::class}, but got: ${restoredData::class}.")
    }
    return restoredData
}