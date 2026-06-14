package ir.amirroid.ktoradmin.annotations.autocomplete

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoComplete(
    val searchFields: Array<String> = [],
)
