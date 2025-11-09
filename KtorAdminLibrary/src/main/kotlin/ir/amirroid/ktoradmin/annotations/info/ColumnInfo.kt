package ir.amirroid.ktoradmin.annotations.info

/**
 * Annotation to provide metadata about a database column.
 *
 * @property columnName The name of the column in the database. Defaults to an empty string.
 * @property verboseName A user-friendly name for the column, useful for UI representation.
 * @property defaultValue The default value for the column if no value is provided.
 * @property nullable Indicates whether the column can have null values.
 * @property unique Specifies whether the column should have unique values across the table.
 * @property blank Determines if the column can be left empty.
 * @property readOnly Marks the column as read-only, meaning it cannot be modified after creation.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ColumnInfo(
    val columnName: String = "",
    val verboseName: String = "",
    val defaultValue: String = "",
    val nullable: Boolean = false,
    val unique: Boolean = false,
    val blank: Boolean = true,
    val readOnly: Boolean = false
)