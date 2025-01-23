package modules

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.configureLoginRouting() {
    get("/admin/login") {
        call.respondText { "Logging In..." }
    }
}