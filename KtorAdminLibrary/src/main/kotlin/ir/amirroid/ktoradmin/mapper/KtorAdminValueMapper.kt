package ir.amirroid.ktoradmin.mapper

interface KtorAdminValueMapper {
    fun map(value: Any?): Any?
    fun restore(value: Any?): Any?
    val key: String
}