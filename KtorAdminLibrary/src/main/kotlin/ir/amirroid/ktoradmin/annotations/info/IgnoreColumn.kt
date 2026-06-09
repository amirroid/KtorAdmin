package ir.amirroid.ktoradmin.annotations.info

/**
 * This column will NOT be shown in the panel.
 *
 * Important: It still exists in the database schema and must remain valid.
 * It should have a default value, be nullable, or be auto-generated
 * to avoid runtime errors when data is inserted.
 *
 * Example:
 * ```
 * object Users : IntIdTable() {
 *
 *     @IgnoreColumn
 *     val id = integer("id").autoIncrement()
 *
 *     val name = varchar("name", 100)
 * }
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class IgnoreColumn
