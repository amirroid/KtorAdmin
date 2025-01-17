package annotations.enumeration

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class EnumerationColumn(
    vararg val values: String
)