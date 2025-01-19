package providers

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URI
import java.time.Duration

internal object AWSS3StorageProvider {
    private var client: S3Client? = null
    private var presigner: S3Presigner? = null

    var defaultBucket: String? = null

    var signatureDuration: Duration? = null

    private fun getRequiredClient(): S3Client {
        if (client == null) {
            throw IllegalStateException("S3 client is not initialized. Please ensure the client is properly configured before using it.")
        }
        return client!!
    }

    fun uploadFile(bytes: ByteArray, fileName: String?, bucket: String?): String? {
        if (fileName == null || bytes.isEmpty()) return null
        val requiredBucket = bucket ?: defaultBucket
        if (requiredBucket == null) {
            throw IllegalStateException("Bucket name is required but was not provided. Please ensure a valid bucket name is specified, or set a default bucket.")
        }
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(requiredBucket)
            .key(fileName)
            .build()
        getRequiredClient().putObject(putObjectRequest, RequestBody.fromBytes(bytes))
        return fileName
    }

    fun getFileUrl(fileName: String, bucket: String?): String? {
        val requiredBucket = bucket ?: defaultBucket
        if (requiredBucket == null) {
            throw IllegalStateException("Bucket name is required but was not provided. Please ensure a valid bucket name is specified, or set a default bucket.")
        }
        return if (signatureDuration != null) {
            getUrlWithDuration(fileName, requiredBucket)
        } else {
            return getRequiredClient().utilities().getUrl {
                it.bucket(requiredBucket).key(fileName)
            }.toExternalForm()
        }
    }

    private fun getUrlWithDuration(fileName: String, bucket: String?): String? {
        if (presigner == null) {
            throw IllegalStateException("S3 client is not initialized. Please ensure the client is properly configured before using it.")
        }
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(fileName)
            .build()
        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(signatureDuration)
            .getObjectRequest(getObjectRequest)
            .build()
        val presignedUrl = presigner!!.presignGetObject(presignRequest)
        return presignedUrl.url()?.toString()
    }


    fun register(
        secretKey: String,
        accessKey: String,
        region: String,
        endpoint: String?
    ) {
        val currentRegion = Region.of(region)
        val credentials = createCredentials(secretKey = secretKey, accessKey = accessKey)
        client = S3Client.builder()
            .region(currentRegion)
            .let {
                if (endpoint != null) {
                    it.endpointOverride(URI(endpoint))
                } else it
            }
            .credentialsProvider(
                credentials
            ).build()
        presigner = S3Presigner.builder().s3Client(getRequiredClient()).region(currentRegion).let {
            if (endpoint != null) {
                it.endpointOverride(URI(endpoint))
            } else it
        }.s3Client(client).credentialsProvider(
            credentials
        ).build()
    }

    private fun createCredentials(secretKey: String, accessKey: String): AwsCredentialsProvider? {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                accessKey, secretKey
            )
        )
    }
}