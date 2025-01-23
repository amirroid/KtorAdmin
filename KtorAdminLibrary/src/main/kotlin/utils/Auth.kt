package utils

import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Routing.withAuthenticate(name: String?, build: Route.() -> Unit) {
    if (name != null) {
        authenticate(name, strategy = AuthenticationStrategy.Required, build = build)
    } else {
        build()
    }
}