package getters

import configuration.DynamicConfiguration
import models.types.FieldType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

internal fun String.toTypedValue(fieldType: FieldType): Any {
    return when (fieldType) {
        is FieldType.Integer -> this.toIntOrNull() ?: this
        is FieldType.Long -> this.toLongOrNull() ?: this
        is FieldType.Double -> this.toDoubleOrNull() ?: this
        is FieldType.Float -> this.toFloatOrNull() ?: this
        is FieldType.Boolean -> this.toBoolean() ?: this
        is FieldType.Date -> this.toLocalDate()?.toDate() ?: this
        is FieldType.DateTime -> this.toLocalDateTime()?.toDate() ?: this
        is FieldType.Instant -> this.toLocalDateTime()?.toDate() ?: this

        is FieldType.Decimal128 -> this.toBigDecimalOrNull() ?: this
        else -> this
    }
}


internal fun LocalDateTime.toDate(): Date =
    Date.from(this.atZone(DynamicConfiguration.timeZone).toInstant())

internal fun LocalDate.toDate() = Date(atStartOfDay(DynamicConfiguration.timeZone).toInstant().toEpochMilli())