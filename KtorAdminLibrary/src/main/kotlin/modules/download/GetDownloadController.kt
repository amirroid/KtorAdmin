package modules.download

import com.itextpdf.layout.element.*
import configuration.DynamicConfiguration
import csrf.CSRF_TOKEN_FIELD_NAME
import csrf.CsrfManager
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import panels.AdminJdbcTable
import panels.AdminPanel
import pdf.PdfHelper
import repository.JdbcQueriesRepository
import utils.Constants
import utils.badRequest
import utils.invalidateRequest
import utils.notFound
import utils.serverError
import utils.withAuthenticate

fun Routing.configureDownloadFilesRouting(authenticateName: String?, panels: List<AdminPanel>) {
    withAuthenticate(authenticateName) {
        // Route for downloading table data as a CSV file
        get("/admin/${Constants.DOWNLOADS_PATH}/{pluralName}/csv") {
            runCatching {
                if (DynamicConfiguration.canDownloadDataAsCsv.not()) {
                    return@get call.badRequest("To use this feature, please enable this option in the configuration.")
                }
                val pluralName = call.parameters["pluralName"]
                val csrfToken = call.parameters[CSRF_TOKEN_FIELD_NAME]
                if (CsrfManager.validateToken(csrfToken).not()) {
                    return@get call.invalidateRequest()
                }
                val panel = panels.find { it.getPluralName() == pluralName }
                if (panel == null || panel.isShowInAdminPanel().not()) {
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
                    val bytes = file.toByteArray()
                    call.respondBytes(contentType = ContentType.Text.CSV) { bytes }
                }
            }.onFailure {
                call.serverError(it.message.orEmpty(), it)
            }
        }

        // Route for downloading a specific record as a PDF file
        get("/admin/${Constants.DOWNLOADS_PATH}/{pluralName}/{primaryKey}/pdf") {
            runCatching {
                if (DynamicConfiguration.canDownloadDataAsPdf.not()) {
                    return@get call.badRequest("To use this feature, please enable this option in the configuration.")
                }
                val pluralName = call.parameters["pluralName"]
                val primaryKey = call.parameters["primaryKey"] ?: return@get call.badRequest("Primary key is missing")
                val csrfToken = call.parameters[CSRF_TOKEN_FIELD_NAME]

                if (!CsrfManager.validateToken(csrfToken)) {
                    return@get call.invalidateRequest()
                }

                val panel = panels.find { it.getPluralName() == pluralName }?.takeIf { it.isShowInAdminPanel() }
                    ?: return@get call.notFound("No table found with plural name: $pluralName")

                val pdfData = PdfHelper.generatePdf(panel, primaryKey)
                    ?: return@get call.badRequest("Error generating PDF")

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"output_${pluralName}_${primaryKey}.pdf\""
                )
                call.respondBytes(pdfData, ContentType.Application.Pdf)
            }.onFailure {
                call.serverError(it.message.orEmpty(), it)
            }
        }
    }
}