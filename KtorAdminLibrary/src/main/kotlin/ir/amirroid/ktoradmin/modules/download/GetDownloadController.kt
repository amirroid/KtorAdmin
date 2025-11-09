package ir.amirroid.ktoradmin.modules.download

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.csrf.CSRF_TOKEN_FIELD_NAME
import ir.amirroid.ktoradmin.csrf.CsrfManager
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.pdf.PdfHelper
import ir.amirroid.ktoradmin.repository.JdbcQueriesRepository
import ir.amirroid.ktoradmin.repository.MongoClientRepository
import ir.amirroid.ktoradmin.utils.Constants
import ir.amirroid.ktoradmin.utils.badRequest
import ir.amirroid.ktoradmin.utils.invalidateRequest
import ir.amirroid.ktoradmin.utils.notFound
import ir.amirroid.ktoradmin.utils.serverError
import ir.amirroid.ktoradmin.utils.withAuthenticate

fun Routing.configureDownloadFilesRouting(authenticateName: String?, panels: List<AdminPanel>) {
    withAuthenticate(authenticateName) {
        // Route for downloading table data as a CSV file
        get("/${DynamicConfiguration.adminPath}/${Constants.DOWNLOADS_PATH}/{pluralName}/csv") {
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
                        is AdminMongoCollection -> MongoClientRepository.getAllDataAsCsvFile(panel)
                        else -> "return@get"
                    }
                    val bytes = file.toByteArray()
                    call.respondBytes(contentType = ContentType.Text.CSV) { bytes }
                }
            }.onFailure {
                call.serverError(it.message.orEmpty(), it)
            }
        }

        // Route for downloading a specific record as a PDF file
        get("/${DynamicConfiguration.adminPath}/${Constants.DOWNLOADS_PATH}/{pluralName}/{primaryKey}/pdf") {
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

                val pdfData = PdfHelper.generatePdf(panel, primaryKey, call)
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