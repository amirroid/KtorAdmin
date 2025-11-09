package ir.amirroid.ktoradmin.getters

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.models.types.FieldType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

internal fun String.toTypedValue(fieldType: FieldType): Any? {
    return when (fieldType) {
        is FieldType.Integer -> this.toIntOrNull()
        is FieldType.Long -> this.toLongOrNull()
        is FieldType.Double -> this.toDoubleOrNull()
        is FieldType.Float -> this.toFloatOrNull()
        is FieldType.Boolean -> this.toBoolean()
        is FieldType.Date -> this.toLocalDate()?.toDate()
        is FieldType.DateTime -> this.toLocalDateTime()?.toDate()
        is FieldType.Instant -> this.toLocalDateTime()?.toDate()
        is FieldType.Decimal128 -> this.toBigDecimalOrNull() ?: this
        else -> this
    }
}


internal fun LocalDateTime.toDate(): Date =
    Date.from(this.atZone(DynamicConfiguration.timeZone).toInstant())

internal fun LocalDate.toDate() = Date(atStartOfDay(DynamicConfiguration.timeZone).toInstant().toEpochMilli())