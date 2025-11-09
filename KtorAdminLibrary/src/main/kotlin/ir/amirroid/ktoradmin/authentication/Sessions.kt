package ir.amirroid.ktoradmin.authentication

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import io.ktor.server.sessions.*
import kotlin.time.Duration

internal const val USER_SESSIONS = "admin_user_sessions"

fun SessionsConfig.configureAdminCookies(maxAge: Duration? = DynamicConfiguration.authenticationSessionMaxAge) {
    cookie<String>(USER_SESSIONS) {
        cookie.path = "/"
        cookie.httpOnly = true
        cookie.maxAge = maxAge
    }
}