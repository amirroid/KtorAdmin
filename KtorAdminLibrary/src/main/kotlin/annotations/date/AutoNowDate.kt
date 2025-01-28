package annotations.date

/**
 * Marks a property to automatically use the current date as its default value.
 *
 * This annotation can be used for:
 * - **SQL columns**: Automatically initializes the field with the current date as the default value in SQL databases.
 * - **MongoDB fields**: Automatically sets the field to the current date when creating a new document in MongoDB.
 *
 * @property updateOnChange Determines if the field should update to the current date whenever the entity is modified. Defaults to `false`.
 *
 * Example usage:
 * ```
 * @AutoNowDate
 * val createdAt: LocalDate
 *
 * @AutoNowDate(updateOnChange = true)
 * val updatedAt: LocalDate
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class AutoNowDate(val updateOnChange: Boolean = false)
