package repository

import io.ktor.server.application.ApplicationCall
import models.UploadTarget
import providers.AWSS3StorageProvider
import providers.LocalStorageProvider
import providers.StorageProvider
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
        bytes: ByteArray,
        originalFileName: String?
    ): Pair<String, ByteArray>? {
        return when (uploadTarget) {
            is UploadTarget.LocalFile -> {
                if (defaultPath == null && uploadTarget.path == null) {
                    throw IllegalArgumentException("Upload path is not provided")
                }
                val path = uploadTarget.path ?: defaultPath!!
                createPath(path)
                saveToLocal(path, bytes, originalFileName)
            }

            is UploadTarget.AwsS3 -> {
                AWSS3StorageProvider.uploadFile(
                    bytes = bytes,
                    fileName = originalFileName,
                    bucket = uploadTarget.bucket
                )?.let { it to bytes }
            }

            is UploadTarget.Custom -> {
                getStorageProvider(uploadTarget.key).uploadFile(
                    bytes = bytes,
                    fileName = originalFileName,
                )?.let { it to bytes }
            }
        }
    }

    suspend fun deleteFile(uploadTarget: UploadTarget, fileName: String): Boolean {
        return when (uploadTarget) {
            is UploadTarget.LocalFile -> LocalStorageProvider.deleteFile(
                fileName,
                uploadTarget.path ?: defaultPath!!
            )

            is UploadTarget.AwsS3 -> AWSS3StorageProvider.deleteFile(
                fileName,
                uploadTarget.bucket
            )

            is UploadTarget.Custom -> getStorageProvider(uploadTarget.key).deleteFile(fileName)
        }
    }

    private fun createPath(path: String) {
        File(path).apply { if (!exists()) mkdir() }
    }


    private fun saveToLocal(
        path: String,
        bytes: ByteArray,
        originalFileName: String?
    ): Pair<String, ByteArray>? {
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

            is UploadTarget.AwsS3 -> AWSS3StorageProvider.getFileUrl(
                fileName,
                bucket = uploadTarget.bucket
            )

            is UploadTarget.Custom -> {
                getStorageProvider(uploadTarget.key).getFileUrl(fileName, call)
            }
        }
    }
}