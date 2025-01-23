package ir.amirreza

import authentication.KtorAdminPrincipal
import authentication.ktorAdmin
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureSecurity() {
    install(Authentication) {
        ktorAdmin(name = "admin") {
            validate { credentials ->
                if (credentials["username"] == "admin" && credentials["password"] == "password") {
                    KtorAdminPrincipal("username")
                } else null
            }
        }
    }
}