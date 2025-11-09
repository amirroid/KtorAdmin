package ir.amirroid.ktoradmin.annotations.exposed

/**
 * Annotation for defining a table in databases managed with Exposed.
 *
 * This annotation provides metadata for a database table in an Exposed-based setup.
 * It assists with table mapping, structure management, and additional configurations,
 * including primary key definition, naming conventions, grouping, and UI representation.
 *
 * @param tableName The name of the table in the database.
 * @param primaryKey The column serving as the primary key for the table.
 * @param singularName (Optional) The singular form of the table name, used in UI elements or forms.
 *                     If left empty, it will be automatically derived from `tableName`.
 * @param pluralName (Optional) The plural form of the table name, used in lists or collections.
 *                   If left empty, it will be automatically derived from `tableName`.
 * @param groupName (Optional) The category or section under which the table is grouped.
 * @param databaseKey (Optional) A custom identifier for referencing the table in queries or configurations.
 *                   This key corresponds to a database identifier defined in the plugin settings.
 * @param iconFile (Optional) The file name or path of an icon representing the table in UI components.
 * @param showInAdminPanel Determines whether this table should be displayed in the admin panel.
 *                         Default is `true`. If `false`, the table will be hidden from UI-based management.
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
    val showInAdminPanel: Boolean = true
)
