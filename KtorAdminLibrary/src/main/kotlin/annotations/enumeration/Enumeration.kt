package annotations.enumeration

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Enumeration(
    vararg val values: String
)