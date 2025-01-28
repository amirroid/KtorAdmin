package annotations.date

/**
 * Marks a property to automatically use the current date as its default value.
 *
 * This annotation can be applied to:
 * - SQL columns: Indicates that the field should be initialized with the current date as the default value in SQL databases.
 * - MongoDB fields: Automatically sets the field to the current date when creating a document in MongoDB.
 *
 * Usage:
 * ```
 * @AutoNowDate
 * val createdAt: LocalDate
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class AutoNowDate