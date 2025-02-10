package plugins

import configuration.KtorAdminConfiguration
import io.ktor.server.application.*
import io.ktor.util.*
import modules.configureRouting
import modules.configureSessions
import modules.configureTemplating
import modules.error.configureErrorHandler
import rate_limiting.configureRateLimit
import repository.AdminTableRepository

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