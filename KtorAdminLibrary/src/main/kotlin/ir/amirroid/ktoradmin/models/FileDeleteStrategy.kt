package ir.amirroid.ktoradmin.models


/**
 * Strategy for handling file deletion in storage providers.
 */
enum class FileDeleteStrategy {
    /** Actually delete the file from storage */
    DELETE,

    /** Ignore deletion and keep the file */
    KEEP,

    /** Inherit strategy from default configuration */
    INHERIT
}