package getters

import models.types.FieldType

internal fun String.toTypedValue(fieldType: FieldType): Any {
    return when (fieldType) {
        is FieldType.Integer -> this.toIntOrNull() ?: this
        is FieldType.Long -> this.toLongOrNull() ?: this
        is FieldType.Double -> this.toDoubleOrNull() ?: this
        is FieldType.Float -> this.toFloatOrNull() ?: this
        is FieldType.Boolean -> this.toBooleanStrictOrNull() ?: this
        is FieldType.Date -> this.toLocalDate() ?: this
        is FieldType.DateTime -> this.toLocalDateTime() ?: this
        is FieldType.Instant -> this.toLocalDateTime() ?: this
        is FieldType.Decimal128 -> this.toBigDecimalOrNull() ?: this
        else -> this
    }
}
