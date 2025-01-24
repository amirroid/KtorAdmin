package modules

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import panels.AdminPanel

fun Application.configureRouting(
    authenticateName: String?,
    tables: List<AdminPanel>
) {
    routing {
        staticResources("/static", "static")
        authenticateName?.let {
            configureLoginRouting(it)
        }
        configureGetRouting(tables, authenticateName)
        configureSavesRouting(tables, authenticateName)
    }
}