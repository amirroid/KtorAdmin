package validators

import configuration.DynamicConfiguration
import getters.toTypedValue
import getters.toTypedValueNullable
import models.ColumnSet
import models.Limit
import models.field.FieldSet
import models.types.ColumnType
import models.types.FieldType
import panels.AdminJdbcTable
import repository.JdbcQueriesRepository
import translator.KtorAdminTranslator
import translator.KtorAdminTranslator.Keys
import utils.Constants
import java.net.URLConnection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

internal object Validators {
    internal fun validateColumnParameter(
        table: AdminJdbcTable,
        columnSet: ColumnSet,
        value: String?,
        primaryKey: String?,
        translator: KtorAdminTranslator,
    ): String? {
        val nullableValue =
            if (columnSet.nullable && columnSet.type != ColumnType.STRING) value?.takeIf { it.isNotEmpty() } else value

        // If the column is nullable and the value is null, no validation is needed
        if (columnSet.nullable && nullableValue == null) {
            return null
        }
        // If the column is not nullable and the value is null, return an error
        if (!columnSet.nullable && nullableValue == null) {
            return translator.translate(Keys.ERROR_NULL_FIELD)
        }

        // Ensure the field is not blank if 'blank' is set to false
        if (!columnSet.blank && value?.isBlank() == true) {
            return translator.translate(Keys.ERROR_EMPTY_FIELD)
        }

        if (columnSet.reference != null) {
            if (value.isNullOrEmpty() && !columnSet.nullable) {
                return translator.translate(Keys.ERROR_EMPTY_OR_NULL_REFERENCE)
            }
        }

        if (nullableValue != null && nullableValue.toTypedValueNullable(columnSet.type) == null) {
            return translator.translate(Keys.ERROR_INVALID_VALUE)
        }

        // Check if the column is marked as unique
        if (columnSet.unique) {
            // Convert the value to the appropriate database type and check for duplicates
            val typedValue = value?.toTypedValue(columnSet.type)
            if (JdbcQueriesRepository.checkExistSameData(table, columnSet, typedValue, primaryKey)) {
                return translator.translate(Keys.ERROR_UNIQUE_FIELD)
            }
        }

        // Perform validation based on the column type
        return when (columnSet.type) {
            ColumnType.STRING -> validateString(value!!, columnSet.limits, translator)
            ColumnType.INTEGER -> validateInteger(value!!, columnSet.limits, translator)
            ColumnType.UINTEGER -> validateUnsignedInteger(value!!, columnSet.limits, translator)
            ColumnType.SHORT -> validateShort(value!!, columnSet.limits, translator)
            ColumnType.USHORT -> validateUnsignedShort(value!!, columnSet.limits, translator)
            ColumnType.LONG -> validateLong(value!!, columnSet.limits, translator)
            ColumnType.ULONG -> validateUnsignedLong(value!!, columnSet.limits, translator)
            ColumnType.DOUBLE -> validateDouble(value!!, columnSet.limits, translator)
            ColumnType.FLOAT -> validateFloat(value!!, columnSet.limits, translator)
            ColumnType.BIG_DECIMAL -> validateBigDecimal(value!!, columnSet.limits, translator)
            ColumnType.CHAR -> validateChar(value!!, translator)
            ColumnType.BOOLEAN -> validateBoolean(value!!, translator)
            ColumnType.ENUMERATION -> validateEnumeration(value!!, columnSet.enumerationValues, translator)
            ColumnType.DATE -> validateDate(value!!, columnSet.limits, translator)
            ColumnType.DURATION -> validateDuration(value!!, translator)
            ColumnType.DATETIME -> validateDateTime(value!!, columnSet.limits, translator)
            else -> null // No validation needed for NOT_AVAILABLE
        }
    }

