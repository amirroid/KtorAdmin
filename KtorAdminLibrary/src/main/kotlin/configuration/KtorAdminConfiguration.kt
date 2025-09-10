package configuration

import action.CustomAdminAction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import csrf.CsrfManager
import dashboard.KtorAdminDashboard
import hikra.KtorAdminHikariCP
import listener.AdminEventListener
import mapper.KtorAdminValueMapper
import models.forms.LoginFiled
import models.menu.Menu
import mongo.MongoCredential
import mongo.MongoServerAddress
import preview.KtorAdminPreview
import providers.AWSS3StorageProvider
import providers.StorageProvider
import repository.FileRepository
import repository.MongoClientRepository
import tiny.TinyMCEConfig
import translator.KtorAdminTranslator
import java.time.Duration
import java.time.ZoneId
import java.util.TimeZone

/**
 * Configuration class for KtorAdmin.
 *
 * Manages database connections, authentication, storage providers, CSRF settings,
 * and other configurable options.
 */
class KtorAdminConfiguration {

    // List to store registered JDBC data sources
    private val jdbcDataSources = mutableListOf<String>()

    /**
     * Media file storage path.
     */
    var mediaPath: String?
        get() = FileRepository.defaultPath
        set(value) {
            FileRepository.defaultPath = value
        }

    /**
     * Root directory for media files.
     */
    var mediaRoot: String?
        get() = FileRepository.mediaRoot
        set(value) {
            FileRepository.mediaRoot = value
        }

    /**
     * Default AWS S3 bucket name.
     */
    var defaultAwsS3Bucket: String?
        get() = AWSS3StorageProvider.defaultBucket
        set(value) {
            AWSS3StorageProvider.defaultBucket = value
        }

    /**
     * Rate limit per minute for API requests.
     */
    var rateLimitPerMinutes: Int
        get() = DynamicConfiguration.rateLimitPerMinutes
        set(value) {
            DynamicConfiguration.rateLimitPerMinutes = value
        }

    /**
     * Authentication name (if required).
     */
    var authenticateName: String? = null
        set(value) {
            DynamicConfiguration.authenticateName = value
            field = value
        }

    var loginPageMessage: String?
        get() = DynamicConfiguration.loginPageMessage
        set(value) {
            DynamicConfiguration.loginPageMessage = value
        }


    /**
     * If debugMode is enabled, error messages will be displayed.
     * Otherwise, no message will be shown for better security.
     */
    var debugMode: Boolean
        get() = DynamicConfiguration.debugMode
        set(value) {
            DynamicConfiguration.debugMode = value
        }

    /**
     * AWS S3 pre-signed URL expiration duration.
     */
    var s3SignatureDuration: Duration?
        get() = AWSS3StorageProvider.signatureDuration
        set(value) {
            AWSS3StorageProvider.signatureDuration = value
        }

    /**
     * Determines if data can be downloaded as CSV.
     */
    var canDownloadDataAsCsv: Boolean
        get() = DynamicConfiguration.canDownloadDataAsCsv
        set(value) {
            DynamicConfiguration.canDownloadDataAsCsv = value
        }

    /**
     * Determines if data can be downloaded as PDF.
     */
    var canDownloadDataAsPdf: Boolean
        get() = DynamicConfiguration.canDownloadDataAsPdf
        set(value) {
            DynamicConfiguration.canDownloadDataAsPdf = value
        }

    /**
     * Maximum items per page in pagination.
     */
    var maxItemsInPage: Int
        get() = DynamicConfiguration.maxItemsInPage
        set(value) {
            DynamicConfiguration.maxItemsInPage = value
        }

    /**
     * TinyMCE editor configuration.
     */
    var tinyMCEConfig: TinyMCEConfig
        get() = DynamicConfiguration.tinyMCEConfig
        set(value) {
            DynamicConfiguration.tinyMCEConfig = value
        }

    /**
     * Maximum session duration for authentication.
     */
    var authenticationSessionMaxAge: kotlin.time.Duration
        get() = DynamicConfiguration.authenticationSessionMaxAge
        set(value) {
            DynamicConfiguration.authenticationSessionMaxAge = value
        }

