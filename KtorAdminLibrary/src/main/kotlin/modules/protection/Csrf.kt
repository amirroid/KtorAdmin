package modules.protection

import csrf.CsrfPlugin
import io.ktor.server.application.*

internal fun Application.configureCsrf() {
    install(CsrfPlugin)
}