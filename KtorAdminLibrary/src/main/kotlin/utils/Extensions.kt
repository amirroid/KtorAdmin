package utils

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import models.ColumnSet
import models.common.toFormattedString
import models.date.toFormattedString
import models.field.FieldSet
import models.toFormattedString
import models.types.toSuitableStringForFile

/**
 * Utility functions for converting model objects and collections to formatted strings
 * suitable for code generation and file output.
 */

/**
 * Converts a ColumnSet instance to a formatted string representation.
 * This generates a multi-line string that can be used in generated Kotlin files,
 * properly formatting all properties of the ColumnSet including nullable values
 * and nested objects.
 *
 * @return A properly formatted string representation of the ColumnSet
 */
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
    |    hasRichEditor = ${hasRichEditor},
    |    unique = ${unique},
    |    blank = ${blank},
    |    hasConfirmation = ${hasConfirmation},
    |    valueMapper = ${valueMapper?.let { "\"${it}\"" }},
    |    preview = ${preview?.let { "\"${it}\"" }},
    |)
""".trimMargin("|")

/**
 * Converts a FieldSet instance to a formatted string representation.
 * Generates a multi-line string suitable for use in generated Kotlin files,
 * properly handling all FieldSet properties including nullable values and
 * complex nested objects.
 *
 * @return A properly formatted string representation of the FieldSet
 */
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
    |    readOnly = $readOnly,
    |    computedField = ${computedField?.let { "\"${it}\"" }},
    |    autoNowDate = ${autoNowDate?.toFormattedString()},
    |)
""".trimMargin("|")

/**
 * Converts a List of Strings to a formatted string representation.
 * Creates a string in the format: listOf("item1", "item2", ...)
 * suitable for use in generated Kotlin code.
 *
 * @return A string representation of the list in Kotlin listOf format
 */
internal fun List<String>.toSuitableStringForFile(): String = joinToString(
    prefix = "listOf(",
    postfix = ")",
    separator = ", "
) { "\"$it\"" }

/**
 * Finds and casts an argument value from a KSAnnotation by property name.
 *
 * @param property The name of the argument to find
 * @return The value of the argument cast to type D, or null if not found or wrong type
 */
internal inline fun <reified D> KSAnnotation.findArgument(property: String) =
    arguments.find { it.name?.asString() == property }?.value as? D

/**
 * Finds a string argument from a KSAnnotation and returns it only if non-empty.
 *
 * @param property The name of the argument to find
 * @return The string value if found and non-empty, null otherwise
 */
internal fun KSAnnotation.findArgumentIfIsNotEmpty(property: String) =
    (arguments.find { it.name?.asString() == property }?.value as? String)?.takeIf { it.isNotEmpty() }

/**
 * Checks if all elements in an Iterable satisfy a predicate that takes both the index
 * and element as parameters.
 *
 * @param predicate Function that takes the index and element and returns a Boolean
 * @return true if all elements satisfy the predicate, false otherwise
 */
internal inline fun <T> Iterable<T>.allIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
    val results = mutableListOf<Boolean>()
    forEachIndexed { index, t ->
        results += predicate.invoke(index, t)
    }
    return results.all { it }
}

/**
 * Formats a Double as an integer string if it has no decimal part,
 * otherwise returns the standard double string representation.
 *
 * @return A string representation of the number without unnecessary decimal places
 */
internal fun Double.formatAsIntegerIfPossible(): String {
    return if (this % 1.0 == 0.0) toLong().toString() else toString()
}


internal fun KSClassDeclaration.toTableName() = simpleName.asString().split(" ").joinToString("_") { it.lowercase() }