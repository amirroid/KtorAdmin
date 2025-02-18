package annotations.mongo

/**
 * Annotation to expose a MongoDB collection with its metadata.
 *
 * This annotation provides metadata for a MongoDB collection, allowing the
 * mapping of the collection's structure and configuration of details such
 * as primary key, names, and more. This is useful for working with MongoDB
 * collections in an organized manner.
 *
 * @param collectionName The name of the collection in MongoDB.
 * @param primaryKey The primary key field of the collection.
 * @param singularName (Optional) The singular name of the collection (used in UI or forms).
 *                     If not provided, it will be generated from the collectionName.
 * @param pluralName (Optional) The plural name of the collection (used in lists or collections).
 *                   If not provided, it will be generated from the collectionName.
 * @param groupName (Optional) The group name for organizing collections.
 * @param databaseKey (Optional) A custom key used for identifying the table in queries or settings.
 *                   This refers to a database key defined in the plugin configuration.
 * @param iconFile (Optional) The file name or path of an icon representing the table in UI components.
 * @param showInAdminPanel Determines whether this table should be displayed in the admin panel.
 *                         Default is `true`. If `false`, the table will be hidden from UI-based management.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MongoCollection(
    val collectionName: String,
    val primaryKey: String,
    val singularName: String = "",
    val pluralName: String = "",
    val groupName: String = "",
    val databaseKey: String = "",
    val iconFile: String = "",
    val showInAdminPanel: Boolean = true
)
