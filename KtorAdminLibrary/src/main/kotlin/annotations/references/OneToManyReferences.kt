package annotations.references

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class OneToManyReferences(
    val tableName: String,
    val foreignKey: String
)