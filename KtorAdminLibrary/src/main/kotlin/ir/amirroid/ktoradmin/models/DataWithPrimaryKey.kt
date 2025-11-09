package ir.amirroid.ktoradmin.models

internal data class DataWithPrimaryKey(
    val primaryKey: String,
    val data: List<Any?>
)
