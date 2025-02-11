package providers

import io.ktor.server.application.*
import repository.FileRepository.mediaRoot
import utils.mediaUrl
import java.io.File

/**
 * Local Storage Provider implementation for handling file operations on the local filesystem.
 * Provides functionality for uploading files and generating URLs for stored files.
 */
internal object LocalStorageProvider {

    /**
     * Uploads a file to the local filesystem.
     * If a file with the same name exists, creates a new file with an incremented counter in parentheses.
     *
     * @param bytes The file content as byte array
     * @param fileName The name of the file to be stored
     * @param path The directory path where the file should be stored
     * @return The actual filename used for storage (may include counter if duplicate), or null if parameters are invalid
     */
    fun uploadFile(bytes: ByteArray, fileName: String?, path: String?): String? {
        if (fileName == null || path == null || bytes.isEmpty()) return null
        val file = File("${path}/${fileName}").requiredSave(path)
        file.writeBytes(bytes)
        return file.name
    }

    /**
     * Generates a URL for accessing a stored file.
     *
     * @param fileName The name of the file to generate URL for
     * @param call The Ktor ApplicationCall instance to generate the URL
     * @return The complete URL for accessing the file
     * @throws IllegalArgumentException if media root is not configured
     */
    fun getFileUrl(fileName: String, call: ApplicationCall): String {
        if (mediaRoot == null) {
            throw IllegalArgumentException("Media root is not provided. Please specify a valid media root.")
        }
        return call.getMediaUrl(fileName)
    }

    /**
     * Ensures a unique file is created for saving content.
     * If a file with the same name exists, creates a new file with an incremented counter in parentheses.
     *
     * @param path The directory path where the file should be stored
     * @return A File instance representing a unique, newly created file
     */
    private fun File.requiredSave(path: String): File {
        if (exists().not()) {
            createNewFile()
            return this
        }
        var counter = 1
        var file = this
        val baseName = nameWithoutExtension
        while (file.exists()) {
            val extension = file.extension
            val filename = "$baseName($counter).$extension"
            file = File("${path}/${filename}")
            counter++
        }
        file.createNewFile()
        return file
    }

    /**
     * Extension function to generate a media URL for a file.
     *
     * @param filename The name of the file to generate URL for
     * @return The complete media URL for the file
     */
    private fun ApplicationCall.getMediaUrl(filename: String) = "$mediaUrl/$filename"
}