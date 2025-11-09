package ir.amirroid.ktoradmin.modules.error

import ir.amirroid.ktoradmin.error.KtorAdminErrorHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import ir.amirroid.ktoradmin.utils.tooManyRequests

internal fun Application.configureErrorHandler() {
    install(KtorAdminErrorHandler) {
        handle(HttpStatusCode.TooManyRequests) { call ->
            call.tooManyRequests()
        }
    }
}