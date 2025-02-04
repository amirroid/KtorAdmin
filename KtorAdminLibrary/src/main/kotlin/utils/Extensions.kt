package utils

import com.google.devtools.ksp.symbol.KSAnnotation
import models.ColumnSet
import models.common.toFormattedString
import models.date.toFormattedString
import models.field.FieldSet
import models.toFormattedString
import models.types.toSuitableStringForFile

internal fun ColumnSet.toSuitableStringForFile() = """
    |ColumnSet(
    |    columnName = "$columnName",
    |    verboseName = "$verboseName",
    |    type = ColumnType.${type},
    |    nullable = $nullable,
    |    showInPanel = $showInPanel,
    |    uploadTarget = ${uploadTarget?.toFormattedString()},
    |    allowedMimeTypes = ${allowedMimeTypes?.toSuitableStringForFile()},
    |    defaultValue = ${defaultValue?.let { "\"${it}\"" }},
    |    enumerationValues = ${enumerationValues?.toSuitableStringForFile()},
    |    statusColors = ${statusColors?.toSuitableStringForFile()},
    |    limits = ${limits?.toFormattedString()},
    |    reference = ${reference?.toFormattedString()},
    |    readOnly = $readOnly,
    |    computedColumn = ${computedColumn?.let { "\"${it}\"" }},
    |    autoNowDate = ${autoNowDate?.toFormattedString()},
    |)
""".trimMargin("|")

internal fun FieldSet.toSuitableStringForFile() = """
    |FieldSet(
    |    fieldName = ${fieldName?.let { "\"${it}\"" }},
    |    verboseName = "$verboseName",
    |    type = FieldType.${type.toSuitableStringForFile()},
    |    nullable = $nullable,
    |    showInPanel = $showInPanel,
    |    uploadTarget = ${uploadTarget?.toFormattedString()},
    |    allowedMimeTypes = ${allowedMimeTypes?.toSuitableStringForFile()},
    |    defaultValue = ${defaultValue?.let { "\"${it}\"" }},
    |    enumerationValues = ${enumerationValues?.toSuitableStringForFile()},
    |    limits = ${limits?.toFormattedString()},
    |    reference = ${reference?.toFormattedString()},
    |    readOnly = $readOnly,
    |    computedField = ${computedField?.let { "\"${it}\"" }},
    |    autoNowDate = ${autoNowDate?.toFormattedString()},
    |)
""".trimMargin("|")

internal fun List<String>.toSuitableStringForFile(): String = joinToString(
    prefix = "listOf(",
    postfix = ")",
    separator = ", "
) { "\"$it\"" }

internal inline fun <reified D> KSAnnotation.findArgument(property: String) =
    arguments.find { it.name?.asString() == property }?.value as? D

internal fun KSAnnotation.findArgumentIfIsNotEmpty(property: String) =
    (arguments.find { it.name?.asString() == property }?.value as? String)?.takeIf { it.isNotEmpty() }


internal inline fun <T> Iterable<T>.allIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
    val results = mutableListOf<Boolean>()
    forEachIndexed { index, t ->
        results += predicate.invoke(index, t)
    }
    return results.all { it }
}