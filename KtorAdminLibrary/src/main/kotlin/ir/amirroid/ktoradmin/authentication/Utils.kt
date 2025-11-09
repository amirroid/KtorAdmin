package ir.amirroid.ktoradmin.authentication

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.flash.setFlashSessionsAndRedirect
import io.ktor.http.URLBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.uri
import io.ktor.server.response.respondRedirect
import ir.amirroid.ktoradmin.utils.baseUrl
import kotlin.text.orEmpty

internal suspend fun redirectToLogin(call: ApplicationCall, requestId: String?) {
    val originUrl = if (call.request.uri.startsWith("/${DynamicConfiguration.adminPath}/login")) {
        call.setFlashSessionsAndRedirect(
            requestId,
            listOf(),
            emptyMap()
        )
        URLBuilder(call.request.uri).parameters["origin"].orEmpty()
    } else {
        URLBuilder(call.baseUrl + call.request.uri).apply {
            if (parameters.contains("origin")) {
                parameters.remove("origin")
            }
        }.buildString()
    }
    call.respondRedirect("${call.baseUrl}/${DynamicConfiguration.adminPath}/login?origin=$originUrl")
}