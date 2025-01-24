package models.types

import models.field.FieldSet
import utils.toSuitableStringForFile

sealed class FieldType {
    data object String : FieldType()
    data object Integer : FieldType()
    data object Long : FieldType()
    data object Double : FieldType()
    data object Float : FieldType()
    data object Boolean : FieldType()
    data object Date : FieldType()
    data object DateTime : FieldType()
    data object Binary : FieldType()
    data object Enumeration : FieldType()
    data object File : FieldType()
    data object Decimal128 : FieldType()
    data object ObjectId : FieldType()
    data object NotAvailable : FieldType()
    data class Map(val fields: kotlin.collections.List<FieldSet>) : FieldType()
    data class List(val fields: kotlin.collections.List<FieldSet>) : FieldType()
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