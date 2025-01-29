package authentication

import io.ktor.server.sessions.*
import models.forms.UserForm
import kotlin.time.Duration

internal const val USER_SESSIONS = "user_sessions"

fun SessionsConfig.configureAdminCookies(maxAge: Duration? = null) {
    cookie<String>(USER_SESSIONS) {
        cookie.path = "/"
        cookie.httpOnly = true
        cookie.maxAge = maxAge
    }
}