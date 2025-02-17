package annotations.references

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class OneToOneReferences(
    val tableName: String,
    val foreignKey: String
)