    /**
     * CSRF token expiration time.
     */
    var csrfTokenExpirationTime: Long
        get() = CsrfManager.tokenExpirationTime
        set(value) {
            CsrfManager.tokenExpirationTime = value
        }

    /**
     * List of login fields for authentication.
     */
    var loginFields: List<LoginFiled>
        get() = DynamicConfiguration.loginFields
        set(value) {
            DynamicConfiguration.loginFields = value
        }

    /**
     * Custom admin dashboard configuration.
     */
    var adminDashboard: KtorAdminDashboard?
        get() = DynamicConfiguration.dashboard
        set(value) {
            DynamicConfiguration.dashboard = value
        }


    var defaultLanguage: String
        get() = DynamicConfiguration.defaultLanguage
        set(value) {
            DynamicConfiguration.defaultLanguage = value
        }


    var zoneId: ZoneId
        get() = DynamicConfiguration.timeZone
        set(value) {
            DynamicConfiguration.timeZone = value
        }


    var adminPath: String
        get() = DynamicConfiguration.adminPath
        set(value) {
            DynamicConfiguration.adminPath = value
        }

    /**
     * Registers a custom admin action.
     */
    fun registerCustomAdminAction(action: CustomAdminAction) {
        DynamicConfiguration.registerCustomAdminAction(action)
    }

    /**
     * Registers a custom admin action for all users.
     */
    fun registerCustomAdminActionForAll(action: CustomAdminAction) {
        DynamicConfiguration.registerCustomAdminActionForAll(action)
    }

    /**
     * Registers a storage provider for file management.
     */
    fun registerStorageProvider(storageProvider: StorageProvider) {
        FileRepository.registerStorageProvider(storageProvider)
    }

    /**
     * Registers a new MongoDB client.
     */
    fun mongo(
        key: String?,
        databaseName: String,
        address: MongoServerAddress,
        credential: MongoCredential? = null
    ) {
        MongoClientRepository.registerNewClient(key, databaseName, address, credential)
    }

    /**
     * Registers a new JDBC data source.
     */
    fun jdbc(
        key: String?,
        url: String,
        username: String,
        password: String,
        driver: String,
    ) {
        val config = HikariConfig().apply {
            driverClassName = driver
            this.password = password
            this.username = username
            this.jdbcUrl = url
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        val dataSource = HikariDataSource(config)

        if (key == null) {
            KtorAdminHikariCP.defaultCustom(dataSource)
        } else {
            KtorAdminHikariCP.custom(key, dataSource)
            jdbcDataSources.add(key)
        }
    }

    /**
     * Registers a new JDBC data source.
     */
    fun jdbc(
        key: String?,
        dataSource: HikariDataSource
    ) {
        if (key == null) {
            KtorAdminHikariCP.defaultCustom(dataSource)
        } else {
            KtorAdminHikariCP.custom(key, dataSource)
            jdbcDataSources.add(key)
        }
    }

    /**
     * Registers an AWS S3 client.
     */
    fun registerS3Client(
        accessKey: String,
        secretKey: String,
        region: String,
        endpoint: String? = null,
    ) {
        AWSS3StorageProvider.register(secretKey, accessKey, region, endpoint)
    }

    fun registerValueMapper(valueMapper: KtorAdminValueMapper) =
        DynamicConfiguration.registerValueMapper(valueMapper)

    /**
     * Registers an event listener for admin events.
     */
    fun registerEventListener(listener: AdminEventListener) {
        DynamicConfiguration.currentEventListener = listener
    }

    /**
     * Registers a new preview.
     */
    fun registerPreview(preview: KtorAdminPreview) {
        DynamicConfiguration.registerPreview(preview)
    }

    /**
     * Registers a new translator.
     */
    fun registerTranslator(translator: KtorAdminTranslator) {
        DynamicConfiguration.registerTranslator(translator)
    }


    fun provideMenu(provide: (tableName: String?) -> List<Menu>) {
        DynamicConfiguration.menuProvider = provide
    }

    /**
     * Closes all active database connections.
     */
    internal fun closeDatabase() {
        KtorAdminHikariCP.closeAllConnections()
        MongoClientRepository.closeAllConnections()
    }
}