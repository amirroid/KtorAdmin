package models

import configuration.DynamicConfiguration
import models.common.Reference
import models.date.AutoNowDate
import models.types.ColumnType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Represents metadata and configuration details for a specific column.
 *
 * @property columnName The name of the column.
 * @property verboseName A human-readable name for the column, often used in UI or reports.
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
 * @property autoNowDate If provided, this property specifies behavior for automatically assigning the current date and time:
 * - `updateOnChange = true`: Updates the column to the current date whenever the entity is modified.
 * - `updateOnChange = false`: Only assigns the current date when the entity is created.
 * */
data class ColumnSet(
    val columnName: String,
    val verboseName: String,
    val type: ColumnType,
    val showInPanel: Boolean = true,
    val nullable: Boolean = false,
    val uploadTarget: UploadTarget? = null,
    val allowedMimeTypes: List<String>? = null,
    val defaultValue: String? = null,
    val enumerationValues: List<String>? = null,
    val limits: Limit? = null,
    val reference: Reference? = null,
    val readOnly: Boolean = false,
    val computedColumn: String? = null,
    val autoNowDate: AutoNowDate? = null,
)


internal fun ColumnSet.getCurrentDate() = when (type) {
    ColumnType.DATE -> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        LocalDate.now(DynamicConfiguration.timeZone).format(formatter)
    }

    ColumnType.DATETIME -> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        LocalDateTime.now(DynamicConfiguration.timeZone).format(formatter)
    }

    else -> null
}