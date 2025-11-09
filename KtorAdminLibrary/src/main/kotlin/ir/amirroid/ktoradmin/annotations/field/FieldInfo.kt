package ir.amirroid.ktoradmin.annotations.field

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class FieldInfo(
    val fieldName: String = "",
    val verboseName: String = "",
    val defaultValue: String = "",
    val nullable: Boolean = false,
    val readOnly: Boolean = false
)