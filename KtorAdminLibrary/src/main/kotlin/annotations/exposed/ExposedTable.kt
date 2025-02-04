package annotations.exposed


/**
 * Annotation to expose a table from the database with its metadata.
 *
 * This annotation provides metadata for a database table that can be used
 * to map it in a database schema, manage its structure, and configure additional
 * details like the primary key, names, and more.
 *
 * @param tableName The name of the table in the database.
 * @param primaryKey The primary key column of the table.
 * @param singularName (Optional) The singular name of the table (used in UI or forms).
 *                     If not provided, it will be generated from the tableName.
 * @param pluralName (Optional) The plural name of the table (used in lists or collections).
 *                   If not provided, it will be generated from the tableName.
 * @param groupName (Optional) The group name used for organizing tables.
 * @param databaseKey (Optional) A custom key used for identifying the table in queries or settings.
 *                   This refers to a database key defined in the plugin configuration.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ExposedTable(
    val tableName: String,
    val primaryKey: String,
    val singularName: String = "",
    val pluralName: String = "",
    val groupName: String = "",
    val databaseKey: String = ""
)