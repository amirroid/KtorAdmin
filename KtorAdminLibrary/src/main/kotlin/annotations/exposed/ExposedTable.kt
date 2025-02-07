package annotations.exposed


/**
 * Annotation for exposing a database table along with its metadata.
 *
 * This annotation defines essential metadata for a database table, facilitating its mapping
 * within a database schema, managing its structure, and configuring additional attributes
 * such as primary key, naming conventions, and grouping.
 *
 * @param tableName The name of the table in the database.
 * @param primaryKey The column serving as the primary key for the table.
 * @param singularName (Optional) The singular form of the table name, used in UI elements or forms.
 *                     If left empty, it will be automatically derived from `tableName`.
 * @param pluralName (Optional) The plural form of the table name, used in lists or collections.
 *                   If left empty, it will be automatically derived from `tableName`.
 * @param groupName (Optional) The group under which the table is categorized.
 * @param databaseKey (Optional) A custom identifier for referencing the table in queries or configurations.
 *                   This key corresponds to a database identifier defined in the plugin settings.
 * @param iconFile (Optional) The file name or path of an icon representing the table in UI components.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ExposedTable(
    val tableName: String,
    val primaryKey: String,
    val singularName: String = "",
    val pluralName: String = "",
    val groupName: String = "",
    val databaseKey: String = "",
    val iconFile: String = "",
)