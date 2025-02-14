package repository

import annotations.computed.Computed
import annotations.confirmation.Confirmation
import annotations.date.AutoNowDate
import annotations.enumeration.Enumeration
import annotations.field.FieldInfo
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.references.References
import annotations.rich_editor.RichEditor
import annotations.status.StatusStyle
import annotations.value_mapper.ValueMapper
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.toClassName
import models.*
import models.common.Reference
import models.field.FieldSet
import models.types.ColumnType
import models.types.FieldType
import utils.UploadUtils
import utils.findArgument
import utils.guessPropertyType

/**
 * Repository responsible for processing Kotlin Symbol Processing (KSP) property declarations
 * and converting them into structured column and field definitions.
 * This object handles the extraction and validation of various annotations used to define
 * database columns and form fields.
 */
object PropertiesRepository {
    /**
     * Processes a property declaration to create a ColumnSet configuration.
     * Extracts and validates various annotations to determine column properties including:
     * - Basic column information (name, type, nullability)
     * - Upload configurations for file columns
     * - Enumeration values and status styles
     * - Computed column properties
     * - Rich text editor settings
     * - Date/time automatic update behaviors
     *
     * @param property The KSP property declaration to process
     * @param type The KSP type information for the property
     * @return ColumnSet configuration or null if the property cannot be processed
     * @throws IllegalArgumentException if annotation combinations are invalid
     */
    fun getColumnSets(property: KSPropertyDeclaration, type: KSType): ColumnSet? {
        val hasUploadAnnotation = UploadUtils.hasUploadAnnotation(property.annotations)
        val hasEnumerationColumnAnnotation = hasEnumerationColumnAnnotation(property.annotations)
        val genericArgument = type.arguments.firstOrNull()?.type?.resolve()?.toClassName()?.canonicalName ?: return null

        // Extract basic column information
        val name = property.simpleName.asString()
        val infoAnnotation = property.annotations.find { it.shortName.asString() == ColumnInfo::class.simpleName }
        val columnName = infoAnnotation?.findArgument<String>("columnName")?.takeIf { it.isNotEmpty() } ?: name
        val verboseName = infoAnnotation?.findArgument<String>("verboseName")?.takeIf { it.isNotEmpty() } ?: columnName

        // Validate and process enumerations
        val enumValues = property.annotations.getEnumerations()
        if (hasUploadAnnotation) {
            UploadUtils.validatePropertyType(genericArgument, columnName)
        }

        // Determine column type
        val columnType = when {
            hasEnumerationColumnAnnotation -> ColumnType.ENUMERATION
            hasUploadAnnotation -> ColumnType.FILE
            else -> guessPropertyType(genericArgument)
        }

        // Process status styles
        val statusColors = property.annotations.getStatusStyles()
        validateStatusColors(columnType, statusColors, enumValues, name)

        // Process additional properties
        val computedColumnInfo = property.annotations.getComputed()
        val isReadOnly = (infoAnnotation?.findArgument<Boolean>("readOnly") ?: false) ||
                (computedColumnInfo?.second ?: false)

        val autoNowDate = getAutoNowDateAnnotation(property.annotations)
        validateAutoNowDate(columnType, autoNowDate, columnName)

        val hasRichEditor = hasRichEditorAnnotation(property.annotations)
        validateRichEditor(hasRichEditor, columnType)

        return ColumnSet(
            columnName = columnName,
            type = columnType,
            verboseName = verboseName,
            nullable = infoAnnotation?.findArgument<Boolean>("nullable") == true,
            blank = infoAnnotation?.findArgument<Boolean>("blank") != false,
            unique = infoAnnotation?.findArgument<Boolean>("unique") == true,
            showInPanel = !hasIgnoreColumnAnnotation(property.annotations),
            uploadTarget = UploadUtils.getUploadTargetFromAnnotation(property.annotations),
            allowedMimeTypes = if (hasUploadAnnotation)
                UploadUtils.getAllowedMimeTypesFromAnnotation(property.annotations)
            else null,
            defaultValue = infoAnnotation?.findArgument<String>("defaultValue")?.takeIf { it.isNotEmpty() },
            enumerationValues = enumValues,
            limits = property.annotations.getLimits(),
            reference = property.annotations.getReferences(),
            readOnly = isReadOnly,
            computedColumn = computedColumnInfo?.first,
            autoNowDate = autoNowDate,
            statusColors = statusColors,
            hasRichEditor = hasRichEditor,
            hasConfirmation = hasConfirmationAnnotation(property.annotations),
            valueMapper = getValueMapperAnnotation(property.annotations),
        )
    }

