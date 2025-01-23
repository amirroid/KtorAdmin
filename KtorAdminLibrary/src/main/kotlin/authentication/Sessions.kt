package authentication

import io.ktor.server.sessions.*
import models.forms.UserForm
import kotlin.time.Duration

fun SessionsConfig.configureAdminCookies(maxAge: Duration? = null) {
    cookie<UserForm>("user_session") {
        cookie.path = "/"
        cookie.httpOnly = true
        cookie.maxAge = maxAge
    }
}