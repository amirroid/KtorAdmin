package providers

import io.ktor.server.application.*

/**
 * Interface representing a storage provider responsible for handling file uploads and generating file URLs.
 */
interface StorageProvider {

    /**
     * Unique identifier for the provider.
     * If `null`, this provider is considered the default provider for file storage operations.
     */
    val key: String?

    /**
     * Uploads a file to the storage provider.
     *
     * @param bytes The content of the file as a byte array.
     * @param fileName The name of the file to be uploaded. If `null`, a default name may be used depending on the provider.
     * @return The name of the uploaded file, or `null` if the upload failed or the provider does not return a file name.
     */
    suspend fun uploadFile(
        bytes: ByteArray,
        fileName: String?,
    ): String?

    /**
     * Generates the URL of a file stored on the provider based on the file's name.
     * This method constructs the URL for accessing the file from the storage provider.
     *
     * @param fileName The name of the file for which to generate the URL.
     * @param call The application call, which may be used for context (such as base URL, etc.).
     * @return The URL pointing to the requested file.
     */
    suspend fun getFileUrl(fileName: String, call: ApplicationCall): String?
}
