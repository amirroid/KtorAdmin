package models

data class ColumnSet(
    val columnName: String,
    val type: ColumnType,
    val showInPanel: Boolean = true,
)
