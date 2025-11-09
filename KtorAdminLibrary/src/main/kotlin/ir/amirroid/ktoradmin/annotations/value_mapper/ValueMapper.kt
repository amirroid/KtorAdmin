package ir.amirroid.ktoradmin.annotations.value_mapper

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ValueMapper(val key: String)
