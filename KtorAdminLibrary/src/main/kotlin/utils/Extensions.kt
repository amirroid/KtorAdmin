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


fun String.extractTextInCurlyBraces(): List<String> {
    val regex = "\\{(.*?)}".toRegex()
    return regex.findAll(this).map { it.groupValues[1] }.toList()
}

fun populateTemplate(template: String, values: Map<String, String?>): String {
    val regexPattern = "\\{(.*?)}".toRegex()
    return regexPattern.replace(template) { matchResult ->
        val key = matchResult.groupValues[1]
        values[key] ?: matchResult.value
    }
}

inline fun <T> Iterable<T>.allIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
    val results = mutableListOf<Boolean>()
    forEachIndexed { index, t ->
        results += predicate.invoke(index, t)
    }
    return results.all { it }
}