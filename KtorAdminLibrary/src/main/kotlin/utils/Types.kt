package utils

import models.field.FieldSet
import models.types.ColumnType
import models.types.FieldType

fun guessPropertyType(type: String) = when (type) {
    "kotlin.Int" -> ColumnType.INTEGER
    "kotlin.UInt" -> ColumnType.UINTEGER
    "kotlin.String" -> ColumnType.STRING
    "kotlin.Byte" -> ColumnType.BYTES
    "kotlin.UByte" -> ColumnType.UBYTES
    "kotlin.Short" -> ColumnType.SHORT
    "kotlin.UShort" -> ColumnType.USHORT
    "kotlin.Long" -> ColumnType.LONG
    "kotlin.ULong" -> ColumnType.ULONG
    "kotlin.Double" -> ColumnType.DOUBLE
    "kotlin.Float" -> ColumnType.FLOAT
    "java.math.BigDecimal" -> ColumnType.BIG_DECIMAL
    "kotlin.Char" -> ColumnType.CHAR
    "kotlin.ByteArray" -> ColumnType.BINARY
    "kotlin.Boolean" -> ColumnType.BOOLEAN
    "kotlinx.datetime.LocalDateTime" -> ColumnType.DATETIME
    "kotlinx.datetime.LocalDate" -> ColumnType.DATE
    "kotlin.time.Duration" -> ColumnType.DURATION
    else -> ColumnType.NOT_AVAILABLE
}

fun guessFieldPropertyType(type: String): FieldType = when (type) {
    "kotlin.String" -> FieldType.String
    "kotlin.Int" -> FieldType.Integer
    "kotlin.Long" -> FieldType.Long
    "kotlin.Double" -> FieldType.Double
    "kotlin.Float" -> FieldType.Float
    "kotlin.Boolean" -> FieldType.Boolean
    "java.util.Date" -> FieldType.Date
    "java.math.BigDecimal" -> FieldType.Decimal128
    "org.bson.types.ObjectId" -> FieldType.ObjectId
    "kotlinx.datetime.Instant" -> FieldType.Instant
    "kotlinx.datetime.LocalDateTime", "java.time.LocalDateTime" -> FieldType.DateTime
    "kotlinx.datetime.LocalDate", "java.time.LocalDate" -> FieldType.Date
    "kotlin.collections.List" -> FieldType.List(emptyList())
    "kotlin.collections.Map" -> FieldType.Map(emptyList())
    else -> FieldType.NotAvailable
}