    internal fun validateFieldParameter(fieldSet: FieldSet, value: String?, translator: KtorAdminTranslator): String? {
        val nullableValue =
            if (fieldSet.nullable && fieldSet.type !is FieldType.String) value?.takeIf { it.isNotEmpty() } else value

        // If the column is nullable and the value is null, no validation is needed
        if (fieldSet.nullable && nullableValue == null) {
            return null
        }
        // If the column is not nullable and the value is null, return an error
        if (!fieldSet.nullable && nullableValue == null) {
            return translator.translate(Keys.ERROR_NULL_FIELD)
        }

//        // Ensure the field is not blank if 'blank' is set to false
//        if (!fieldSet.blank && value?.isBlank() == true) {
//            return translator.translate(Keys.ERROR_EMPTY_FIELD)
//        }

        // Perform validation based on the column type
        return when (fieldSet.type) {
            FieldType.String -> validateString(value!!, fieldSet.limits, translator)
            FieldType.Integer -> validateInteger(value!!, fieldSet.limits, translator)
            FieldType.Long -> validateLong(value!!, fieldSet.limits, translator)
            FieldType.Double -> validateDouble(value!!, fieldSet.limits, translator)
            FieldType.Float -> validateFloat(value!!, fieldSet.limits, translator)
            FieldType.Boolean -> validateBoolean(value!!, translator)
            FieldType.Date -> validateDate(value!!, fieldSet.limits, translator)
            FieldType.DateTime -> validateDateTime(value!!, fieldSet.limits, translator)
            FieldType.Enumeration -> validateEnumeration(value!!, fieldSet.enumerationValues, translator)
            FieldType.Instant -> validateInstant(value!!, fieldSet.limits, translator)
            FieldType.Decimal128 -> validateDecimal128(value!!, fieldSet.limits, translator)
            else -> null // No validation needed for NotAvailable type
        }
    }

    // Validation for STRING type
    private fun validateString(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        limits?.let {
            if (value.length > it.maxLength) return translator.translate(Keys.ERROR_MAX_LENGTH_EXCEEDED, mapOf("length" to it.maxLength.toString()))
            if (value.length < it.minLength) return translator.translate(Keys.ERROR_MIN_LENGTH_NOT_MET, mapOf("length" to it.minLength.toString()))
            if (it.regexPattern != null && !value.matches(Regex(it.regexPattern))) {
                return translator.translate(Keys.ERROR_REGEX_MISMATCH, mapOf("pattern" to it.regexPattern))
            }
        }
        return null
    }

