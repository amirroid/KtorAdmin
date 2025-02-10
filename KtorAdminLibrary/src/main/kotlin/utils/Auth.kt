package utils

import io.ktor.server.auth.*
import io.ktor.server.routing.*
import rate_limiting.withRateLimit

fun Routing.withAuthenticate(name: String?, build: Route.() -> Unit) {
    withRateLimit {
        if (name != null) {
            authenticate(name, strategy = AuthenticationStrategy.Required, build = build)
        } else {
            build()
        }
    }
}