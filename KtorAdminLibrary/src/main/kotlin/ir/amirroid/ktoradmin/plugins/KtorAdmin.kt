package ir.amirroid.ktoradmin.plugins

import ir.amirroid.ktoradmin.configuration.KtorAdminConfiguration
import io.ktor.server.application.*
import io.ktor.util.*
import ir.amirroid.ktoradmin.modules.configureRouting
import ir.amirroid.ktoradmin.modules.configureSessions
import ir.amirroid.ktoradmin.modules.configureTemplating
import ir.amirroid.ktoradmin.modules.error.configureErrorHandler
import ir.amirroid.ktoradmin.rate_limiting.configureRateLimit
import ir.amirroid.ktoradmin.repository.AdminTableRepository

class KtorAdmin {
    companion object Plugin : BaseApplicationPlugin<Application, KtorAdminConfiguration, KtorAdmin> {
        override val key: AttributeKey<KtorAdmin>
            get() = AttributeKey("Ktor Admin")

        override fun install(pipeline: Application, configure: KtorAdminConfiguration.() -> Unit): KtorAdmin {
            val tables = AdminTableRepository.getAll()
            val configuration = KtorAdminConfiguration().apply(configure)
            val authenticateName = configuration.authenticateName
            pipeline.configureTemplating()
            pipeline.configureRateLimit()
            pipeline.configureErrorHandler()
            pipeline.configureSessions()
            pipeline.configureRouting(authenticateName, tables)
            pipeline.monitor.subscribe(ApplicationStopping) { configuration.closeDatabase() }
            return KtorAdmin()
        }
    }
}