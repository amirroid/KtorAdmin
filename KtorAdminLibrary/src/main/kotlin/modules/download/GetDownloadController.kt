package modules.download

import configuration.DynamicConfiguration
import csrf.CSRF_TOKEN_FIELD_NAME
import csrf.CsrfManager
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import panels.AdminJdbcTable
import panels.AdminPanel
import repository.JdbcQueriesRepository
import utils.badRequest
import utils.invalidateRequest
import utils.notFound
import utils.withAuthenticate
import kotlin.collections.find

fun Routing.configureDownloadFilesRouting(authenticateName: String?, panels: List<AdminPanel>) {
    withAuthenticate(authenticateName) {
        get("/admin/download/{pluralName}/csv") {
            if (DynamicConfiguration.canDownloadDataAsCsv.not()) {
                return@get call.badRequest("To use this feature, please enable this option in the configuration.")
            }
            val pluralName = call.parameters["pluralName"]
            val csrfToken = call.parameters[CSRF_TOKEN_FIELD_NAME]
            if (CsrfManager.validateToken(csrfToken).not()) {
                return@get call.invalidateRequest()
            }
            val panel = panels.find { it.getPluralName() == pluralName }
            if (panel == null) {
                call.notFound("No table found with plural name: $pluralName")
            } else {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"${pluralName}_data.csv\""
                )
                val file = when (panel) {
                    is AdminJdbcTable -> JdbcQueriesRepository.getAllDataAsCsvFile(panel)
                    else -> return@get
                }
                call.respondBytes(contentType = ContentType.Text.CSV) { file.toByteArray() }
            }
        }
    }
}