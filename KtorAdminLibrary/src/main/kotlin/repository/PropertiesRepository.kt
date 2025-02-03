package repository

import annotations.computed.Computed
import annotations.date.AutoNowDate
import annotations.enumeration.Enumeration
import annotations.field.FieldInfo
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.references.References
import annotations.status.StatusStyle
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.squareup.kotlinpoet.ksp.toClassName
import models.ColumnSet
import models.types.ColumnType
import models.Limit
import models.common.Reference
import models.field.FieldSet
import models.types.FieldType
import utils.UploadUtils
import utils.findArgument
import utils.guessPropertyType

object PropertiesRepository {
    fun getColumnSets(property: KSPropertyDeclaration, type: KSType): ColumnSet? {
        val hasUploadAnnotation = UploadUtils.hasUploadAnnotation(property.annotations)
        val hasEnumerationColumnAnnotation = hasEnumerationColumnAnnotation(property.annotations)
        val genericArgument = type.arguments.firstOrNull()?.type?.resolve()?.toClassName()?.canonicalName ?: return null
        val name = property.simpleName.asString()
        val infoAnnotation =
            property.annotations.find { it.shortName.asString() == ColumnInfo::class.simpleName }
        val columnName = infoAnnotation?.findArgument<String>("columnName")?.takeIf { it.isNotEmpty() } ?: name
        val verboseName = infoAnnotation?.findArgument<String>("verboseName")?.takeIf { it.isNotEmpty() } ?: columnName
        val enumValues = property.annotations.getEnumerations()
        if (hasUploadAnnotation) {
            UploadUtils.validatePropertyType(genericArgument, columnName)
        }
        val columnType = when {
            hasEnumerationColumnAnnotation -> ColumnType.ENUMERATION
            hasUploadAnnotation -> ColumnType.FILE
            else -> guessPropertyType(genericArgument)
        }
        val statusColors = property.annotations.getStatusStyles()
        if (columnType != ColumnType.ENUMERATION && statusColors != null) {
            throw IllegalArgumentException("StatusStyle can only be used with ENUMERATION column types.")
        }
        val hexColorPattern = "^#([A-Fa-f0-9]{6})$".toRegex()
        if ((statusColors != null && enumValues != null) && statusColors.count() != enumValues.count() || statusColors?.any {
                !hexColorPattern.matches(
                    it
                )
            } == true) {
            throw IllegalArgumentException("($name) Invalid status colors: The number of status colors must match the number of enumeration values, and all colors must be valid hex codes (excluding #000000).")
        }
        val showInPanel = hasIgnoreColumnAnnotation(property.annotations).not()
        val nullable = infoAnnotation?.findArgument<Boolean>("nullable") ?: false
        val defaultValue = infoAnnotation?.findArgument<String>("defaultValue")?.takeIf { it.isNotEmpty() }
        val uploadTarget = UploadUtils.getUploadTargetFromAnnotation(property.annotations)
        val allowedMimeTypes =
            if (hasUploadAnnotation) UploadUtils.getAllowedMimeTypesFromAnnotation(property.annotations) else null
        val computedColumnInfo = property.annotations.getComputed()
        val isReadOnly =
            (infoAnnotation?.findArgument<Boolean>("readOnly") ?: false) || (computedColumnInfo?.second ?: false)
        val autoNowDate = getAutoNowDateAnnotation(property.annotations)
        if (columnType !in listOf(ColumnType.DATE, ColumnType.DATETIME) && autoNowDate != null) {
            throw IllegalArgumentException(
                "The 'autoNowDate' property can only be used with columns of type 'DATE' or 'DATETIME'. " +
                        "Column '$columnName' has type '$columnType', which is incompatible."
            )
        }
        return ColumnSet(
            columnName = columnName,
            type = columnType,
            verboseName = verboseName,
            nullable = nullable,
            showInPanel = showInPanel,
            uploadTarget = uploadTarget,
            allowedMimeTypes = allowedMimeTypes,
            defaultValue = defaultValue,
            enumerationValues = enumValues,
            limits = property.annotations.getLimits(),
            reference = property.annotations.getReferences(),
            readOnly = isReadOnly,
            computedColumn = computedColumnInfo?.first,
            autoNowDate = autoNowDate,
            statusColors = statusColors
        )
    }

