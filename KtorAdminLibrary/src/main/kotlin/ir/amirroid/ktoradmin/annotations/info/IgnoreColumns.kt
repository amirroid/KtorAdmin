package ir.amirroid.ktoradmin.annotations.info

/**
 * Columns listed here will NOT be shown in the panel.
 *
 * Important: The table schema and database must still match these columns.
 * They should either have a default value or be auto-generated,
 * otherwise inserts/queries may fail.
 *
 * Example:
 * ```
 * @ExposedTable(tableName = "projects", primaryKey = "id")
 * @IgnoreColumns(columnNames = ["id"])
 * object Projects : UuidTable("projects") {
 *     val name = varchar("name", 100)
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class IgnoreColumns(
    val columnNames: Array<String>,
)
