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

/**
 * AWS S3 Storage Provider implementation for handling file operations with Amazon S3.
 * Provides functionality for uploading files, generating URLs, and managing S3 client configuration.
 */
internal object AWSS3StorageProvider {
    private var client: S3Client? = null
    private var presigner: S3Presigner? = null

    /** Default S3 bucket name to be used when no specific bucket is provided */
    var defaultBucket: String? = null

    /** Duration for which generated presigned URLs will be valid */
    var signatureDuration: Duration? = null

    /**
     * Retrieves the initialized S3 client or throws an exception if not initialized.
     * @throws IllegalStateException if the client is not initialized
     */
    private fun getRequiredClient(): S3Client {
        if (client == null) {
            throw IllegalStateException("S3 client is not initialized. Please ensure the client is properly configured before using it.")
        }
        return client!!
    }

    /**
     * Uploads a file to S3 bucket.
     *
     * @param bytes The file content as byte array
     * @param fileName The name of the file to be stored
     * @param bucket Optional bucket name, falls back to default bucket if not provided
     * @return The filename if upload is successful, null otherwise
     * @throws IllegalStateException if no bucket is specified and no default bucket is set
     */
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

    /**
     * Generates a URL for accessing a file in S3.
     * If signatureDuration is set, generates a presigned URL valid for that duration.
     *
     * @param fileName The name of the file to generate URL for
     * @param bucket Optional bucket name, falls back to default bucket if not provided
     * @return The generated URL as string, or null if generation fails
     * @throws IllegalStateException if no bucket is specified and no default bucket is set
     */
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

    /**
     * Generates a presigned URL with the configured duration.
     *
     * @param fileName The name of the file to generate URL for
     * @param bucket The bucket name
     * @return The presigned URL as string, or null if generation fails
     * @throws IllegalStateException if the presigner is not initialized
     */
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

    /**
     * Registers and initializes the S3 client and presigner with provided credentials and configuration.
     *
     * @param secretKey AWS secret key
     * @param accessKey AWS access key
     * @param region AWS region name
     * @param endpoint Optional custom endpoint URL
     */
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

    /**
     * Creates AWS credentials provider using the provided access and secret keys.
     *
     * @param secretKey AWS secret key
     * @param accessKey AWS access key
     * @return AWS credentials provider
     */
    private fun createCredentials(secretKey: String, accessKey: String): AwsCredentialsProvider? {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                accessKey, secretKey
            )
        )
    }
}