package validators

import configuration.DynamicConfiguration
import models.ColumnSet
import models.Limit
import models.types.ColumnType
import utils.Constants
import java.net.URLConnection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

internal object Validators {
    internal fun validateColumnParameter(columnSet: ColumnSet, value: String?): String? {
        // If the column is nullable and the value is null, no validation is needed
        if (columnSet.nullable && value == null) {
            return null
        }
        // If the column is not nullable and the value is null, return an error
        if (!columnSet.nullable && value == null) {
            return "The field cannot be null"
        }

        if (columnSet.reference != null) {
            if (value.isNullOrEmpty()) {
                return "The field cannot be empty or null when a reference is required."
            }
        }

        // Perform validation based on the column type
        return when (columnSet.type) {
            ColumnType.STRING -> validateString(value!!, columnSet.limits)
            ColumnType.INTEGER -> validateInteger(value!!, columnSet.limits)
            ColumnType.UINTEGER -> validateUnsignedInteger(value!!, columnSet.limits)
            ColumnType.SHORT -> validateShort(value!!, columnSet.limits)
            ColumnType.USHORT -> validateUnsignedShort(value!!, columnSet.limits)
            ColumnType.LONG -> validateLong(value!!, columnSet.limits)
            ColumnType.ULONG -> validateUnsignedLong(value!!, columnSet.limits)
            ColumnType.DOUBLE -> validateDouble(value!!, columnSet.limits)
            ColumnType.FLOAT -> validateFloat(value!!, columnSet.limits)
            ColumnType.BIG_DECIMAL -> validateBigDecimal(value!!, columnSet.limits)
            ColumnType.CHAR -> validateChar(value!!)
            ColumnType.BOOLEAN -> validateBoolean(value!!)
            ColumnType.ENUMERATION -> validateEnumeration(value!!, columnSet.enumerationValues)
            ColumnType.DATE -> validateDate(value!!, columnSet.limits)
            ColumnType.DURATION -> validateDuration(value!!)
            ColumnType.DATETIME -> validateDateTime(value!!, columnSet.limits)
            else -> null // No validation needed for NOT_AVAILABLE
        }
    }

    // Validation for STRING type
    private fun validateString(value: String, limits: Limit?): String? {
        limits?.let {
            if (value.length > it.maxLength) return "Value exceeds maximum length of ${it.maxLength}"
            if (value.length < it.minLength) return "Value is shorter than minimum length of ${it.minLength}"
            if (it.regexPattern != null && !value.matches(Regex(it.regexPattern))) {
                return "Value does not match the required pattern"
            }
        }
        return null
    }

