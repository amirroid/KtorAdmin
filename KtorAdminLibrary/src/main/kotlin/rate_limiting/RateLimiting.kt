package rate_limiting

import authentication.KtorAdminPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import utils.badRequest
import utils.tooManyRequests
import kotlin.time.Duration.Companion.minutes

internal const val ADMIN_RATE_LIMITING_NAME = "ktor_admin_rate_limiting"

fun RateLimitConfig.configureKtorAdminRateLimit() {
    register(RateLimitName(ADMIN_RATE_LIMITING_NAME)) {
        rateLimiter(
            2, 1.minutes
        )
        requestKey {
            it.principal<KtorAdminPrincipal>()?.name ?: "guest"
        }
    }
}


internal fun Application.configureRateLimit() {
    if (pluginOrNull(RateLimit) == null) {
        install(RateLimit) {
            configureKtorAdminRateLimit()
        }
    }
}

internal fun Routing.withRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(ADMIN_RATE_LIMITING_NAME), build = build)
}