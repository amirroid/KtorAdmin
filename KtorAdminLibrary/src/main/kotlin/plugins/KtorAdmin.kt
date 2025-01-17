package plugins

import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import repository.AdminTableRepository

class KtorAdmin {
    class Configuration {
    }
    companion object Plugin : BaseApplicationPlugin<Application, Configuration, KtorAdmin> {
        override val key: AttributeKey<KtorAdmin>
            get() = AttributeKey("Ktor Admin")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): KtorAdmin {
            val tables = AdminTableRepository.getAll()
            val configuration = Configuration().apply(configure)
            pipeline.routing {
                get("/admin") {
                    call.respondText(
                        contentType = ContentType.parse("text/html; charset=utf-8"),
                        text = tables
                            .joinToString(prefix = "<ul>", postfix = "</ul>", separator = "\n") {
                                "<li>${it.getTableName()}<ul>${
                                    it.getAllColumns().joinToString(separator = "") {
                                        "<li>${it.columnName} = ${it.type}</li>"
                                    }
                                }</ul></li>"
                            }
                    )
                }
            }
            return KtorAdmin()
        }
    }
}