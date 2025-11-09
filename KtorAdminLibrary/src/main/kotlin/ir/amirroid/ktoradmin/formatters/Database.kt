package ir.amirroid.ktoradmin.formatters

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.models.types.FieldType
import ir.amirroid.ktoradmin.utils.Constants
import java.sql.ResultSet
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date

internal fun Any?.formatToDisplayInTable(columnType: ColumnType): String {
    return when {
        this is Timestamp && (columnType == ColumnType.DATETIME || columnType == ColumnType.TIMESTAMP_WITH_TIMEZONE) -> {
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
        this is Timestamp && (columnType == ColumnType.DATETIME || columnType == ColumnType.TIMESTAMP_WITH_TIMEZONE) -> {
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
    ColumnType.LONG, ColumnType.ULONG -> getObject(name)?.toString()?.toLongOrNull()
    ColumnType.INTEGER, ColumnType.UINTEGER -> getObject(name)?.toString()?.toIntOrNull()
    ColumnType.SHORT, ColumnType.USHORT -> getObject(name)?.toString()?.toShortOrNull()
    ColumnType.FLOAT -> getObject(name)?.toString()?.toFloatOrNull()
    ColumnType.DOUBLE -> getObject(name)?.toString()?.toDoubleOrNull()

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