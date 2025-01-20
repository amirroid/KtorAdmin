package models


data class ColumnSet(
    val columnName: String,
    val type: ColumnType,
    val showInPanel: Boolean = true,
    val nullable: Boolean = false,
    val uploadTarget: UploadTarget? = null,
    val allowedMimeTypes: List<String>? = null,
    val defaultValue: String? = null,
    val enumerationValues: List<String>? = null,
    val limits: Limit? = null,
    val reference: ColumnReference? = null,
    val readOnly: Boolean = false,
    val computedColumn: String? = null
)
