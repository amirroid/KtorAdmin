package annotations.order

/**
 * Annotation that specifies the default ordering for a class.
 *
 * This annotation can be applied to a class to define the default field and its order (ascending or descending)
 * when fetching or querying data related to the class.
 *
 * @property name The name of the field in MongoDB to order by. For SQL, it corresponds to the column name.
 * @property direction The direction of the order: "ASC" for ascending or "DESC" for descending.
 *
 * Usage:
 * ```
 * @DefaultOrder(name = "createdAt", direction = "ASC")
 * class SomeEntity
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DefaultOrder(
    val name: String,
    val direction: String = "ASC"
)