    /**
     * Processes a property declaration to create a FieldSet configuration.
     * Similar to getColumnSets but specifically for form field configurations.
     * Handles validation and processing of field-specific annotations and properties.
     *
     * @param property The KSP property declaration to process
     * @param type The field type to process
     * @return FieldSet configuration or null if the property cannot be processed
     * @throws IllegalArgumentException if annotation combinations are invalid
     */
    fun getFieldSet(property: KSPropertyDeclaration, type: FieldType): FieldSet? {
        val hasUploadAnnotation = UploadUtils.hasUploadAnnotation(property.annotations)
        val hasEnumerationColumnAnnotation = hasEnumerationColumnAnnotation(property.annotations)

        // Extract basic field information
        val name = property.simpleName.asString()
        val infoAnnotation = property.annotations.find { it.shortName.asString() == FieldInfo::class.simpleName }
        val fieldName = infoAnnotation?.findArgument<String>("fieldName")?.takeIf { it.isNotEmpty() } ?: name
        val verboseName = infoAnnotation?.findArgument<String>("verboseName")?.takeIf { it.isNotEmpty() }
            ?: fieldName

        if (hasUploadAnnotation) {
            UploadUtils.validatePropertyType(fieldName, type)
        }

        // Determine field type
        val fieldType = when {
            hasEnumerationColumnAnnotation -> FieldType.Enumeration
            hasUploadAnnotation -> FieldType.File
            else -> type
        }

        // Process computed field information
        val computedFieldInfo = property.annotations.getComputed()
        val isReadOnly = (infoAnnotation?.findArgument<Boolean>("readOnly") ?: false) ||
                (computedFieldInfo?.second ?: false)

        // Validate date-related configurations
        val autoNowDate = getAutoNowDateAnnotation(property.annotations)
        validateFieldAutoNowDate(fieldType, autoNowDate, fieldName)

        return FieldSet(
            fieldName = fieldName,
            verboseName = verboseName,
            type = fieldType,
            nullable = infoAnnotation?.findArgument<Boolean>("nullable") ?: false,
            showInPanel = !hasIgnoreColumnAnnotation(property.annotations),
            uploadTarget = UploadUtils.getUploadTargetFromAnnotation(property.annotations),
            allowedMimeTypes = if (hasUploadAnnotation)
                UploadUtils.getAllowedMimeTypesFromAnnotation(property.annotations)
            else null,
            defaultValue = infoAnnotation?.findArgument<String>("defaultValue")?.takeIf { it.isNotEmpty() },
            enumerationValues = property.annotations.getEnumerations(),
            limits = property.annotations.getLimits(),
            readOnly = isReadOnly,
            computedField = computedFieldInfo?.first,
            autoNowDate = autoNowDate,
        )
    }

