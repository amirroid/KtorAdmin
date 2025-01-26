package annotations.query

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class AdminQueries(
    val searches: Array<String> = [],
    val filters: Array<String> = [],
)