package modules

import authentication.configureAdminCookies
import com.vladsch.kotlin.jdbc.Session
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import models.forms.UserForm
import kotlin.time.Duration.Companion.days


fun Application.configureSessions() {
    if (pluginOrNull(Sessions) == null) {
        log.info("Creating admin user cookies")
        install(Sessions) {
            configureAdminCookies(15.days)
        }
    }
}