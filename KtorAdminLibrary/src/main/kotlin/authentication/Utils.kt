package authentication

import io.ktor.http.URLBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.uri
import io.ktor.server.response.respondRedirect
import utils.baseUrl
import kotlin.text.orEmpty

internal suspend fun redirectToLogin(call: ApplicationCall) {
    val originUrl = if (call.request.uri.startsWith("/admin/login")) {
        URLBuilder(call.request.uri).parameters["origin"].orEmpty()
    } else {
        URLBuilder(call.baseUrl + call.request.uri).apply {
            if (parameters.contains("origin")) {
                parameters.remove("origin")
            }
        }.buildString()
    }
    call.respondRedirect("${call.baseUrl}/admin/login?origin=$originUrl")
}