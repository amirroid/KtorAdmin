package plugins

import com.vladsch.kotlin.jdbc.HikariCP
import com.vladsch.kotlin.jdbc.SessionImpl
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.util.*
import modules.configureRouting
import modules.configureTemplating
import repository.AdminTableRepository
import repository.FileRepository

class KtorAdmin {
    class Configuration {
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

        internal fun closeDatabase() {
            runCatching {
                HikariCP.dataSource().close()
            }
            jdbcDataSources.forEach { HikariCP.dataSource(it).close() }
        }
    }

    companion object Plugin : BaseApplicationPlugin<Application, Configuration, KtorAdmin> {
        override val key: AttributeKey<KtorAdmin>
            get() = AttributeKey("Ktor Admin")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): KtorAdmin {
            val tables = AdminTableRepository.getAll()
            val configuration = Configuration().apply(configure)
            pipeline.configureTemplating()
            pipeline.configureRouting(tables)
            pipeline.monitor.subscribe(ApplicationStopPreparing) {
                configuration.closeDatabase()
            }
            return KtorAdmin()
        }
    }
}