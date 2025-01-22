package repository

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import models.UploadTarget
import providers.AWSS3StorageProvider
import providers.StorageProvider
import providers.LocalStorageProvider
import java.io.File

internal object FileRepository {
    var defaultPath: String? = null
    var mediaRoot: String? = null
    private val storageProviders = mutableListOf<StorageProvider>()

    private fun getStorageProvider(key: String?): StorageProvider {
        return storageProviders.find { it.key == key }
            ?: throw NoSuchElementException("Storage provider with key '$key' not found.")
    }

    fun registerStorageProvider(storageProvider: StorageProvider) {
        if (storageProviders.any { it.key == storageProvider.key }) {
            throw IllegalArgumentException("A storage provider with key '${storageProvider.key}' already exists.")
        }
        storageProviders.add(storageProvider)
    }

    suspend fun uploadFile(
        uploadTarget: UploadTarget,
        partData: PartData.FileItem,
    ): Pair<String, ByteArray>? {
        return when (uploadTarget) {
            is UploadTarget.LocalFile -> {
                if (defaultPath == null && uploadTarget.path == null) {
                    throw IllegalArgumentException("Upload path is not provided")
                }
                val path = uploadTarget.path ?: defaultPath!!
                createPath(path)
                partData.saveToLocal(path)
            }

            is UploadTarget.AwsS3 -> {
                val bytes = partData.readBytes()
                AWSS3StorageProvider.uploadFile(
                    bytes = bytes,
                    fileName = partData.originalFileName,
                    bucket = uploadTarget.bucket
                )?.let { it to bytes }
            }

            is UploadTarget.Custom -> {
                val bytes = partData.readBytes()
                getStorageProvider(uploadTarget.key).uploadFile(
                    bytes = bytes,
                    fileName = partData.originalFileName,
                )?.let { it to bytes }
            }
        }
    }


    private fun createPath(path: String) {
        File(path).apply { if (!exists()) mkdir() }
    }

    private suspend fun PartData.FileItem.readBytes() = provider().readRemaining().readByteArray()

    private suspend fun PartData.FileItem.saveToLocal(
        path: String,
    ): Pair<String, ByteArray>? {
        val bytes = readBytes()
        return LocalStorageProvider.uploadFile(
            bytes = bytes,
            fileName = originalFileName,
            path = path
        )?.let { it to bytes }
    }

    suspend fun generateMediaUrl(
        uploadTarget: UploadTarget,
        fileName: String,
        call: ApplicationCall
    ): String? {
        return when (uploadTarget) {
            is UploadTarget.LocalFile -> {
                if (mediaRoot == null) {
                    throw IllegalArgumentException("Media root is not provided. Please specify a valid media root.")
                }
                return LocalStorageProvider.getFileUrl(fileName, call)
            }

            is UploadTarget.AwsS3 -> AWSS3StorageProvider.getFileUrl(fileName, bucket = uploadTarget.bucket)

            is UploadTarget.Custom -> {
                getStorageProvider(uploadTarget.key).getFileUrl(fileName, call)
            }
        }
    }
}