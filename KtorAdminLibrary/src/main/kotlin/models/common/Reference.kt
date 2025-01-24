package models.common

data class Reference(
    val tableName: String,
    val columnName: String,
)

fun Reference.toFormattedString(): String {
    return """
        |Reference(
        |       tableName = "$tableName",
        |       columnName = "$columnName",
        |   )
    """.trimMargin("|")
}