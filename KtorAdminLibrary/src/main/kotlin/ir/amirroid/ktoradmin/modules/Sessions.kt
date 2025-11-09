package ir.amirroid.ktoradmin.modules

import ir.amirroid.ktoradmin.authentication.configureAdminCookies
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlin.time.Duration.Companion.days


fun Application.configureSessions() {
    if (pluginOrNull(Sessions) == null) {
        log.info("Creating admin user cookies")
        install(Sessions) {
            configureAdminCookies(15.days)
        }
    }
}