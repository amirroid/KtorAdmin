package ir.amirroid.ktoradmin.utils

import ir.amirroid.ktoradmin.annotations.file.AllowedMimeTypes
import ir.amirroid.ktoradmin.annotations.uploads.S3Upload
import ir.amirroid.ktoradmin.annotations.uploads.CustomUpload
import ir.amirroid.ktoradmin.annotations.uploads.LocalUpload
import com.google.devtools.ksp.symbol.KSAnnotation
import ir.amirroid.ktoradmin.models.FileDeleteStrategy
import ir.amirroid.ktoradmin.models.UploadTarget
import ir.amirroid.ktoradmin.models.types.FieldType

object UploadUtils {
    private val awsS3Name = S3Upload::class.simpleName
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
                awsS3Name -> UploadTarget.AwsS3(
                    it.findArgumentIfIsNotEmpty("bucket"),
                    it.findDeleteStrategy()
                )

                customName -> UploadTarget.Custom(it.findArgumentIfIsNotEmpty("key"))
                localName -> UploadTarget.LocalFile(
                    it.findArgumentIfIsNotEmpty("path"),
                    it.findDeleteStrategy()
                )

                else -> null
            }
        }
    }

    private fun KSAnnotation.findDeleteStrategy(key: String = "deleteStrategy") =
        arguments.find { it.name?.asString() == key }?.let { item ->
            enumValues<FileDeleteStrategy>().find { it.name == item.toString().split(".").last() }
        }!!

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

    fun validatePropertyType(fieldName: String, fieldType: FieldType): Boolean {
        if (fieldType != FieldType.String) {
            throw IllegalArgumentException("Field '$fieldType' must be of type 'kotlin.String' for file uploads. Only properties of type 'kotlin.String' are allowed for file uploads.")
        }
        return true
    }
}