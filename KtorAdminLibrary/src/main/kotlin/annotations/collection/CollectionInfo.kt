package annotations.collection


@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class CollectionInfo(
    val fieldName: String = "",
    val verboseName: String = "",
    val defaultValue: String = "",
    val nullable: Boolean = false,
    val limits: String = "",
    val readOnly: Boolean = false,
)