    // Validation for INTEGER type
    private fun validateInteger(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val intValue = value.toIntOrNull() ?: return translator.translate(Keys.ERROR_INVALID_INTEGER)
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
            val minCount = it.minCount.coerceAtLeast(Int.MIN_VALUE.toDouble()).toInt()

            if (intValue > maxCount) return translator.translate(Keys.ERROR_INTEGER_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (intValue < minCount) return translator.translate(Keys.ERROR_INTEGER_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }

    // Validation for UINTEGER type
    private fun validateUnsignedInteger(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val uintValue = value.toUIntOrNull() ?: return translator.translate(Keys.ERROR_INVALID_UNSIGNED_INTEGER)
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(UInt.MAX_VALUE.toDouble()).toUInt()
            val minCount = it.minCount.coerceAtLeast(UInt.MIN_VALUE.toDouble()).toUInt()

            if (uintValue > maxCount) return translator.translate(Keys.ERROR_UNSIGNED_INTEGER_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (uintValue < minCount) return translator.translate(Keys.ERROR_UNSIGNED_INTEGER_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }

    // Validation for SHORT type
    private fun validateShort(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val shortValue = value.toShortOrNull() ?: return translator.translate(Keys.ERROR_INVALID_SHORT)
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(Short.MAX_VALUE.toDouble()).toInt().toShort()
            val minCount = it.minCount.coerceAtLeast(Short.MIN_VALUE.toDouble()).toInt().toShort()

            if (shortValue > maxCount) return translator.translate(Keys.ERROR_SHORT_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (shortValue < minCount) return translator.translate(Keys.ERROR_SHORT_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }

    // Validation for USHORT type
    private fun validateUnsignedShort(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val ushortValue = value.toUShortOrNull() ?: return translator.translate(Keys.ERROR_INVALID_UNSIGNED_SHORT)
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(UShort.MAX_VALUE.toDouble()).toInt().toUShort()
            val minCount = it.minCount.coerceAtLeast(UShort.MIN_VALUE.toDouble()).toInt().toUShort()

            if (ushortValue > maxCount) return translator.translate(Keys.ERROR_UNSIGNED_SHORT_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (ushortValue < minCount) return translator.translate(Keys.ERROR_UNSIGNED_SHORT_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }

    // Validation for LONG type
    private fun validateLong(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val longValue = value.toLongOrNull() ?: return translator.translate(Keys.ERROR_INVALID_LONG)
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(Long.MAX_VALUE.toDouble()).toLong()
            val minCount = it.minCount.coerceAtLeast(Long.MIN_VALUE.toDouble()).toLong()

            if (longValue > maxCount) return translator.translate(Keys.ERROR_LONG_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (longValue < minCount) return translator.translate(Keys.ERROR_LONG_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }

    // Validation for ULONG type
    private fun validateUnsignedLong(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val ulongValue = value.toULongOrNull() ?: return translator.translate(Keys.ERROR_INVALID_UNSIGNED_LONG)
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(ULong.MAX_VALUE.toDouble()).toULong()
            val minCount = it.minCount.coerceAtLeast(ULong.MIN_VALUE.toDouble()).toULong()

            if (ulongValue > maxCount) return translator.translate(Keys.ERROR_UNSIGNED_LONG_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (ulongValue < minCount) return translator.translate(Keys.ERROR_UNSIGNED_LONG_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }

    // Validation for DOUBLE type
    private fun validateDouble(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val doubleValue = value.toDoubleOrNull() ?: return translator.translate(Keys.ERROR_INVALID_DOUBLE)
        limits?.let {
            val maxCount = it.maxCount
            val minCount = it.minCount
            if (doubleValue > maxCount) return translator.translate(Keys.ERROR_DOUBLE_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (doubleValue < minCount) return translator.translate(Keys.ERROR_DOUBLE_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }

    // Validation for FLOAT type
    private fun validateFloat(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val floatValue = value.toFloatOrNull() ?: return translator.translate(Keys.ERROR_INVALID_FLOAT)
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(Float.MAX_VALUE.toDouble()).toFloat()
            val minCount = it.minCount.coerceAtLeast(-Float.MAX_VALUE.toDouble()).toFloat()

            if (floatValue > maxCount) return translator.translate(Keys.ERROR_FLOAT_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (floatValue < minCount) return translator.translate(Keys.ERROR_FLOAT_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }

    // Validation for BIG_DECIMAL type
    private fun validateBigDecimal(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val bigDecimalValue = value.toBigDecimalOrNull() ?: return translator.translate(Keys.ERROR_INVALID_BIG_DECIMAL)
        limits?.let {
            if (bigDecimalValue > it.maxCount.toBigDecimal()) return translator.translate(Keys.ERROR_BIG_DECIMAL_MAX_EXCEEDED, mapOf("max" to it.maxCount.toString()))
            if (bigDecimalValue < it.minCount.toBigDecimal()) return translator.translate(Keys.ERROR_BIG_DECIMAL_MIN_EXCEEDED, mapOf("min" to it.minCount.toString()))
        }
        return null
    }

    // Validation for CHAR type
    private fun validateChar(value: String, translator: KtorAdminTranslator): String? {
        if (value.length != 1) return translator.translate(Keys.ERROR_INVALID_CHAR)
        return null
    }

    // Validation for BOOLEAN type
    private fun validateBoolean(value: String, translator: KtorAdminTranslator): String? {
        if (value !in Constants.booleanForms) return translator.translate(Keys.ERROR_INVALID_BOOLEAN)
        return null
    }

    // Validation for ENUMERATION type
    private fun validateEnumeration(value: String, enumValues: List<String>?, translator: KtorAdminTranslator): String? {
        if (enumValues == null || value !in enumValues) return translator.translate(Keys.ERROR_INVALID_ENUMERATION, mapOf("values" to (enumValues?.joinToString() ?: "")))
        return null
    }

    // Validation for DATE type
    private fun validateDate(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        try {
            // Parse the input value to LocalDate
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val date = LocalDate.parse(value, formatter)

            // Check if limits are provided
            limits?.let {
                // Get the current date based on the specified time zone
                val now = LocalDate.now(DynamicConfiguration.timeZone)

                // Convert minDateRelativeToNow and maxDateRelativeToNow from milliseconds to days
                val minDateInDays = java.time.Duration.ofMillis(it.minDateRelativeToNow).toDays()
                val maxDateInDays = java.time.Duration.ofMillis(it.maxDateRelativeToNow).toDays()

                // Check if the date is before the minimum allowed date
                if (it.minDateRelativeToNow != Long.MAX_VALUE && date.isBefore(now.minusDays(minDateInDays))) {
                    val minDate = now.minusDays(minDateInDays)
                    return translator.translate(Keys.ERROR_DATE_BEFORE_MIN, mapOf("date" to minDate.toString()))
                }

                // Check if the date is after the maximum allowed date
                if (it.maxDateRelativeToNow != Long.MAX_VALUE && date.isAfter(now.plusDays(maxDateInDays))) {
                    val maxDate = now.plusDays(maxDateInDays)
                    return translator.translate(Keys.ERROR_DATE_AFTER_MAX, mapOf("date" to maxDate.toString()))
                }
            }
        } catch (e: Exception) {
            // Return an error message if the value is not a valid date
            return translator.translate(Keys.ERROR_INVALID_DATE)
        }

        // Return null if the date is valid
        return null
    }

    // Validation for DURATION type
    private fun validateDuration(value: String, translator: KtorAdminTranslator): String? {
        try {
            Duration.parse(value)
        } catch (e: Exception) {
            return translator.translate(Keys.ERROR_INVALID_DURATION)
        }
        return null
    }

    // Validation for DATETIME type
    private fun validateDateTime(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        try {
            // Parse the input value to Instant
            val formatter = DateTimeFormatter.ofPattern(Constants.LOCAL_DATETIME_FORMAT)
            val parsedDate = LocalDateTime.parse(value, formatter)
            val instant = parsedDate.atZone(DynamicConfiguration.timeZone).toInstant()

            // If limits are provided, validate the date against them
            if (limits != null) {
                val now = Instant.now()

                // Calculate the minimum and maximum allowed dates
                val minDate = now.plusMillis(limits.minDateRelativeToNow)
                val maxDate = now.plusMillis(limits.maxDateRelativeToNow)

                // Check if the parsed date is before the minimum allowed date
                if (limits.minDateRelativeToNow != Long.MAX_VALUE && instant.isBefore(minDate)) {
                    val minDateTime = minDate.atOffset(ZoneOffset.of(DynamicConfiguration.timeZone.id))
                    return translator.translate(Keys.ERROR_DATETIME_BEFORE_MIN, mapOf("datetime" to minDateTime.toString()))
                }
                // Check if the parsed date is after the maximum allowed date
                if (limits.maxDateRelativeToNow != Long.MAX_VALUE && instant.isAfter(maxDate)) {
                    val maxDateTime = maxDate.atOffset(ZoneOffset.of(DynamicConfiguration.timeZone.id))
                    return translator.translate(Keys.ERROR_DATETIME_AFTER_MAX, mapOf("datetime" to maxDateTime.toString()))
                }
            }
        } catch (e: Exception) {
            // Return an error message if the value is not a valid datetime
            return translator.translate(Keys.ERROR_INVALID_DATETIME, mapOf("format" to Constants.LOCAL_DATETIME_FORMAT))
        }

        // Return null if the value is valid
        return null
    }

    // Validation for FILE and BINARY size
    fun validateBytesSize(bytesSize: Long, limits: Limit?, translator: KtorAdminTranslator): String? {
        if (limits == null) return null
        if (bytesSize > limits.maxBytes) return translator.translate(Keys.ERROR_FILE_SIZE_EXCEEDED, mapOf("size" to limits.maxBytes.toString()))
        return null
    }

    // Validation for FILE mimetype
    fun validateMimeType(fileName: String?, limits: Limit?, translator: KtorAdminTranslator): String? {
        if (limits?.allowedMimeTypes == null) return null
        val mimeType = getMimeType(fileName ?: return null)
        if (mimeType !in limits.allowedMimeTypes) {
            return translator.translate(Keys.ERROR_INVALID_MIME_TYPE, mapOf(
                "file" to fileName,
                "types" to limits.allowedMimeTypes.joinToString()
            ))
        }
        return null
    }

    private fun getMimeType(fileName: String): String {
        return URLConnection.guessContentTypeFromName(fileName) ?: "unknown"
    }

    // Validation for Instant type
    private fun validateInstant(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        // Return null if the value is valid
        return validateDateTime(value, limits, translator)
    }

    // Validation for Decimal28 type
    private fun validateDecimal128(value: String, limits: Limit?, translator: KtorAdminTranslator): String? {
        val decimalValue = value.toBigDecimalOrNull() ?: return translator.translate(Keys.ERROR_INVALID_DECIMAL128)
        limits?.let {
            val maxCount = it.maxCount.toBigDecimal()
            val minCount = it.minCount.toBigDecimal()

            if (decimalValue > maxCount) return translator.translate(Keys.ERROR_DECIMAL128_MAX_EXCEEDED, mapOf("max" to maxCount.toString()))
            if (decimalValue < minCount) return translator.translate(Keys.ERROR_DECIMAL128_MIN_EXCEEDED, mapOf("min" to minCount.toString()))
        }
        return null
    }
}