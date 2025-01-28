package providers

import io.ktor.server.application.*
import repository.FileRepository.mediaRoot
import utils.mediaUrl
import java.io.File

internal object LocalStorageProvider {

    fun uploadFile(bytes: ByteArray, fileName: String?, path: String?): String? {
        println("UPLOAD FILE $fileName $path ${bytes.size}")
        if (fileName == null || path == null || bytes.isEmpty()) return null
        val file = File("${path}/${fileName}").requiredSave(path)
        file.writeBytes(bytes)
        return file.name
    }

    fun getFileUrl(fileName: String, call: ApplicationCall): String {
        if (mediaRoot == null) {
            throw IllegalArgumentException("Media root is not provided. Please specify a valid media root.")
        }
        return call.getMediaUrl(fileName)
    }

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

    private fun ApplicationCall.getMediaUrl(filename: String) = "$mediaUrl/$filename"
}