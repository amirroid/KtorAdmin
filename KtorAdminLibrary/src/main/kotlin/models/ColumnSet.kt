package models

import configuration.DynamicConfiguration
import models.common.Reference
import models.date.AutoNowDate
import models.types.ColumnType
import utils.Constants
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Holds metadata and configuration details for a database column.
 *
 * This data class provides configuration options for managing how columns are displayed,
 * stored, and processed in the system.
 *
 * @property columnName The technical name of the column.
 * @property verboseName A user-friendly name for display in UI or reports.
 * @property type The data type of the column (e.g., numeric, string, etc.).
 * @property showInPanel Determines whether the column should be visible in the panel. Defaults to `true`.
 * @property nullable Indicates if the column allows null values. Defaults to `false`.
 * @property uploadTarget Defines upload-related configurations for this column, if applicable.
 * @property allowedMimeTypes A list of permitted MIME types for uploaded content, if relevant.
 * @property defaultValue The default value assigned to the column, if specified.
 * @property enumerationValues A list of possible values if the column represents an enumeration.
 * @property limits Specifies constraints such as minimum and maximum values.
 * @property reference A reference to another column or data entity, if applicable.
 * @property readOnly Marks the column as read-only, preventing modifications. Defaults to `false`.
 * @property computedColumn Defines a computed value or formula for the column, if applicable.
 * @property autoNowDate Configures automatic date assignment:
 * - `updateOnChange = true`: Updates the column to the current date when the entity changes.
 * - `updateOnChange = false`: Assigns the current date only when the entity is created.
 * @property statusColors A list of colors used for styling status-based values in the column,
 *                        typically applied when using enumerations.
 * @property hasRichEditor Determines whether this column should be displayed as a rich text editor in the admin panel.
 *                        Defaults to `false`.
 */
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
    val statusColors: List<String>? = null,
    val hasRichEditor: Boolean = false,
)


internal fun ColumnSet.getCurrentDate() = when (type) {
    ColumnType.DATE -> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        LocalDate.now(DynamicConfiguration.timeZone).format(formatter)
    }

    ColumnType.DATETIME -> {
        val formatter = DateTimeFormatter.ofPattern(Constants.LOCAL_DATETIME_FORMAT)
        LocalDateTime.now(DynamicConfiguration.timeZone).format(formatter)
    }

    else -> null
}


internal fun ColumnSet.getCurrentDateClass() = when (type) {
    ColumnType.DATE -> {
        LocalDate.now(DynamicConfiguration.timeZone)
    }

    ColumnType.DATETIME -> {
        LocalDateTime.now(DynamicConfiguration.timeZone)
    }

    else -> null
}