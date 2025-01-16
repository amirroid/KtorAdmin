package annotations.exposed


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ExposedTable(
    val tableName: String,
    val pluralName: String,
    val groupName: String = "",
)