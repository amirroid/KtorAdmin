package modules

import com.vladsch.kotlin.jdbc.Session
import configuration.DynamicConfiguration
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.velocity.*
import models.forms.toUserForm
import utils.Constants
import utils.withAuthenticate

fun Routing.configureLoginRouting(authenticatedName: String) {
    get("/admin/login") {
        if (DynamicConfiguration.loginFields.isEmpty()) {
            throw IllegalStateException("Login fields are not configured.")
        }
        val origin = call.parameters["origin"] ?: "/admin"
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/login_admin.vm", model = mutableMapOf(
                    "fields" to DynamicConfiguration.loginFields, "origin" to origin
                )
            )
        )
    }
    authenticate(authenticatedName, strategy = AuthenticationStrategy.Required) {
        post("/admin/login") {
            val origin = call.parameters["origin"] ?: "/admin"
            call.respondRedirect(origin)
        }
    }
}