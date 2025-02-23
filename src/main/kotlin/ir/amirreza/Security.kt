package ir.amirreza

import authentication.KtorAdminPrincipal
import authentication.configureAdminCookies
import authentication.ktorAdminFormAuth
import authentication.ktorAdminTokenAuth
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.Sessions
import kotlin.math.max
import kotlin.time.Duration.Companion.days

fun Application.configureSecurity() {
    install(Authentication) {
        ktorAdminFormAuth(name = "admin") {
            validate { credentials ->
                if (credentials["username"] == "admin" && credentials["password"] == "password") {
                    KtorAdminPrincipal("Amirreza", roles = listOf("admin"))
                } else null
            }
        }
//        ktorAdminTokenAuth(name = "admin") {
//            validateToken { token ->
//                if (token == "1234") {
//                    KtorAdminPrincipal("Amirreza", roles = listOf("admin"))
//                } else null
//            }
//            validateForm { credentials ->
//                if (credentials["username"] == "admin" && credentials["password"] == "password") {
//                    "1234"
//                } else null
//            }
//        }
    }
}