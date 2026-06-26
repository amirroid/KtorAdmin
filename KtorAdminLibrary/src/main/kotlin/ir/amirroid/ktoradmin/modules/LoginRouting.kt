package ir.amirroid.ktoradmin.modules

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import ir.amirroid.ktoradmin.authentication.USER_SESSIONS
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.csrf.CsrfManager
import ir.amirroid.ktoradmin.flash.getFlashDataAndClear
import ir.amirroid.ktoradmin.flash.getRequestId
import ir.amirroid.ktoradmin.ratelimit.withRateLimit
import ir.amirroid.ktoradmin.template.TemplateModel
import ir.amirroid.ktoradmin.translator.translator

fun Routing.configureLoginRouting(authenticatedName: String) {
    withRateLimit {
        get("/${DynamicConfiguration.adminPath}/login") {
            val requestId = call.getRequestId()
            val valuesWithErrors = call.getFlashDataAndClear(requestId)
            if (DynamicConfiguration.loginFields.isEmpty()) {
                throw IllegalStateException("Login fields are not configured.")
            }
            val origin = call.parameters["origin"] ?: "/${DynamicConfiguration.adminPath}"
            val translator = call.translator
            val model = TemplateModel(
                mutableMapOf(
                    "fields" to DynamicConfiguration.loginFields,
                    "origin" to origin,
                    "csrfToken" to CsrfManager.generateToken(),
                    "requestId" to requestId,
                    "translations" to translator.translates,
                    "layout_direction" to translator.layoutDirection,
                    "lang" to translator.languageCode,
                ).apply {
                    DynamicConfiguration.loginPageMessage?.let {
                        put("message", it)
                    }
                    if (valuesWithErrors.second != null) {
                        put("hasError", true)
                    }
                }
            )
            DynamicConfiguration.template.renderLogin(call, model)
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
