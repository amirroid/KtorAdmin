package models

sealed class UploadTarget {
    data class LocalFile(val path: String?) : UploadTarget()
    data object AwsS3 : UploadTarget()
    data class Custom(val key: String?) : UploadTarget()
}

fun UploadTarget.toFormattedString(): String {
    return when (this) {
        is UploadTarget.LocalFile -> "UploadTarget.LocalFile(path=${path?.let { "\"$it\"" } ?: "null"})"
        is UploadTarget.AwsS3 -> "UploadTarget.AwsS3"
        is UploadTarget.Custom -> "UploadTarget.Custom(key=${key?.let { "\"$it\"" } ?: "null"})"
    }
}