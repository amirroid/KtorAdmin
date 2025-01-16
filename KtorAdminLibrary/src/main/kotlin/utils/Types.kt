package utils

import models.ColumnType

fun guessPropertyType(type: String) = when (type) {
    "kotlin.Int" -> ColumnType.INTEGER
    "kotlin.String" -> ColumnType.STRING
    else -> ColumnType.STRING
}