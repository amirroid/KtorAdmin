package models.field

import configuration.DynamicConfiguration
import models.ColumnSet
import models.Limit
import models.UploadTarget
import models.common.Reference
import models.date.AutoNowDate
import models.types.ColumnType
import models.types.FieldType
import utils.Constants
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
 * @property readOnly Specifies if the field is read-only. Defaults to `false`.
 * @property computedField A computed value or formula for the field, if applicable.
 * @property autoNowDate If provided, this property specifies behavior for automatically assigning the current date and time:
 * - `updateOnChange = true`: Updates the column to the current date whenever the entity is modified.
 * - `updateOnChange = false`: Only assigns the current date when the entity is created.
 * @property hasRichEditor Determines whether this column should be displayed as a rich text editor in the admin panel.
 *                        Defaults to `false`.
 * @property preview A key used in `KtorAdminPreview` to reference a preview of this column's content, if applicable.
 */
data class FieldSet(
    val fieldName: String?,
    val verboseName: String = fieldName.orEmpty(),
    val type: FieldType,
    val showInPanel: Boolean = true,
    val nullable: Boolean = false,
    val uploadTarget: UploadTarget? = null,
    val allowedMimeTypes: List<String>? = null,
    val defaultValue: String? = null,
    val enumerationValues: List<String>? = null,
    val limits: Limit? = null,
    val readOnly: Boolean = false,
    val computedField: String? = null,
    val autoNowDate: AutoNowDate? = null,
    val hasRichEditor: Boolean = false,
    val preview: String? = null
)


internal fun FieldSet.getCurrentDate() = when (type) {
    FieldType.Date -> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        LocalDate.now(DynamicConfiguration.timeZone).format(formatter)
    }

    FieldType.DateTime -> {
        val formatter = DateTimeFormatter.ofPattern(Constants.LOCAL_DATETIME_FORMAT)
        LocalDateTime.now(DynamicConfiguration.timeZone).format(formatter)
    }


    FieldType.Instant -> {
        val formatter = DateTimeFormatter.ofPattern(Constants.LOCAL_DATETIME_FORMAT)
        LocalDateTime.now(DynamicConfiguration.timeZone).format(formatter)
    }

    else -> null
}