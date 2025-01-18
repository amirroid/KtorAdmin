package plugins

import com.vladsch.kotlin.jdbc.HikariCP
import com.vladsch.kotlin.jdbc.SessionImpl
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.util.*
import models.JDBCDrivers
import modules.configureRouting
import modules.configureTemplating
import repository.AdminTableRepository

class KtorAdmin {
    class Configuration {
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
    }

    companion object Plugin : BaseApplicationPlugin<Application, Configuration, KtorAdmin> {
        override val key: AttributeKey<KtorAdmin>
            get() = AttributeKey("Ktor Admin")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): KtorAdmin {
            val tables = AdminTableRepository.getAll()
            val configuration = Configuration().apply(configure)
            pipeline.configureTemplating()
            pipeline.configureRouting(tables)
            return KtorAdmin()
        }
    }
}