    fun getFieldSet(property: KSPropertyDeclaration, type: FieldType): FieldSet? {
        val hasUploadAnnotation = UploadUtils.hasUploadAnnotation(property.annotations)
        val hasEnumerationColumnAnnotation = hasEnumerationColumnAnnotation(property.annotations)
        val name = property.simpleName.asString()
        val infoAnnotation =
            property.annotations.find { it.shortName.asString() == FieldInfo::class.simpleName }
        val fieldName = infoAnnotation?.findArgument<String>("fieldName")?.takeIf { it.isNotEmpty() } ?: name
        val verboseName =
            infoAnnotation?.findArgument<String>("verboseName")?.takeIf { it.isNotEmpty() } ?: fieldName ?: name
        if (hasUploadAnnotation) {
            UploadUtils.validatePropertyType(fieldName, type)
        }
        val fieldType = when {
            hasEnumerationColumnAnnotation -> FieldType.Enumeration
            hasUploadAnnotation -> FieldType.File
            else -> type
        }
        val showInPanel = hasIgnoreColumnAnnotation(property.annotations).not()
        val nullable = infoAnnotation?.findArgument<Boolean>("nullable") ?: false
        val defaultValue = infoAnnotation?.findArgument<String>("defaultValue")?.takeIf { it.isNotEmpty() }
        val uploadTarget = UploadUtils.getUploadTargetFromAnnotation(property.annotations)
        val allowedMimeTypes =
            if (hasUploadAnnotation) UploadUtils.getAllowedMimeTypesFromAnnotation(property.annotations) else null
        val computedFieldInfo = property.annotations.getComputed()
        val isReadOnly =
            (infoAnnotation?.findArgument<Boolean>("readOnly") ?: false) || (computedFieldInfo?.second ?: false)
        val autoNowDate = getAutoNowDateAnnotation(property.annotations)
        if (fieldType !in listOf(FieldType.Date, FieldType.DateTime, FieldType.Instant) && autoNowDate != null) {
            throw IllegalArgumentException(
                "The 'autoNowDate' property can only be used with fields of type 'DATE' or 'DATETIME'. " +
                        "Field '$fieldName' has type '$fieldType', which is incompatible."
            )
        }
        return FieldSet(
            fieldName = fieldName,
            verboseName = verboseName,
            type = fieldType,
            nullable = nullable,
            showInPanel = showInPanel,
            uploadTarget = uploadTarget,
            allowedMimeTypes = allowedMimeTypes,
            defaultValue = defaultValue,
            enumerationValues = property.annotations.getEnumerations(),
            limits = property.annotations.getLimits(),
            reference = property.annotations.getReferences(),
            readOnly = isReadOnly,
            computedField = computedFieldInfo?.first,
            autoNowDate = autoNowDate,
        )
    }

    private fun hasEnumerationColumnAnnotation(annotations: Sequence<KSAnnotation>): Boolean = annotations.any {
        it.shortName.asString() == Enumeration::class.simpleName
    }

    private fun getAutoNowDateAnnotation(annotations: Sequence<KSAnnotation>) = annotations.firstOrNull {
        it.shortName.asString() == AutoNowDate::class.simpleName
    }?.let { models.date.AutoNowDate(updateOnChange = it.arguments.getArgument<Boolean>("updateOnChange") == true) }

    private fun hasIgnoreColumnAnnotation(annotations: Sequence<KSAnnotation>): Boolean = annotations.any {
        it.shortName.asString() == IgnoreColumn::class.simpleName
    }

    private fun Sequence<KSAnnotation>.getEnumerations(): List<String>? {
        return find { it.shortName.asString() == Enumeration::class.simpleName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "values" }
            ?.value
            ?.let { it as? List<*> }
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun Sequence<KSAnnotation>.getStatusStyles(): List<String>? {
        return find { it.shortName.asString() == StatusStyle::class.simpleName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "color" }
            ?.value
            ?.let { it as? List<*> }
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun Sequence<KSAnnotation>.getReferences(): Reference? {
        return find { it.shortName.asString() == References::class.simpleName }
            ?.arguments
            ?.let {
                Reference(
                    tableName = it.firstOrNull { argument -> argument.name?.asString() == "tableName" }!!.value as String,
                    columnName = it.firstOrNull { argument -> argument.name?.asString() == "targetColumn" }!!.value as String,
                )
            }
    }

    private fun Sequence<KSAnnotation>.getLimits(): Limit? {
        return find { it.shortName.asString() == Limits::class.simpleName }?.arguments?.let { args ->
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
    }

    private fun Sequence<KSAnnotation>.getComputed(): Pair<String, Boolean>? {
        return find { it.shortName.asString() == Computed::class.simpleName }
            ?.arguments?.let {
                it.getArgument<String>("compute")!! to it.getArgument<Boolean>("readOnly")!!
            }
    }

    private inline fun <reified D> List<KSValueArgument>.getArgument(name: String): D? =
        firstOrNull { it.name?.asString() == name }?.value as? D
}