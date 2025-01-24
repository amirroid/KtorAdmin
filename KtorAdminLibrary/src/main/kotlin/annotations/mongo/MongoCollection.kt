package annotations.mongo

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MongoCollection(
    val collectionName: String,
    val primaryKey: String,
    val singularName: String = "",
    val pluralName: String = "",
    val groupName: String = "",
    val databaseKey: String = ""
)