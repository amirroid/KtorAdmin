package modules.error

import error.KtorAdminErrorHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import utils.tooManyRequests

internal fun Application.configureErrorHandler() {
    install(KtorAdminErrorHandler) {
        handle(HttpStatusCode.TooManyRequests) { call ->
            call.tooManyRequests()
        }
    }
}