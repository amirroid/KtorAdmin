package annotations.limit

/**
 * Annotation to specify various constraints for a column property.
 *
 * @property maxLength The maximum length allowed for the property. Default is [Int.MAX_VALUE].
 * @property minLength The minimum length allowed for the property. Default is [Int.MIN_VALUE].
 * @property regexPattern A regular expression pattern that the property's value must match. Default is an empty string.
 * @property maxCount The maximum count allowed for numeric properties. Default is [Double.MAX_VALUE].
 * @property minCount The minimum count allowed for numeric properties. Default is [Double.MIN_VALUE].
 * @property maxBytes The maximum size in bytes allowed for the property's value. Default is [Long.MAX_VALUE].
 * @property minDateRelativeToNow The minimum date allowed, specified as milliseconds relative to the current time.
 *                                Negative values allow dates in the past. Default is [Long.MAX_VALUE].
 * @property maxDateRelativeToNow The maximum date allowed, specified as milliseconds relative to the current time.
 *                                Positive values allow dates in the future. Default is [Long.MAX_VALUE].
 *
 * This annotation can be used to enforce constraints on property values in a data class,
 * helping ensure data integrity and consistency according to the specified rules.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ColumnLimits(
    val maxLength: Int = Int.MAX_VALUE,
    val minLength: Int = 0,
    val regexPattern: String = "",
    val maxCount: Double = Double.MAX_VALUE,
    val minCount: Double = Double.MIN_VALUE,
    val maxBytes: Long = Long.MAX_VALUE,
    val minDateRelativeToNow: Long = Long.MAX_VALUE,
    val maxDateRelativeToNow: Long = Long.MAX_VALUE,
    val allowedMimeTypes: Array<String> = [],
)
