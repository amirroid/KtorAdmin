package repository

import annotations.computed.Computed
import annotations.confirmation.Confirmation
import annotations.date.AutoNowDate
import annotations.enumeration.Enumeration
import annotations.field.FieldInfo
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.references.ManyToManyReferences
import annotations.references.ManyToOneReferences
import annotations.references.OneToManyReferences
import annotations.references.OneToOneReferences
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
import processors.hibernate.HibernateTableProcessor
import processors.qualifiedName
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
     * Data class representing the basic column information extracted from annotations.
     * Acts as a container for common column properties to reduce duplication.
     */
    data class BaseColumnInfo(
        val name: String,
        val columnName: String,
        val verboseName: String,
        val nullable: Boolean
    )

    /**
     * Extracts basic column information from property annotations.
     * Handles both native (Hibernate/Exposed) and custom ColumnInfo annotations.
     *
     * @param property The property declaration to process
     * @param type The KSType information for the property
     * @param nativeColumnName Optional native column name (e.g., from Hibernate @Column)
     * @param nativeNullable Optional native nullable setting
     * @return BaseColumnInfo containing processed column information
     */
    private fun extractBaseColumnInfo(
        property: KSPropertyDeclaration,
        type: KSType,
        nativeColumnName: String? = null,
        nativeNullable: Boolean? = null
    ): BaseColumnInfo {
        val name = property.simpleName.asString()
        val infoAnnotation = property.annotations.find { it.shortName.asString() == ColumnInfo::class.simpleName }

        val infoName = infoAnnotation?.findArgument<String>("columnName")?.takeIf { it.isNotEmpty() }
        val columnName = nativeColumnName ?: infoName ?: name

        val infoNullable =
            infoAnnotation?.findArgument<String>("nullable")?.takeIf { it.isNotEmpty() }?.let { it == "true" }
        val nullable = nativeNullable ?: infoNullable ?: type.isMarkedNullable

        val verboseName = infoAnnotation?.findArgument<String>("verboseName")?.takeIf { it.isNotEmpty() } ?: columnName

        return BaseColumnInfo(name, columnName, verboseName, nullable)
    }

    /**
     * Core processing function for creating ColumnSet configurations.
     * Handles all common logic between Hibernate and Exposed processing.
     *
     * @param property The property declaration being processed
     * @param baseInfo Basic column information from extractBaseColumnInfo
     * @param genericArgument The resolved generic type argument
     * @param enumValues List of enumeration values if applicable
     * @param isNativeEnumerated Whether this is a native enumerated type (Hibernate)
     * @return Processed ColumnSet configuration
     */
    private fun processColumnSet(
        property: KSPropertyDeclaration,
        baseInfo: BaseColumnInfo,
        genericArgument: String,
        enumValues: List<String>?,
        isNativeEnumerated: Boolean = false
    ): ColumnSet {
        // Check for special column types
        val hasUploadAnnotation = UploadUtils.hasUploadAnnotation(property.annotations)
        val hasEnumerationColumnAnnotation = hasEnumerationColumnAnnotation(property.annotations)
        val infoAnnotation = property.annotations.find { it.shortName.asString() == ColumnInfo::class.simpleName }

        // Validate upload configuration if present
        if (hasUploadAnnotation) {
            UploadUtils.validatePropertyType(genericArgument, baseInfo.columnName)
        }

        // Determine the column type based on annotations and property type
        val columnType = when {
            hasEnumerationColumnAnnotation || isNativeEnumerated -> ColumnType.ENUMERATION
            hasUploadAnnotation -> ColumnType.FILE
            else -> guessPropertyType(genericArgument)
        }

        // Process and validate status styles
        val statusColors = property.annotations.getStatusStyles()
        validateStatusColors(columnType, statusColors, enumValues, baseInfo.name)

        // Process additional column properties
        val computedColumnInfo = property.annotations.getComputed()
        val isReadOnly = (infoAnnotation?.findArgument<Boolean>("readOnly") == true) ||
                (computedColumnInfo?.second == true)

        val autoNowDate = getAutoNowDateAnnotation(property.annotations)
        validateAutoNowDate(columnType, autoNowDate, baseInfo.columnName)

        val hasRichEditor = hasRichEditorAnnotation(property.annotations)
        validateRichEditor(hasRichEditor, columnType)

        val reference = property.annotations.getReferences()

        // Construct and return the final ColumnSet configuration
        return ColumnSet(
            columnName = baseInfo.columnName,
            type = columnType,
            verboseName = baseInfo.verboseName,
            nullable = baseInfo.nullable,
            blank = infoAnnotation?.findArgument<Boolean>("blank") != false,
            unique = infoAnnotation?.findArgument<Boolean>("unique") == true || reference is Reference.OneToOne,
            showInPanel = !hasIgnoreColumnAnnotation(property.annotations),
            uploadTarget = UploadUtils.getUploadTargetFromAnnotation(property.annotations),
            allowedMimeTypes = if (hasUploadAnnotation)
                UploadUtils.getAllowedMimeTypesFromAnnotation(property.annotations)
            else null,
            defaultValue = infoAnnotation?.findArgument<String>("defaultValue")?.takeIf { it.isNotEmpty() },
            enumerationValues = enumValues,
            limits = property.annotations.getLimits(),
            reference = reference,
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
     * Processes column configuration for Exposed ORM.
     * Handles specific Exposed-related logic while delegating common processing.
     *
     * @param property The property declaration to process
     * @param type The KSType information for the property
     * @return ColumnSet configuration or null if processing fails
     */
    fun getColumnSetsForExposed(property: KSPropertyDeclaration, type: KSType): ColumnSet? {
        val genericArgument = type.arguments.firstOrNull()?.type?.resolve()?.toClassName()?.canonicalName ?: return null
        val baseInfo = extractBaseColumnInfo(property, type)
        val enumValues = property.annotations.getEnumerations()

        return processColumnSet(
            property = property,
            baseInfo = baseInfo,
            genericArgument = genericArgument,
            enumValues = enumValues
        )
    }

    /**
     * Processes column configuration for Hibernate ORM.
     * Handles Hibernate-specific annotations and enumeration processing.
     *
     * @param property The property declaration to process
     * @param type The KSType information for the property
     * @return ColumnSet configuration or null if processing fails
     */
    fun getColumnSetsForHibernate(property: KSPropertyDeclaration, type: KSType): ColumnSet? {
        val genericArgument = type.declaration.qualifiedName?.asString() ?: return null

        // Check for native Hibernate enumeration
        val isNativeEnumerated = type.declaration is KSClassDeclaration &&
                (type.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS &&
                property.annotations.any {
                    it.qualifiedName in HibernateTableProcessor.getListOfHibernatePackage("Enumerated")
                }

        // Process Hibernate-specific column annotation
        val columnAnnotation = property.annotations.find {
            it.qualifiedName in HibernateTableProcessor.getListOfHibernatePackage("Column")
        }

        val nativeName = columnAnnotation?.findArgument<String>("name")?.takeIf { it.isNotEmpty() }
        val nativeNullable =
            columnAnnotation?.findArgument<String>("nullable")?.takeIf { it.isNotEmpty() }?.let { it == "true" }

        val baseInfo = extractBaseColumnInfo(property, type, nativeName, nativeNullable)

        // Handle native enumeration values
        val nativeEnumeratedValues = if (isNativeEnumerated) {
            (type.declaration as KSDeclarationContainer).declarations
                .filterIsInstance<KSClassDeclaration>()
                .map { it.simpleName.asString() }
                .toList()
        } else null

        val enumValues = property.annotations.getEnumerations() ?: nativeEnumeratedValues

        return processColumnSet(
            property = property,
            baseInfo = baseInfo,
            genericArgument = genericArgument,
            enumValues = enumValues,
            isNativeEnumerated = isNativeEnumerated,
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

    private val OneToOneReferencesQualifiedName = OneToOneReferences::class.qualifiedName
    private val ManyToOneReferencesQualifiedName = ManyToOneReferences::class.qualifiedName
    private val OneToManyReferencesQualifiedName = OneToManyReferences::class.qualifiedName
    private val ManyToManyReferencesQualifiedName = ManyToManyReferences::class.qualifiedName

    private fun Sequence<KSAnnotation>.getReferences(): Reference? =
        find {
            it.qualifiedName.orEmpty() in listOf(
                OneToOneReferencesQualifiedName,
                ManyToManyReferencesQualifiedName,
                ManyToOneReferencesQualifiedName,
                OneToManyReferencesQualifiedName
            )
        }?.let {
            when (it.qualifiedName) {
                OneToOneReferencesQualifiedName -> {
                    Reference.OneToOne(
                        relatedTable = it.arguments.firstOrNull { arg -> arg.name?.asString() == "tableName" }!!.value as String,
                        foreignKey = it.arguments.firstOrNull { arg -> arg.name?.asString() == "foreignKey" }!!.value as String
                    )
                }

                OneToManyReferencesQualifiedName -> {
                    Reference.OneToMany(
                        relatedTable = it.arguments.firstOrNull { arg -> arg.name?.asString() == "tableName" }!!.value as String,
                        foreignKey = it.arguments.firstOrNull { arg -> arg.name?.asString() == "foreignKey" }!!.value as String
                    )
                }

                ManyToOneReferencesQualifiedName -> {
                    Reference.ManyToOne(
                        relatedTable = it.arguments.firstOrNull { arg -> arg.name?.asString() == "tableName" }!!.value as String,
                        foreignKey = it.arguments.firstOrNull { arg -> arg.name?.asString() == "foreignKey" }!!.value as String
                    )
                }

                ManyToManyReferencesQualifiedName -> {
                    Reference.ManyToMany(
                        relatedTable = it.arguments.firstOrNull { arg -> arg.name?.asString() == "tableName" }!!.value as String,
                        joinTable = it.arguments.firstOrNull { arg -> arg.name?.asString() == "joinTable" }!!.value as String,
                        leftPrimaryKey = it.arguments.firstOrNull { arg -> arg.name?.asString() == "leftPrimaryKey" }!!.value as String,
                        rightPrimaryKey = it.arguments.firstOrNull { arg -> arg.name?.asString() == "rightPrimaryKey" }!!.value as String,
                    )
                }

                else -> null
            }
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