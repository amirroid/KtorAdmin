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
import rate_limiting.withRateLimit
import utils.Constants

fun Routing.configureLoginRouting(authenticatedName: String) {
    withRateLimit {
        get("/${DynamicConfiguration.adminPath}/login") {
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
    }
    authenticate(authenticatedName, strategy = AuthenticationStrategy.Required) {
        post("/${DynamicConfiguration.adminPath}/login") {
            val origin = call.parameters["origin"] ?: "/${DynamicConfiguration.adminPath}"
            call.respondRedirect(origin)
        }
        post("/${DynamicConfiguration.adminPath}/logout") {
            call.sessions.clear(USER_SESSIONS)
            call.respond(HttpStatusCode.OK)
        }
    }
}