package ir.amirreza

import authentication.KtorAdminPrincipal
import authentication.ktorAdminFormAuth
import authentication.ktorAdminTokenAuth
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureSecurity() {
    install(Authentication) {
        ktorAdminFormAuth(name = "admin") {
            validate { credentials ->
                if (credentials["username"] == "admin" && credentials["password"] == "password") {
                    KtorAdminPrincipal("Amirreza", roles = listOf("admin"))
                } else null
            }
        }
//        ktorAdminTokenAuth("admin") {
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