package annotations.info

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ColumnInfo(
    val columnName: String = "",
    val verboseName: String = "",
    val defaultValue: String = "",
    val nullable: Boolean = false,
    val readOnly: Boolean = false
)