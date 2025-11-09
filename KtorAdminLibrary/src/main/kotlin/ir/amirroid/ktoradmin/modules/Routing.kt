package ir.amirroid.ktoradmin.modules

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import ir.amirroid.ktoradmin.modules.download.configureDownloadFilesRouting
import ir.amirroid.ktoradmin.modules.file.handleGenerateFileUrl
import ir.amirroid.ktoradmin.modules.uploads.configureUploadFileRouting
import ir.amirroid.ktoradmin.panels.AdminPanel

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