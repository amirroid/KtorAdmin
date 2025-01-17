package models


data class ColumnSet(
    val columnName: String,
    val type: ColumnType,
    val showInPanel: Boolean = true,
    val uploadTarget: UploadTarget? = null,
    val allowedMimeTypes: List<String>? = null,
    val defaultValue: String? = null,
    val enumerationValues: List<String>? = null,
    val limits: Limit? = null,
)
