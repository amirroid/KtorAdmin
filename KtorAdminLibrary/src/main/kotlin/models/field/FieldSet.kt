package models.field

import models.Limit
import models.UploadTarget
import models.common.Reference
import models.types.FieldType

/**
 * Represents metadata and configuration details for a specific field.
 *
 * @property fieldName The name of the field.
 * @property type The type of the field (e.g., numeric, string, etc.).
 * @property showInPanel Whether this field should be displayed in the panel. Defaults to `true`.
 * @property nullable Indicates if the field can have null values. Defaults to `false`.
 * @property uploadTarget Target information for uploading data related to this field.
 * @property allowedMimeTypes A list of MIME types allowed for this field's value, if applicable.
 * @property defaultValue The default value for the field, if specified.
 * @property enumerationValues A list of possible enumeration values for the field, if applicable.
 * @property limits Constraints or limits for the field's value (e.g., min/max values).
 * @property reference A reference to another field or data entity, if applicable.
 * @property readOnly Specifies if the field is read-only. Defaults to `false`.
 * @property computedField A computed value or formula for the field, if applicable.
 */
data class FieldSet(
    val fieldName: String?,
    val type: FieldType,
    val showInPanel: Boolean = true,
    val nullable: Boolean = false,
    val uploadTarget: UploadTarget? = null,
    val allowedMimeTypes: List<String>? = null,
    val defaultValue: String? = null,
    val enumerationValues: List<String>? = null,
    val limits: Limit? = null,
    val reference: Reference? = null,
    val readOnly: Boolean = false,
    val computedField: String? = null
) {
    companion object {
        val Empty = FieldSet("", FieldType.NotAvailable)
    }
}