package models.types

import models.field.FieldSet
import utils.toSuitableStringForFile

sealed class FieldType(val name: kotlin.String, val fieldType: kotlin.String) {
    data object String : FieldType("String", "text")
    data object Integer : FieldType("Integer", "number")
    data object Long : FieldType("Long", "number")
    data object Double : FieldType("Double", "number")
    data object Float : FieldType("Float", "number")
    data object Boolean : FieldType("Boolean", "checkbox")
    data object Date : FieldType("Date", "date")
    data object DateTime : FieldType("DateTime", "datetime-local")
    data object Enumeration : FieldType("Enumeration", "select")
    data object Instant : FieldType("Instant", "datetime-local")
    data object File : FieldType("File", "file")
    data object Decimal128 : FieldType("Decimal128", "number")
    data object ObjectId : FieldType("ObjectId", "text")
    data object NotAvailable : FieldType("NotAvailable", "text")

    data class Map(val fields: kotlin.collections.List<FieldSet>) : FieldType("Map", "map")
    data class List(val fields: kotlin.collections.List<FieldSet>) : FieldType("List", "list")
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