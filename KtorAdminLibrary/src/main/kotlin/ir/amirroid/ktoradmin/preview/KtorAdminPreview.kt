package ir.amirroid.ktoradmin.preview

abstract class KtorAdminPreview : ExtraPreview() {
    abstract val key: String
    abstract fun createPreview(tableName: String, name: String, value: Any?): String?
}