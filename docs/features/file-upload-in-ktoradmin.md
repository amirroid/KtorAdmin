---
description: File management in KtorAdmin is handled through Local, S3, and Custom Storage.
---

# File Upload in KtorAdmin

KtorAdmin supports three types of file uploads:

1. Local Upload (Stores files on the server)
2. S3 Bucket Upload (Stores files in AWS S3 or compatible storage)
3. Custom Upload (Stores files using a user-defined storage provider)

Each method has its configuration and usage.

***

#### 1. Local Upload

To enable local file uploads, configure the plugin as follows:

```kotlin
install(KtorAdmin) {
    mediaPath = MEDIA_PATH  // Directory where files will be stored
    mediaRoot = MEDIA_ROOT  // URL path for accessing uploaded files
    fileDeleteStrategy = FileDeleteStrategy.DELETE // Strategy for deleting files
}
```

**Static File Routing**\
Ensure you add static file routing so uploaded files are accessible:

```kotlin
routing {
    staticFiles("/$MEDIA_ROOT", File(MEDIA_PATH))
}
```

**Using @LocalUpload on Fields**\
Apply `@LocalUpload` to any column or field that should support local uploads:

```kotlin
@LocalUpload
val file = varchar("file", 1000).nullable()
```

**Custom Upload Path**\
You can define a custom path for specific fields. If not set, the default `mediaPath` will be used:

```kotlin
@LocalUpload(path = "custom")
val profilePicture = varchar("profile_picture", 500).nullable()
```

The file is stored directly in the specified path. If no path is provided, it is stored in `mediaPath` as defined in the configuration.

**File Deletion Strategy per Field**\
You can also define how files are handled when the related record is deleted:

```kotlin
@LocalUpload(deleteStrategy = FileDeleteStrategy.KEEP)
val file = varchar("file", 1000).nullable()
```

By default, the value is `FileDeleteStrategy.INHERIT`, which uses the global setting from `KtorAdmin`.

***

#### 2. S3 Bucket Upload

For storing files in an AWS S3 bucket (or an S3-compatible storage), you must first register the S3 client and configure necessary settings.

**Registering the S3 Client**\
Call `registerS3Client` with the required parameters to set up the connection.

**Configuring the Plugin**

```kotlin
install(KtorAdmin) {
    defaultAwsS3Bucket = S3_BUCKET_NAME  // Default bucket name
    s3SignatureDuration = 1.minutes.toJavaDuration() // Link expiration time
    fileDeleteStrategy = FileDeleteStrategy.DELETE // Strategy for deleting files
}
```

* `defaultAwsS3Bucket` → Defines a default bucket for uploads.
* `s3SignatureDuration` → Defines how long the generated S3 URLs remain valid. This setting is required to ensure controlled access to files.
* `fileDeleteStrategy` → Defines the default strategy for deleting S3 files when a record is removed.

**Using @S3Upload on Fields**\
Apply `@S3Upload` to any column or field requiring S3 storage. By default, the file is stored in the bucket specified by `defaultAwsS3Bucket`.

```kotlin
@S3Upload
val file = varchar("file", 1000).nullable()
```

**Custom Bucket per Field**\
You can specify a custom bucket for individual fields:

```kotlin
@S3Upload(bucket = "user-files")
val profilePicture = varchar("profile_picture", 500).nullable()
```

Without `bucket` → Uses `defaultAwsS3Bucket`.\
With `bucket` → Stores the file in a specific bucket.\
This allows flexible storage solutions for different types of files.

**File Deletion Strategy per Field**

```kotlin
@S3Upload(deleteStrategy = FileDeleteStrategy.KEEP)
val file = varchar("file", 1000).nullable()
```

By default, `deleteStrategy` is `INHERIT`, meaning it follows the global configuration.

***

#### 3. Custom Upload

To enable custom file uploads, you must create a class that inherits from `StorageProvider`. This class handles file storage and URL generation.

**Creating a Custom Storage Provider**\
Implement the `StorageProvider` interface:

```kotlin
class CustomStorageProvider() : StorageProvider {
    override val key: String?
        get() = "main_provider"

    override suspend fun uploadFile(bytes: ByteArray, fileName: String?): String? {
        // Upload File
        return fileName
    }

    override suspend fun getFileUrl(
        fileName: String,
        call: ApplicationCall
    ): String? {
        // Generate file URL
        return url
    }
}
```

**Important Notes:**

* `key` → Must be unique. If `null`, this provider is considered the default provider for file storage.
* `uploadFile` → Handles the actual file upload and returns the file name.
* `getFileUrl` → Generates the accessible URL for the uploaded file.

**Registering the Custom Provider**\
Once your provider is created, register it in the plugin:

```kotlin
install(KtorAdmin) {
    registerStorageProvider(CustomStorageProvider())
    fileDeleteStrategy = FileDeleteStrategy.DELETE // Default deletion strategy
}
```

**Using `@CustomUpload` on Fields**\
Apply `@CustomUpload` to any column or field requiring custom storage:

```kotlin
@CustomUpload
val file = varchar("file", 1000).nullable()
```

**Using a Specific Provider**\
If you want a specific storage provider for a field, specify its key:

```kotlin
@CustomUpload("main_provider")
val file = varchar("file", 1000).nullable()
```

This ensures the file is uploaded using the specified storage provider instead of the default one.

***

#### File Deletion Strategy

KtorAdmin lets you define how linked files are handled when a database record is deleted.\
You can configure this behavior globally:

```kotlin
install(KtorAdmin) {
    fileDeleteStrategy = FileDeleteStrategy.DELETE
}
```

Available strategies:

* `DELETE` → Deletes the file when the record is removed.
* `KEEP` → Keeps the file in storage even after deletion.
* `INHERIT` → Uses the global default strategy.

By default, the global strategy is `KEEP`.
