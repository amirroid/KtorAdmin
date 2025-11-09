package ir.amirroid.ktoradmin.annotations.type

/**
 * Annotation for manually setting the type of field in MongoDB.
 * The `type` value should be the name of one of the predefined types in [FieldType].
 *
 * Example:
 * ```kotlin
 * @OverrideFieldType("DATETIME")
 * val myField: LocalDate
 * ```
 *
 * @param type The name of the field type, which must be taken from [ir.amirroid.ktoradmin.models.types.FieldType].
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class OverrideFieldType(val type: String)