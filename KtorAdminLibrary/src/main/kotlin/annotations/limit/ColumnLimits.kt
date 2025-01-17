package annotations.limit

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ColumnLimits(
    val maxLength: Int = Int.MAX_VALUE,
    val minLength: Int = 0,
    val regexPattern: String = ""
)
