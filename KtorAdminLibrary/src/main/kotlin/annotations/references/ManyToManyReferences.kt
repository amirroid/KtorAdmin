package annotations.references

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ManyToManyReferences(
    val tableName: String,
    val targetColumn: String
)