package annotations.exposed


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