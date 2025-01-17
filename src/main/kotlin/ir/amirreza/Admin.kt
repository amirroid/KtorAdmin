package ir.amirreza

import io.ktor.server.application.*
import plugins.KtorAdmin

fun Application.configureAdmin() {
    install(KtorAdmin)
}