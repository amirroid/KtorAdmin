package annotations.references

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ManyToOneReferences(
    val tableName: String,
    val foreignKey: String
)