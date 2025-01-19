package models

data class ColumnReference(
    val tableName: String,
    val columnName: String,
)

fun ColumnReference.toFormattedString(): String {
    return """
        |ColumnReference(
        |       tableName = "$tableName",
        |       columnName = "$columnName",
        |   )
    """.trimMargin("|")
}