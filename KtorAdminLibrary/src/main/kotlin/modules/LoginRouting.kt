package modules

import authentication.USER_SESSIONS
import configuration.DynamicConfiguration
import csrf.CsrfManager
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.velocity.*
import utils.Constants

fun Routing.configureLoginRouting(authenticatedName: String) {
    get("/admin/login") {
        if (DynamicConfiguration.loginFields.isEmpty()) {
            throw IllegalStateException("Login fields are not configured.")
        }
        val origin = call.parameters["origin"] ?: "/admin"
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_login.vm", model = mutableMapOf(
                    "fields" to DynamicConfiguration.loginFields, "origin" to origin,
                    "csrfToken" to CsrfManager.generateToken()
                )
            )
        )
    }
    authenticate(authenticatedName, strategy = AuthenticationStrategy.Required) {
        post("/admin/login") {
            val origin = call.parameters["origin"] ?: "/admin"
            call.respondRedirect(origin)
        }
        post("/admin/logout") {
            call.sessions.clear(USER_SESSIONS)
            call.respond(HttpStatusCode.OK)
        }
    }
}