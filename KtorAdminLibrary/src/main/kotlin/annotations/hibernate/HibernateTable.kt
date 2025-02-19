package annotations.hibernate

/**
 * Annotation for defining a table in databases managed with Hibernate.
 *
 * This annotation provides metadata for a database table in a Hibernate-based database setup.
 * It helps with table mapping, structure management, and configuration of additional attributes
 * such as primary key, naming conventions, and grouping.
 *
 * @param singularName (Optional) The singular form of the table name, used in UI elements or forms.
 *                     If left empty, it will be automatically derived from `tableName`.
 * @param pluralName (Optional) The plural form of the table name, used in lists or collections.
 *                   If left empty, it will be automatically derived from `tableName`.
 * @param groupName (Optional) The group under which the table is categorized.
 * @param databaseKey (Optional) A custom identifier for referencing the table in queries or configurations.
 *                   This key corresponds to a database identifier defined in the plugin settings.
 * @param iconFile (Optional) The file name or path of an icon representing the table in UI components.
 * @param showInAdminPanel Determines whether this table should be displayed in the admin panel.
 *                         Default is `true`. If `false`, the table will be hidden from UI-based management.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class HibernateTable(
    val singularName: String = "",
    val pluralName: String = "",
    val groupName: String = "",
    val databaseKey: String = "",
    val iconFile: String = "",
    val showInAdminPanel: Boolean = true
)