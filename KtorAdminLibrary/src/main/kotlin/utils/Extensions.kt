package utils

import com.google.devtools.ksp.symbol.KSAnnotation
import models.ColumnSet
import models.toFormattedString

fun ColumnSet.toSuitableStringForFile() = """
    |ColumnSet(
    |    columnName = "$columnName",
    |    type = ColumnType.${type},
    |    nullable = $nullable,
    |    showInPanel = $showInPanel,
    |    uploadTarget = ${uploadTarget?.toFormattedString()},
    |    allowedMimeTypes = ${allowedMimeTypes?.toSuitableStringForFile()},
    |    defaultValue = ${defaultValue?.let { "\"${it}\"" }},
    |    enumerationValues = ${enumerationValues?.toSuitableStringForFile()},
    |    limits = ${limits?.toFormattedString()},
    |    reference = ${reference?.toFormattedString()},
    |    readOnly = $readOnly,
    |    computedColumn = ${computedColumn?.let { "\"${it}\"" }},
    |)
""".trimMargin("|")

fun List<String>.toSuitableStringForFile(): String = joinToString(
    prefix = "listOf(",
    postfix = ")",
    separator = ", "
) { "\"$it\"" }

inline fun <reified D> KSAnnotation.findArgument(property: String) =
    arguments.find { it.name?.asString() == property }?.value as? D

fun KSAnnotation.findArgumentIfIsNotEmpty(property: String) =
    (arguments.find { it.name?.asString() == property }?.value as? String)?.takeIf { it.isNotEmpty() }


inline fun <T> Iterable<T>.allIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
    val results = mutableListOf<Boolean>()
    forEachIndexed { index, t ->
        results += predicate.invoke(index, t)
    }
    return results.all { it }
}