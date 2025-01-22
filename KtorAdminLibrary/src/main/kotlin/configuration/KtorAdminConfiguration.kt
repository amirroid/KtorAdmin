package configuration

import com.vladsch.kotlin.jdbc.HikariCP
import com.vladsch.kotlin.jdbc.SessionImpl
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import listener.AdminEventListener
import providers.StorageProvider
import providers.AWSS3StorageProvider
import repository.FileRepository
import software.amazon.awssdk.regions.Region
import java.time.Duration

class KtorAdminConfiguration {
    private val jdbcDataSources = mutableListOf<String>()
    var mediaPath: String?
        get() = FileRepository.defaultPath
        set(value) {
            FileRepository.defaultPath = value
        }
    var mediaRoot: String?
        get() = FileRepository.mediaRoot
        set(value) {
            FileRepository.mediaRoot = value
        }

    var defaultAwsS3Bucket: String?
        get() = AWSS3StorageProvider.defaultBucket
        set(value) {
            AWSS3StorageProvider.defaultBucket = value
        }

    var awsS3SignatureDuration: Duration?
        get() = AWSS3StorageProvider.signatureDuration
        set(value) {
            AWSS3StorageProvider.signatureDuration = value
        }

    var maxItemsInPage: Int
        get() = DynamicConfiguration.maxItemsInPage
        set(value) {
            DynamicConfiguration.maxItemsInPage = value
        }

    fun registerStorageProvider(storageProvider: StorageProvider) {
        FileRepository.registerStorageProvider(storageProvider)
    }

    fun jdbc(key: String?, url: String, username: String, password: String, driver: String) {
        val config = HikariConfig().apply {
            driverClassName = driver
            this.password = password
            this.username = username
            this.jdbcUrl = url
        }
        val dataSource = HikariDataSource(config)
        if (key == null) {
            HikariCP.defaultCustom(dataSource)
            SessionImpl.defaultDataSource = { HikariCP.dataSource() }
        } else {
            HikariCP.custom(key, dataSource)
        }
    }

    fun registerS3Client(
        accessKey: String,
        secretKey: String,
        region: String,
        endpoint: String? = null,
    ) {
        AWSS3StorageProvider.register(secretKey, accessKey, region, endpoint)
    }

    fun registerEventListener(listener: AdminEventListener) {
        DynamicConfiguration.registerEventListener(listener)
    }

    internal fun closeDatabase() {
        runCatching {
            HikariCP.dataSource().close()
        }
        jdbcDataSources.forEach { HikariCP.dataSource(it).close() }
    }
}