package repository

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import models.UploadTarget
import repository.FileRepository.saveToLocal
import java.io.File

internal object FileRepository {
    var defaultPath: String? = null
    var mediaRoot: String? = null

    suspend fun uploadFile(
        uploadTarget: UploadTarget,
        partData: PartData.FileItem,
    ): String? {
        return when (uploadTarget) {
            is UploadTarget.LocalFile -> {
                if (defaultPath == null && uploadTarget.path == null) {
                    throw IllegalArgumentException("Upload path is not provided")
                }
                val path = uploadTarget.path ?: defaultPath!!
                createPath(path)
                partData.saveToLocal(path)
            }

            else -> ""
        }
    }

    private fun createPath(path: String) {
        File(path).apply { if (!exists()) mkdir() }
    }

    private suspend fun PartData.FileItem.saveToLocal(
        path: String,
    ): String? {
        var filename = originalFileName ?: return null
        val bytes = provider().readRemaining().readByteArray()
        if (bytes.isEmpty()) return null
        val file = File("${path}/${filename}").requiredSave(path)
        filename = file.name
        file.writeBytes(bytes)
        dispose()
        return filename
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


    fun generateMediaUrl(
        uploadTarget: UploadTarget,
        fileName: String,
        call: ApplicationCall
    ): String {
        return when (uploadTarget) {
            is UploadTarget.LocalFile -> {
                if (mediaRoot == null) {
                    throw IllegalArgumentException("Media root is not provided. Please specify a valid media root.")
                }
                return call.getMediaUrl(fileName)
            }

            else -> ""
        }
    }

    private val ApplicationCall.baseUrl: String
        get() = "${request.origin.scheme}://${request.origin.serverHost}:${request.origin.serverPort}"

    private val ApplicationCall.mediaUrl: String
        get() = "$baseUrl/$mediaRoot"

    private fun ApplicationCall.getMediaUrl(filename: String) = "$mediaUrl/$filename"
}