    // Validation for INTEGER type
    private fun validateInteger(value: String, limits: Limit?): String? {
        val intValue = value.toIntOrNull() ?: return "Value should be a valid integer"
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
            val minCount = it.minCount.coerceAtLeast(Int.MIN_VALUE.toDouble()).toInt()

            if (intValue > maxCount) return "Value exceeds maximum count of $maxCount"
            if (intValue < minCount) return "Value is less than minimum count of $minCount"
        }
        return null
    }

    // Validation for UINTEGER type
    private fun validateUnsignedInteger(value: String, limits: Limit?): String? {
        val uintValue = value.toUIntOrNull() ?: return "Value should be a valid unsigned integer"
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(UInt.MAX_VALUE.toDouble()).toUInt()
            val minCount = it.minCount.coerceAtLeast(UInt.MIN_VALUE.toDouble()).toUInt()

            if (uintValue > maxCount) return "Value exceeds maximum count of $maxCount"
            if (uintValue < minCount) return "Value is less than minimum count of $minCount"
        }
        return null
    }

    // Validation for SHORT type
    private fun validateShort(value: String, limits: Limit?): String? {
        val shortValue = value.toShortOrNull() ?: return "Value should be a valid short"
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(Short.MAX_VALUE.toDouble()).toInt().toShort()
            val minCount = it.minCount.coerceAtLeast(Short.MIN_VALUE.toDouble()).toInt().toShort()

            if (shortValue > maxCount) return "Value exceeds maximum count of $maxCount"
            if (shortValue < minCount) return "Value is less than minimum count of $minCount"
        }
        return null
    }

    // Validation for USHORT type
    private fun validateUnsignedShort(value: String, limits: Limit?): String? {
        val ushortValue = value.toUShortOrNull() ?: return "Value should be a valid unsigned short"
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(UShort.MAX_VALUE.toDouble()).toInt().toUShort()
            val minCount = it.minCount.coerceAtLeast(UShort.MIN_VALUE.toDouble()).toInt().toUShort()

            if (ushortValue > maxCount) return "Value exceeds maximum count of $maxCount"
            if (ushortValue < minCount) return "Value is less than minimum count of $minCount"
        }
        return null
    }

    // Validation for LONG type
    private fun validateLong(value: String, limits: Limit?): String? {
        val longValue = value.toLongOrNull() ?: return "Value should be a valid long"
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(Long.MAX_VALUE.toDouble()).toLong()
            val minCount = it.minCount.coerceAtLeast(Long.MIN_VALUE.toDouble()).toLong()

            if (longValue > maxCount) return "Value exceeds maximum count of $maxCount"
            if (longValue < minCount) return "Value is less than minimum count of $minCount"
        }
        return null
    }

    // Validation for ULONG type
    private fun validateUnsignedLong(value: String, limits: Limit?): String? {
        val ulongValue = value.toULongOrNull() ?: return "Value should be a valid unsigned long"
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(ULong.MAX_VALUE.toDouble()).toULong()
            val minCount = it.minCount.coerceAtLeast(ULong.MIN_VALUE.toDouble()).toULong()

            if (ulongValue > maxCount) return "Value exceeds maximum count of $maxCount"
            if (ulongValue < minCount) return "Value is less than minimum count of $minCount"
        }
        return null
    }

    // Validation for DOUBLE type
    private fun validateDouble(value: String, limits: Limit?): String? {
        val doubleValue = value.toDoubleOrNull() ?: return "Value should be a valid double"
        limits?.let {
            val maxCount = it.maxCount
            val minCount = it.minCount
            if (doubleValue > maxCount) return "Value exceeds maximum count of $maxCount"
            if (doubleValue < minCount) return "Value is less than minimum count of $minCount"
        }
        return null
    }

    // Validation for FLOAT type
    private fun validateFloat(value: String, limits: Limit?): String? {
        val floatValue = value.toFloatOrNull() ?: return "Value should be a valid float"
        limits?.let {
            val maxCount = it.maxCount.coerceAtMost(Float.MAX_VALUE.toDouble()).toFloat()
            val minCount = it.minCount.coerceAtLeast(-Float.MAX_VALUE.toDouble()).toFloat()

            if (floatValue > maxCount) return "Value exceeds maximum count of $maxCount"
            if (floatValue < minCount) return "Value is less than minimum count of $minCount"
        }
        return null
    }

    // Validation for BIG_DECIMAL type
    private fun validateBigDecimal(value: String, limits: Limit?): String? {
        val bigDecimalValue = value.toBigDecimalOrNull() ?: return "Value should be a valid big decimal"
        limits?.let {
            if (bigDecimalValue > it.maxCount.toBigDecimal()) return "Value exceeds maximum count of ${it.maxCount}"
            if (bigDecimalValue < it.minCount.toBigDecimal()) return "Value is less than minimum count of ${it.minCount}"
        }
        return null
    }

    // Validation for CHAR type
    private fun validateChar(value: String): String? {
        if (value.length != 1) return "Value should be a single character"
        return null
    }


    // Validation for BOOLEAN type
    private fun validateBoolean(value: String): String? {
        if (value !in Constants.booleanForms) return "Value should be on or off"
        return null
    }

    // Validation for ENUMERATION type
    private fun validateEnumeration(value: String, enumValues: List<String>?): String? {
        if (enumValues == null || value !in enumValues) return "Value should be one of ${enumValues ?: "allowed values"}"
        return null
    }

    // Validation for DATE type
    private fun validateDate(value: String, limits: Limit?): String? {
        try {
            // Parse the input value to LocalDate
            val date = LocalDate.parse(value)

            // Check if limits are provided
            limits?.let {
                // Get the current date based on the specified time zone
                val now = LocalDate.now(DynamicConfiguration.timeZone)

                // Convert minDateRelativeToNow and maxDateRelativeToNow from milliseconds to days
                val minDateInDays = java.time.Duration.ofMillis(it.minDateRelativeToNow).toDays()
                val maxDateInDays = java.time.Duration.ofMillis(it.maxDateRelativeToNow).toDays()

                // Check if the date is before the minimum allowed date
                if (it.minDateRelativeToNow != Long.MAX_VALUE && date.isBefore(now.minusDays(minDateInDays))) {
                    return "Date is before the allowed minimum date (${now.minusDays(minDateInDays)})"
                }

                // Check if the date is after the maximum allowed date
                if (it.maxDateRelativeToNow != Long.MAX_VALUE && date.isAfter(now.plusDays(maxDateInDays))) {
                    return "Date is after the allowed maximum date (${now.plusDays(maxDateInDays)})"
                }
            }
        } catch (e: Exception) {
            // Return an error message if the value is not a valid date
            return "Value should be a valid date (yyyy-MM-dd)"
        }

        // Return null if the date is valid
        return null
    }

    // Validation for DURATION type
    private fun validateDuration(value: String): String? {
        try {
            Duration.parse(value)
        } catch (e: Exception) {
            return "Value should be a valid duration (e.g., PT1H30M)"
        }
        return null
    }

    // Validation for DATETIME type
    private fun validateDateTime(value: String, limits: Limit?): String? {
        try {
            // Parse the input value to Instant
            val parsedDate = LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
                .atOffset(ZoneOffset.of(DynamicConfiguration.timeZone.id))
                .toInstant()

            // If limits are provided, validate the date against them
            if (limits != null) {
                val now = Instant.now()

                // Calculate the minimum and maximum allowed dates
                val minDate = now.plusMillis(limits.minDateRelativeToNow)
                val maxDate = now.plusMillis(limits.maxDateRelativeToNow)

                // Check if the parsed date is before the minimum allowed date
                if (limits.minDateRelativeToNow != Long.MAX_VALUE && parsedDate.isBefore(minDate)) {
                    return "Value should not be before ${minDate.atOffset(ZoneOffset.of(DynamicConfiguration.timeZone.id))}"
                }
                // Check if the parsed date is after the maximum allowed date
                if (limits.maxDateRelativeToNow != Long.MAX_VALUE && parsedDate.isAfter(maxDate)) {
                    return "Value should not be after ${maxDate.atOffset(ZoneOffset.of(DynamicConfiguration.timeZone.id))}"
                }
            }
        } catch (e: Exception) {
            // Return an error message if the value is not a valid datetime
            return "Value should be a valid datetime (yyyy-MM-dd'T'HH:mm:ss)"
        }

        // Return null if the value is valid
        return null
    }

    // Validation for FILE and BINARY size
    fun validateBytesSize(bytesSize: Long, limits: Limit?): String? {
        if (limits == null) return null
        if (bytesSize > limits.maxBytes) return "Size exceeds the maximum allowed size of ${limits.maxBytes} bytes"
        return null
    }

    // Validation for FILE mimetype
    fun validateMimeType(fileName: String?, limits: Limit?): String? {
        if (limits?.allowedMimeTypes == null) return null
        if (fileName == null) {
            return "File name cannot be null"
        }
        val mimeType = getMimeType(fileName)
        if (mimeType !in limits.allowedMimeTypes) {
            return "Invalid MIME type for file ${fileName}. Allowed types are ${limits.allowedMimeTypes.joinToString()}"
        }
        return null
    }

    private fun getMimeType(fileName: String): String {
        return URLConnection.guessContentTypeFromName(fileName) ?: "unknown"
    }
}