    /**
     * Checks if a property has the Enumeration annotation.
     */
    private fun hasEnumerationColumnAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.shortName.asString() == Enumeration::class.simpleName }

    /**
     * Checks if a property has the RichEditor annotation.
     */
    private fun hasRichEditorAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.shortName.asString() == RichEditor::class.simpleName }

    /**
     * Checks if a property has the Confirmation annotation.
     */
    private fun hasConfirmationAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.shortName.asString() == Confirmation::class.simpleName }

    /**
     * Extracts AutoNowDate annotation configuration if present.
     */
    private fun getAutoNowDateAnnotation(annotations: Sequence<KSAnnotation>) =
        annotations.firstOrNull { it.shortName.asString() == AutoNowDate::class.simpleName }
            ?.let {
                models.date.AutoNowDate(
                    updateOnChange = it.arguments.getArgument<Boolean>("updateOnChange") == true
                )
            }

    /**
     * Extracts ValueMapper annotation configuration if present.
     */
    private fun getValueMapperAnnotation(annotations: Sequence<KSAnnotation>) =
        annotations.firstOrNull { it.shortName.asString() == ValueMapper::class.simpleName }?.arguments?.getArgument<String>(
            "key"
        )

    /**
     * Checks if a property should be ignored in panels.
     */
    private fun hasIgnoreColumnAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.shortName.asString() == IgnoreColumn::class.simpleName }

    /**
     * Extracts enumeration values from annotations.
     */
    private fun Sequence<KSAnnotation>.getEnumerations(): List<String>? =
        find { it.shortName.asString() == Enumeration::class.simpleName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "values" }
            ?.value
            ?.let { it as? List<*> }
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }

    /**
     * Extracts status style colors from annotations.
     */
    private fun Sequence<KSAnnotation>.getStatusStyles(): List<String>? =
        find { it.shortName.asString() == StatusStyle::class.simpleName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "color" }
            ?.value
            ?.let { it as? List<*> }
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }

    /**
     * Extracts reference configuration from annotations.
     */
    private fun Sequence<KSAnnotation>.getReferences(): Reference? =
        find { it.shortName.asString() == References::class.simpleName }
            ?.arguments
            ?.let {
                Reference(
                    tableName = it.firstOrNull { arg -> arg.name?.asString() == "tableName" }!!.value as String,
                    columnName = it.firstOrNull { arg -> arg.name?.asString() == "targetColumn" }!!.value as String
                )
            }

    /**
     * Extracts and processes limit configurations from annotations.
     */
    private fun Sequence<KSAnnotation>.getLimits(): Limit? =
        find { it.shortName.asString() == Limits::class.simpleName }?.arguments?.let { args ->
            Limit(
                maxLength = args.getArgument("maxLength") ?: Int.MAX_VALUE,
                minLength = args.getArgument("minLength") ?: Int.MIN_VALUE,
                regexPattern = args.getArgument<String?>("regexPattern")?.takeIf { it.isNotEmpty() },
                maxCount = args.getArgument("maxCount") ?: Double.MAX_VALUE,
                minCount = args.getArgument("minCount") ?: Double.MIN_VALUE,
                maxBytes = args.getArgument("maxBytes") ?: Long.MAX_VALUE,
                minDateRelativeToNow = args.getArgument("minDateRelativeToNow") ?: Long.MAX_VALUE,
                maxDateRelativeToNow = args.getArgument("maxDateRelativeToNow") ?: Long.MAX_VALUE,
                allowedMimeTypes = args
                    .firstOrNull { it.name?.asString() == "allowedMimeTypes" }
                    ?.value
                    ?.let { it as? List<*> }
                    ?.filterIsInstance<String>()
                    ?.takeIf { it.isNotEmpty() }
            )
        }

    /**
     * Extracts computed column/field configuration from annotations.
     */
    private fun Sequence<KSAnnotation>.getComputed(): Pair<String, Boolean>? =
        find { it.shortName.asString() == Computed::class.simpleName }
            ?.arguments?.let {
                it.getArgument<String>("compute")!! to it.getArgument<Boolean>("readOnly")!!
            }

    /**
     * Helper function to extract typed arguments from KSValueArgument list.
     */
    private inline fun <reified D> List<KSValueArgument>.getArgument(name: String): D? =
        firstOrNull { it.name?.asString() == name }?.value as? D

    /**
     * Validates status color configuration.
     */
    private fun validateStatusColors(
        columnType: ColumnType,
        statusColors: List<String>?,
        enumValues: List<String>?,
        name: String
    ) {
        if (columnType != ColumnType.ENUMERATION && statusColors != null) {
            throw IllegalArgumentException("StatusStyle can only be used with ENUMERATION column types.")
        }

        val hexColorPattern = "^#([A-Fa-f0-9]{6})$".toRegex()
        if ((statusColors != null && enumValues != null) &&
            (statusColors.count() != enumValues.count() ||
                    statusColors.any { !hexColorPattern.matches(it) })
        ) {
            throw IllegalArgumentException(
                "($name) Invalid status colors: The number of status colors must match the number of " +
                        "enumeration values, and all colors must be valid hex codes (excluding #000000)."
            )
        }
    }

    /**
     * Validates auto-now-date configuration for columns.
     */
    private fun validateAutoNowDate(columnType: ColumnType, autoNowDate: models.date.AutoNowDate?, columnName: String) {
        if (columnType !in listOf(ColumnType.DATE, ColumnType.DATETIME) && autoNowDate != null) {
            throw IllegalArgumentException(
                "The 'autoNowDate' property can only be used with columns of type 'DATE' or 'DATETIME'. " +
                        "Column '$columnName' has type '$columnType', which is incompatible."
            )
        }
    }

    /**
     * Validates rich editor configuration.
     */
    private fun validateRichEditor(hasRichEditor: Boolean, columnType: ColumnType) {
        if (hasRichEditor && columnType != ColumnType.STRING) {
            throw IllegalArgumentException("Rich editor can only be used with columns of type STRING.")
        }
    }

    /**
     * Validates auto-now-date configuration for fields.
     */
    private fun validateFieldAutoNowDate(
        fieldType: FieldType,
        autoNowDate: models.date.AutoNowDate?,
        fieldName: String
    ) {
        if (fieldType !in listOf(FieldType.Date, FieldType.DateTime, FieldType.Instant) && autoNowDate != null) {
            throw IllegalArgumentException(
                "The 'autoNowDate' property can only be used with fields of type 'DATE' or 'DATETIME'. " +
                        "Field '$fieldName' has type '$fieldType', which is incompatible."
            )
        }
    }
}