package annotations.info

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ColumnInfo(val columnName: String, val showInPanel: Boolean = true)