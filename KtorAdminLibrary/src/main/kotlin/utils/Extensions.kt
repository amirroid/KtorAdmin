package utils

import com.google.devtools.ksp.symbol.KSAnnotation
import models.ColumnSet


fun ColumnSet.toSuitableStringForFile() =
    "\n\t\tColumnSet(\n\t\t\tcolumnName=\"$columnName\",\n\t\t\ttype=ColumnType.${type},\n\t\t\tshowInPanel=$showInPanel)"

inline fun <reified D> KSAnnotation.findArgument(property: String) =
    arguments.find { it.name?.asString() == property }?.value as? D
