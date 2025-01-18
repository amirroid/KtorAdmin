package utils

import annotations.enumeration.EnumerationColumn
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.ColumnLimits
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.squareup.kotlinpoet.ksp.toClassName
import models.ColumnSet
import models.ColumnType
import models.Limit

object ColumnsUtils {
    fun getColumnSets(property: KSPropertyDeclaration, type: KSType): ColumnSet? {
        val hasUploadAnnotation = UploadUtils.hasUploadAnnotation(property.annotations)
        val hasEnumerationColumnAnnotation = hasEnumerationColumnAnnotation(property.annotations)
        val genericArgument = type.arguments.firstOrNull()?.type?.resolve()?.toClassName()?.canonicalName ?: return null
        val name = property.simpleName.asString()
        val infoAnnotation =
            property.annotations.find { it.shortName.asString() == ColumnInfo::class.simpleName }
        val columnName = infoAnnotation?.findArgument<String>("columnName") ?: name
        if (hasUploadAnnotation) {
            UploadUtils.validatePropertyType(genericArgument, columnName)
        }
        val columnType = when {
            hasEnumerationColumnAnnotation -> ColumnType.ENUMERATION
            hasUploadAnnotation -> ColumnType.FILE
            else -> guessPropertyType(genericArgument)
        }
        val showInPanel = hasIgnoreColumnAnnotation(property.annotations).not()
        val nullable = infoAnnotation?.findArgument<Boolean>("nullable") ?: false
        val defaultValue = infoAnnotation?.findArgument<String>("defaultValue")?.takeIf { it.isNotEmpty() }
        val uploadTarget = UploadUtils.getUploadTargetFromAnnotation(property.annotations)
        val allowedMimeTypes =
            if (hasUploadAnnotation) UploadUtils.getAllowedMimeTypesFromAnnotation(property.annotations) else null
        return ColumnSet(
            columnName = columnName,
            type = columnType,
            nullable = nullable,
            showInPanel = showInPanel,
            uploadTarget = uploadTarget,
            allowedMimeTypes = allowedMimeTypes,
            defaultValue = defaultValue,
            enumerationValues = property.annotations.getEnumerations(),
            limits = property.annotations.getLimits()
        )
    }

    private fun hasEnumerationColumnAnnotation(annotations: Sequence<KSAnnotation>): Boolean = annotations.any {
        it.shortName.asString() == EnumerationColumn::class.simpleName
    }


    private fun hasIgnoreColumnAnnotation(annotations: Sequence<KSAnnotation>): Boolean = annotations.any {
        it.shortName.asString() == IgnoreColumn::class.simpleName
    }

    private fun Sequence<KSAnnotation>.getEnumerations(): List<String>? {
        return find { it.shortName.asString() == EnumerationColumn::class.simpleName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "values" }
            ?.value
            ?.let { it as? List<*> }
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun hasColumnLimitsAnnotation(annotations: Sequence<KSAnnotation>): Boolean = annotations.any {
        it.shortName.asString() == ColumnLimits::class.simpleName
    }

    private fun Sequence<KSAnnotation>.getLimits(): Limit? {
        return find { it.shortName.asString() == ColumnLimits::class.simpleName }
            ?.arguments?.let {
                Limit(
                    maxLength = it.getArgument("maxLength"),
                    minLength = it.getArgument("minLength"),
                    regexPattern = it.getArgument<String?>("regexPattern")?.takeIf { it.isNotEmpty() },
                )
            }
    }

    private inline fun <reified D> List<KSValueArgument>.getArgument(name: String): D? =
        firstOrNull { it.name?.asString() == name }?.value as? D
}