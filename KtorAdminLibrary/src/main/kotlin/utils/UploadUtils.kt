package utils

import annotations.file.AllowedMimeTypes
import annotations.uploads.AwsS3Upload
import annotations.uploads.CustomUpload
import annotations.uploads.LocalUpload
import com.google.devtools.ksp.symbol.KSAnnotation
import models.UploadTarget

object UploadUtils {
    private val awsS3Name = AwsS3Upload::class.simpleName
    private val localName = LocalUpload::class.simpleName
    private val customName = CustomUpload::class.simpleName
    private val annotationNames = listOf(
        awsS3Name, customName, localName
    )

    fun hasUploadAnnotation(annotations: Sequence<KSAnnotation>): Boolean = annotations.any {
        it.shortName.asString() in annotationNames
    }

    fun getUploadTargetFromAnnotation(annotations: Sequence<KSAnnotation>): UploadTarget? {
        return annotations.find {
            it.shortName.asString() in annotationNames
        }?.let {
            when (it.shortName.asString()) {
                awsS3Name -> UploadTarget.AwsS3
                customName -> UploadTarget.Custom(it.findArgumentIfIsNotEmpty("key"))
                localName -> UploadTarget.LocalFile(it.findArgumentIfIsNotEmpty("path"))
                else -> null
            }
        }
    }

    fun getAllowedMimeTypesFromAnnotation(annotations: Sequence<KSAnnotation>): List<String>? {
        return annotations.find { it.shortName.asString() == AllowedMimeTypes::class.simpleName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "types" }
            ?.value
            ?.let { it as? List<*> }
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }
    }

    fun validatePropertyType(columnType: String, columnName: String): Boolean {
        if (columnType != "kotlin.String") {
            throw IllegalArgumentException("Column '$columnName' must be of type 'kotlin.String' for file uploads. Only properties of type 'kotlin.String' are allowed for file uploads.")
        }
        return true
    }
}