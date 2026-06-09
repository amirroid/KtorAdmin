package ir.amirroid.ktoradmin.annotations.defaultvalue

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class DefaultValueProvider(
    val key: String,
)
