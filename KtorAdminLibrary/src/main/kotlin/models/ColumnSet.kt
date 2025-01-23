package models

import models.types.ColumnType

/**
 * Represents metadata and configuration details for a specific column.
 *
 * @property columnName The name of the column.
 * @property type The type of the column (e.g., numeric, string, etc.).
 * @property showInPanel Whether this column should be displayed in the panel. Defaults to `true`.
 * @property nullable Indicates if the column can have null values. Defaults to `false`.
 * @property uploadTarget Target information for uploading data related to this column.
 * @property allowedMimeTypes A list of MIME types allowed for this column's value, if applicable.
 * @property defaultValue The default value for the column, if specified.
 * @property enumerationValues A list of possible enumeration values for the column, if applicable.
 * @property limits Constraints or limits for the column's value (e.g., min/max values).
 * @property reference A reference to another column or data entity, if applicable.
 * @property readOnly Specifies if the column is read-only. Defaults to `false`.
 * @property computedColumn A computed value or formula for the column, if applicable.
 */
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
