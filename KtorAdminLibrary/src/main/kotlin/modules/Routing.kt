package modules

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.*
import modules.download.configureDownloadFilesRouting
import modules.file.handleGenerateFileUrl
import modules.uploads.configureUploadFileRouting
import panels.AdminPanel

fun Application.configureRouting(
    authenticateName: String?,
    panels: List<AdminPanel>
) {
    routing {
        staticResources("/static", "static")
        configureUploadFileRouting(authenticateName)
        configureDownloadFilesRouting(authenticateName, panels)
        authenticateName?.let {
            configureLoginRouting(it)
        }
        handleGenerateFileUrl(panels, authenticateName)
        configureGetRouting(panels, authenticateName)
        configureSavesRouting(panels, authenticateName)
    }
}