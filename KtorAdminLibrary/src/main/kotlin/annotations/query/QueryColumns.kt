package annotations.query

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class QueryColumns(
    val searches: Array<String> = [],
    val filters: Array<String> = [],
)