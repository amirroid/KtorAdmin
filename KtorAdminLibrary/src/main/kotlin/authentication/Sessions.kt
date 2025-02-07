package authentication

import configuration.DynamicConfiguration
import io.ktor.server.sessions.*
import models.forms.UserForm
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal const val USER_SESSIONS = "admin_user_sessions"

fun SessionsConfig.configureAdminCookies(maxAge: Duration? = DynamicConfiguration.authenticationSessionMaxAge) {
    cookie<String>(USER_SESSIONS) {
        cookie.path = "/"
        cookie.httpOnly = true
        cookie.maxAge = maxAge
    }
}