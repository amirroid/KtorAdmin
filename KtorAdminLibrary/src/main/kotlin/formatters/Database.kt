package formatters

import configuration.DynamicConfiguration
import models.ColumnSet
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