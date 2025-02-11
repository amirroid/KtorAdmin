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
import utils.badRequest
import utils.invalidateRequest
import utils.notFound
import utils.withAuthenticate

fun Routing.configureDownloadFilesRouting(authenticateName: String?, panels: List<AdminPanel>) {
    withAuthenticate(authenticateName) {
        // Route for downloading table data as a CSV file
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

        // Route for downloading a specific record as a PDF file
        get("/admin/download/{pluralName}/{primaryKey}/pdf") {
            val pluralName = call.parameters["pluralName"]
            val primaryKey = call.parameters["primaryKey"] ?: return@get call.badRequest("Primary key is missing")
            val csrfToken = call.parameters[CSRF_TOKEN_FIELD_NAME]

            if (!CsrfManager.validateToken(csrfToken)) {
                return@get call.invalidateRequest()
            }

            val panel = panels.find { it.getPluralName() == pluralName }
                ?: return@get call.notFound("No table found with plural name: $pluralName")

            val pdfData = PdfHelper.generatePdf(panel, primaryKey)
                ?: return@get call.badRequest("Error generating PDF")

            call.response.header(
                HttpHeaders.ContentDisposition,
                "attachment; filename=\"output_${pluralName}_${primaryKey}.pdf\""
            )
            call.respondBytes(pdfData, ContentType.Application.Pdf)
        }
    }
}