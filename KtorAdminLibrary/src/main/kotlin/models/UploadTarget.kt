package models

sealed class UploadTarget {
    data class LocalFile(val path: String?) : UploadTarget()
    data class AwsS3(val bucket: String?) : UploadTarget()
    data class Custom(val key: String?) : UploadTarget()
}

fun UploadTarget.toFormattedString(): String {
    return when (this) {
        is UploadTarget.LocalFile -> "UploadTarget.LocalFile(path=${path?.let { "\"$it\"" } ?: "null"})"
        is UploadTarget.AwsS3 -> "UploadTarget.AwsS3(bucket= ${bucket?.let { "\"$it\"" } ?: "null"})"
        is UploadTarget.Custom -> "UploadTarget.Custom(key=${key?.let { "\"$it\"" } ?: "null"})"
    }
}