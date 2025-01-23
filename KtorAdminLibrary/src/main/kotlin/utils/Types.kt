package utils

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

fun guessFieldPropertyType(type: String) = when (type) {
    "kotlin.String" -> FieldType.STRING
    "kotlin.Int" -> FieldType.INTEGER
    "kotlin.Long" -> FieldType.LONG
    "kotlin.Double" -> FieldType.DOUBLE
    "kotlin.Float" -> FieldType.FLOAT
    "kotlin.Boolean" -> FieldType.BOOLEAN
    "java.util.Date" -> FieldType.DATE
    "kotlin.ByteArray" -> FieldType.BINARY
    "java.math.BigDecimal" -> FieldType.DECIMAL128
    "org.bson.types.ObjectId" -> FieldType.OBJECT_ID
    "kotlinx.datetime.LocalDateTime" -> FieldType.DATETIME
    "kotlinx.datetime.LocalDate" -> FieldType.DATE
    else -> FieldType.NOT_AVAILABLE
}
