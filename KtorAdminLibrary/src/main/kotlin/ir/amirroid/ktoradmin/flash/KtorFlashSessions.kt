package ir.amirroid.ktoradmin.flash

import io.ktor.server.sessions.*
import kotlin.time.Duration


internal const val FLASH_ERROR_SESSIONS = "KtorAdminErrorFlashSessions"
internal const val FLASH_FORM_SESSIONS = "KtorAdminFormFlashSessions"

fun SessionsConfig.configureAdminFlashes(maxAge: Duration? = null) {
}