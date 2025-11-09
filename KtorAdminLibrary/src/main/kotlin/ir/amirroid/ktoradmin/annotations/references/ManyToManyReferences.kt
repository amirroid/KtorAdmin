package ir.amirroid.ktoradmin.annotations.references

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ManyToManyReferences(
    val tableName: String,
    val joinTable: String,
    val leftPrimaryKey: String,
    val rightPrimaryKey: String,
)