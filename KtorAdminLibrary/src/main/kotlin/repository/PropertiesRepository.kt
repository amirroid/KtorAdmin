package repository

import annotations.computed.Computed
import annotations.confirmation.Confirmation
import annotations.date.AutoNowDate
import annotations.enumeration.Enumeration
import annotations.field.FieldInfo
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.preview.Preview
import annotations.references.ManyToManyReferences
import annotations.references.ManyToOneReferences
import annotations.references.OneToOneReferences
import annotations.rich_editor.RichEditor
import annotations.status.StatusStyle
import annotations.text_area.TextAreaField
import annotations.type.OverrideColumnType
import annotations.value_mapper.ValueMapper
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.toClassName
import models.*
import models.common.Reference
import models.field.FieldSet
import models.types.ColumnType
import models.types.FieldType
import processors.hibernate.HibernateTableProcessor.Companion.getListOfHibernatePackage
import processors.qualifiedName
import repository.PropertiesRepository.hasConfirmationAnnotation
import utils.UploadUtils
import utils.findArgument
import utils.guessPropertyType
import utils.toTableName

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
        nativeNullable: Boolean? = null,
        hibernateReference: HibernateReferenceData? = null
    ): BaseColumnInfo {
        val name = property.simpleName.asString()
        val infoAnnotation = property.annotations.find { it.shortName.asString() == ColumnInfo::class.simpleName }

        val infoName = infoAnnotation?.findArgument<String>("columnName")?.takeIf { it.isNotEmpty() }
        val columnName = nativeColumnName ?: infoName ?: hibernateReference?.columnName ?: name

        val infoNullable = infoAnnotation?.findArgument<Boolean>("nullable")
        val nullable = nativeNullable ?: infoNullable ?: type.isMarkedNullable

        val defaultVerboseName = columnName.split("_")
            .mapIndexed { index, item -> if (index == 0) item.replaceFirstChar { it.uppercase() } else item }
            .joinToString(" ")
        val verboseName =
            infoAnnotation?.findArgument<String>("verboseName")?.takeIf { it.isNotEmpty() } ?: defaultVerboseName

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
        genericArgument: String?,
        enumValues: List<String>?,
        isNativeEnumerated: Boolean = false,
        referenceData: HibernateReferenceData? = null
    ): ColumnSet {
        // Check for special column types
        val hasUploadAnnotation = UploadUtils.hasUploadAnnotation(property.annotations)
        val hasEnumerationColumnAnnotation = hasEnumerationColumnAnnotation(property.annotations)
        val infoAnnotation = property.annotations.find { it.shortName.asString() == ColumnInfo::class.simpleName }

        // Validate upload configuration if present
        if (hasUploadAnnotation && genericArgument != null) {
            UploadUtils.validatePropertyType(genericArgument, baseInfo.columnName)
        }

        val overrideType = property.annotations.getOverrideType()

        // Determine the column type based on annotations and property type
        val columnType = when {
            hasEnumerationColumnAnnotation || isNativeEnumerated -> ColumnType.ENUMERATION
            hasUploadAnnotation -> ColumnType.FILE
            overrideType != null -> overrideType
            genericArgument == null -> ColumnType.NOT_AVAILABLE
            referenceData != null -> referenceData.type
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

        val reference = property.annotations.getReferences() ?: referenceData?.reference

        val hasTextAreaField = hasTextAreaFieldAnnotation(property.annotations)
        validateTextArea(hasTextAreaField, columnType)

        // Construct and return the final ColumnSet configuration
        return ColumnSet(
            columnName = baseInfo.columnName,
            type = columnType,
            verboseName = baseInfo.verboseName,
            nullable = referenceData?.nullable ?: baseInfo.nullable,
            blank = infoAnnotation?.findArgument<Boolean>("blank") != false,
            unique = referenceData?.unique
                ?: (infoAnnotation?.findArgument<Boolean>("unique") == true || reference is Reference.OneToOne),
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
            preview = getPreviewAnnotation(property.annotations),
            hasTextArea = hasTextAreaField,
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
    fun getColumnSetsForExposed(property: KSPropertyDeclaration, type: KSType, isEmpty: Boolean = false): ColumnSet? {
        val genericArgument = if (isEmpty) {
            null
        } else type.arguments.firstOrNull()?.type?.resolve()?.toClassName()?.canonicalName ?: return null
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
                    it.qualifiedName in getListOfHibernatePackage("Enumerated")
                }

        // Process Hibernate-specific column annotation
        val columnAnnotation = property.annotations.find {
            it.qualifiedName in getListOfHibernatePackage("Column")
        }

        val hibernateReference = detectReferenceAnnotationForHibernateTable(property, type)

        val nativeName = columnAnnotation?.findArgument<String>("name")?.takeIf { it.isNotEmpty() }
        val nativeNullable =
            columnAnnotation?.findArgument<String>("nullable")?.takeIf { it.isNotEmpty() }?.let { it == "true" }

        val baseInfo = extractBaseColumnInfo(property, type, nativeName, nativeNullable, hibernateReference)

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
            referenceData = hibernateReference,
        )
    }

    data class HibernateReferenceData(
        val type: ColumnType,
        val reference: Reference,
        val columnName: String,
        val nullable: Boolean? = null,
        val unique: Boolean? = null,
    )

    /**
     * Detects and extracts Hibernate reference annotations (@OneToOne, @ManyToOne) from a property
     * and builds corresponding reference data.
     *
     * @param property The property declaration to analyze
     * @param type The KSType of the property
     * @return HibernateReferenceData if a valid reference annotation is found, null otherwise
     */
    private fun detectReferenceAnnotationForHibernateTable(
        property: KSPropertyDeclaration,
        type: KSType
    ): HibernateReferenceData? {
        // Find relevant Hibernate annotations on the property
        val annotations = property.annotations
        val oneToOneReference = annotations.findReferenceAnnotation("OneToOne")
        val manyToOneReference = annotations.findReferenceAnnotation("ManyToOne")
        val joinColumn = annotations.findReferenceAnnotation("JoinColumn")

        // Return early if no JoinColumn annotation is present
        if (joinColumn == null) return null

        // Extract column name from JoinColumn annotation or use property name as fallback
        val columnName = joinColumn.arguments.getArgument<String>("name")
            ?.takeIf { it.isNotEmpty() }
            ?: property.simpleName.asString()


        val referenceColumnName =
            joinColumn.arguments.getArgument<String>("referencedColumnName")?.takeIf { it.isNotEmpty() }

        // Get referenced table information
        val tableNameWithPrimaryKey = (type.declaration as? KSClassDeclaration)?.let {
            getTableNameWithPrimaryKey(it, referenceColumnName)
        } ?: return null

        // Extract nullable and unique constraints from JoinColumn
        val nullable = joinColumn.findArgument<Boolean>("nullable")
        val unique = joinColumn.findArgument<Boolean>("unique")

        // Create appropriate reference data based on annotation type
        return when {
            oneToOneReference != null -> createReferenceData(
                tableNameWithPrimaryKey,
                Reference.OneToOne(
                    tableNameWithPrimaryKey.first,
                    tableNameWithPrimaryKey.second
                ),
                columnName,
                unique,
                nullable
            )

            manyToOneReference != null -> createReferenceData(
                tableNameWithPrimaryKey,
                Reference.ManyToOne(
                    tableNameWithPrimaryKey.first,
                    tableNameWithPrimaryKey.second
                ),
                columnName,
                unique,
                nullable
            )

            else -> null
        }
    }

    /**
     * Helper function to find a specific Hibernate annotation
     */
    private fun Sequence<KSAnnotation>.findReferenceAnnotation(annotationName: String): KSAnnotation? =
        firstOrNull { it.qualifiedName in getListOfHibernatePackage(annotationName) }

    /**
     * Helper function to create HibernateReferenceData
     */
    private fun createReferenceData(
        tableInfo: Triple<String, String, ColumnType>,
        reference: Reference,
        columnName: String,
        unique: Boolean?,
        nullable: Boolean?
    ): HibernateReferenceData = HibernateReferenceData(
        type = tableInfo.third,
        reference = reference,
        columnName = columnName,
        unique = unique,
        nullable = nullable
    )

    /**
     * Extracts table name and primary key information from a class declaration with Hibernate annotations.
     *
     * @param classDeclaration The class declaration to analyze
     * @return Triple containing (table name, primary key name, primary key type) or null if not found
     */
    fun getTableNameWithPrimaryKey(
        classDeclaration: KSClassDeclaration,
        referenceColumnName: String?
    ): Triple<String, String, ColumnType>? {
        // Find @Table annotation
        val hibernateTable = classDeclaration.annotations.findReferenceAnnotation("Table") ?: return null

        var primaryKeyType: ColumnType? = null
        val primaryKey = referenceColumnName?.let { refColumn ->
            classDeclaration.getDeclaredProperties().firstOrNull { it.findColumnName() == refColumn }?.let {
                val type = it.type.resolve().declaration.qualifiedName?.asString()
                primaryKeyType = type?.let(::guessPropertyType)
                refColumn
            } ?: throw IllegalArgumentException("Reference column '$refColumn' not found")
        } ?: run {
            classDeclaration.getDeclaredProperties().firstOrNull { property ->
                property.annotations.findReferenceAnnotation("Id")?.also {
                    val type = property.type.resolve().declaration.qualifiedName?.asString()
                    primaryKeyType = type?.let(::guessPropertyType)
                } != null
            }?.findColumnName()
        }

        primaryKeyType ?: return null

        return Triple(
            hibernateTable.arguments.getArgument("name") ?: classDeclaration.toTableName(),
            primaryKey ?: throw IllegalStateException("Primary key not found"),
            primaryKeyType
        )
    }

    private fun KSPropertyDeclaration.findColumnName(): String? {
        val columnAnnotation = annotations.find {
            it.qualifiedName in getListOfHibernatePackage("Column")
        }
        return columnAnnotation?.findArgument<String>("name")?.takeIf { it.isNotEmpty() } ?: simpleName.asString()
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
                (computedFieldInfo?.second == true)

        // Validate date-related configurations
        val autoNowDate = getAutoNowDateAnnotation(property.annotations)
        validateFieldAutoNowDate(fieldType, autoNowDate, fieldName)

        val hasRichEditor = hasRichEditorAnnotation(property.annotations)
        val hasTextAreaField = hasTextAreaFieldAnnotation(property.annotations)

        if ((hasTextAreaField || hasRichEditor) && fieldType !is FieldType.String) {
            throw IllegalArgumentException("($fieldName) Fields annotated with @TextAreaField or marked as rich editors must be of type String.")
        }

        return FieldSet(
            fieldName = fieldName,
            verboseName = verboseName,
            type = fieldType,
            nullable = infoAnnotation?.findArgument<Boolean>("nullable") == true,
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
            preview = getPreviewAnnotation(property.annotations),
            hasRichEditor = hasRichEditor,
            hasTextArea = hasTextAreaField,
            hasConfirmation = hasConfirmationAnnotation(property.annotations),
        )
    }

    /**
     * Checks if a property has the Enumeration annotation.
     */
    private fun hasEnumerationColumnAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.qualifiedName == Enumeration::class.qualifiedName }

    /**
     * Checks if a property has the RichEditor annotation.
     */
    private fun hasRichEditorAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.qualifiedName == RichEditor::class.qualifiedName }

    /**
     * Checks if a property has the TextAreaField annotation.
     */
    private fun hasTextAreaFieldAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.qualifiedName == TextAreaField::class.qualifiedName }

    /**
     * Checks if a property has the Confirmation annotation.
     */
    private fun hasConfirmationAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.qualifiedName == Confirmation::class.qualifiedName }

    /**
     * Extracts AutoNowDate annotation configuration if present.
     */
    private fun getAutoNowDateAnnotation(annotations: Sequence<KSAnnotation>) =
        annotations.firstOrNull { it.qualifiedName == AutoNowDate::class.qualifiedName }
            ?.let {
                models.date.AutoNowDate(
                    updateOnChange = it.arguments.getArgument<Boolean>("updateOnChange") == true
                )
            }

    /**
     * Extracts Preview annotation configuration if present.
     */
    private fun getPreviewAnnotation(annotations: Sequence<KSAnnotation>) =
        annotations.firstOrNull { it.qualifiedName == Preview::class.qualifiedName }
            ?.arguments?.getArgument<String>("key")

    /**
     * Extracts ValueMapper annotation configuration if present.
     */
    private fun getValueMapperAnnotation(annotations: Sequence<KSAnnotation>) =
        annotations.firstOrNull { it.qualifiedName == ValueMapper::class.qualifiedName }?.arguments?.getArgument<String>(
            "key"
        )

    /**
     * Checks if a property should be ignored in panels.
     */
    private fun hasIgnoreColumnAnnotation(annotations: Sequence<KSAnnotation>): Boolean =
        annotations.any { it.qualifiedName == IgnoreColumn::class.qualifiedName }

    /**
     * Extracts enumeration values from annotations.
     */
    private fun Sequence<KSAnnotation>.getEnumerations(): List<String>? =
        find { it.qualifiedName == Enumeration::class.qualifiedName }
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
        find { it.qualifiedName == StatusStyle::class.qualifiedName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "color" }
            ?.value
            ?.let { it as? List<*> }
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }


    private fun Sequence<KSAnnotation>.getOverrideType(): ColumnType? =
        find { it.qualifiedName == OverrideColumnType::class.qualifiedName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "type" }
            ?.value?.let { item ->
                enumValues<ColumnType>().find { it.name == item.toString().split(".").last() }
            }

    /**
     * Extracts reference configuration from annotations.
     */

    private val OneToOneReferencesQualifiedName = OneToOneReferences::class.qualifiedName
    private val ManyToOneReferencesQualifiedName = ManyToOneReferences::class.qualifiedName
    private val ManyToManyReferencesQualifiedName = ManyToManyReferences::class.qualifiedName

    private fun Sequence<KSAnnotation>.getReferences(): Reference? =
        find {
            it.qualifiedName.orEmpty() in listOf(
                OneToOneReferencesQualifiedName,
                ManyToManyReferencesQualifiedName,
                ManyToOneReferencesQualifiedName
            )
        }?.let {
            when (it.qualifiedName) {
                OneToOneReferencesQualifiedName -> {
                    Reference.OneToOne(
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
        find { it.qualifiedName == Limits::class.qualifiedName }?.arguments?.let { args ->
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
        find { it.qualifiedName == Computed::class.qualifiedName }
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
     * Validates text area configuration.
     */
    private fun validateTextArea(hasTextArea: Boolean, columnType: ColumnType) {
        if (hasTextArea && columnType != ColumnType.STRING) {
            throw IllegalArgumentException("Text area can only be used with columns of type STRING.")
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