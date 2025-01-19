package plugins

import configuration.KtorAdminConfiguration
import io.ktor.server.application.*
import io.ktor.util.*
import modules.configureRouting
import modules.configureTemplating
import repository.AdminTableRepository

class KtorAdmin {
    companion object Plugin : BaseApplicationPlugin<Application, KtorAdminConfiguration, KtorAdmin> {
        override val key: AttributeKey<KtorAdmin>
            get() = AttributeKey("Ktor Admin")

        override fun install(pipeline: Application, configure: KtorAdminConfiguration.() -> Unit): KtorAdmin {
            val tables = AdminTableRepository.getAll()
            val configuration = KtorAdminConfiguration().apply(configure)
            pipeline.configureTemplating()
            pipeline.configureRouting(tables)
            pipeline.monitor.subscribe(ApplicationStopPreparing) {
                configuration.closeDatabase()
            }
            return KtorAdmin()
        }
    }
}