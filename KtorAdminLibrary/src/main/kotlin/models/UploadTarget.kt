package models

/**
 * Represents different targets for uploading files.
 * This sealed class allows flexibility in defining various storage destinations.
 * If properties are `null`, the default values should be used.
 */
sealed class UploadTarget {

    /**
     * Uploads the file to a local directory.
     * @property path The path where the file should be stored locally. If `null`, a default path should be used.
     */
    data class LocalFile(val path: String?) : UploadTarget()

    /**
     * Uploads the file to an AWS S3 bucket.
     * @property bucket The name of the S3 bucket where the file should be uploaded. If `null`, a default bucket should be used.
     */
    data class AwsS3(val bucket: String?) : UploadTarget()

    /**
     * Uploads the file to a custom storage solution.
     * @property key The key or identifier for the custom storage location. If `null`, a default key should be used.
     */
    data class Custom(val key: String?) : UploadTarget()
}

fun UploadTarget.toFormattedString(): String {
    return when (this) {
        is UploadTarget.LocalFile -> "UploadTarget.LocalFile(path=${path?.let { "\"$it\"" } ?: "null"})"
        is UploadTarget.AwsS3 -> "UploadTarget.AwsS3(bucket= ${bucket?.let { "\"$it\"" } ?: "null"})"
        is UploadTarget.Custom -> "UploadTarget.Custom(key=${key?.let { "\"$it\"" } ?: "null"})"
    }
}