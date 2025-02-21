package models.types

import models.field.FieldSet
import utils.toSuitableStringForFile

sealed class FieldType(val name: kotlin.String, val fieldType: kotlin.String) {
    data object String : FieldType("STRING", "text")
    data object Integer : FieldType("INTEGER", "number")
    data object Long : FieldType("LONG", "number")
    data object Double : FieldType("DOUBLE", "number")
    data object Float : FieldType("FLOAT", "number")
    data object Boolean : FieldType("BOOLEAN", "checkbox")
    data object Date : FieldType("DATE", "date")
    data object DateTime : FieldType("DATETIME", "datetime-local")
    data object Enumeration : FieldType("ENUMERATION", "select")
    data object Instant : FieldType("INSTANT", "datetime-local")
    data object File : FieldType("FILE", "file")
    data object Decimal128 : FieldType("DECIMAL128", "number")
    data object ObjectId : FieldType("OBJECT_ID", "text")
    data object NotAvailable : FieldType("NOT_AVAILABLE", "text")

    data class Map(val fields: kotlin.collections.List<FieldSet>) : FieldType("MAP", "map")
    data class List(val fields: kotlin.collections.List<FieldSet>) : FieldType("LIST", "list")
}

fun FieldType.toSuitableStringForFile(): String = when (this) {
    is FieldType.Map -> "Map(${
        fields.joinToString(
            prefix = "listOf(",
            postfix = ")",
            separator = ",\n"
        ) { it.toSuitableStringForFile() }
    })"

    is FieldType.List -> "List(${
        fields.joinToString(
            prefix = "listOf(",
            postfix = ")",
            separator = ",\n"
        ) { it.toSuitableStringForFile() }
    })"

    else -> this.toString()
}