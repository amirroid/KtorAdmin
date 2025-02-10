package modules.uploads

import configuration.DynamicConfiguration
import csrf.CSRF_TOKEN_FIELD_NAME
import csrf.CsrfManager
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import repository.FileRepository
import utils.withAuthenticate

fun Routing.configureUploadFileRouting(authenticationName: String?) {
    withAuthenticate(authenticationName) {
        post("/admin/rich_editor/upload") {
            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null
            var csrfToken: String? = null
            var fileName: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> if (part.name == "file") {
                        fileBytes = part.provider().readRemaining().readByteArray()
                        fileName = part.originalFileName
                    }

                    is PartData.FormItem -> if (part.name == CSRF_TOKEN_FIELD_NAME) {
                        csrfToken = part.value
                    }

                    else -> Unit
                }
                part.dispose()
            }

            if (!CsrfManager.validateToken(csrfToken) || fileBytes == null || fileName == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalidate Request"))
                return@post
            }

            val uploadTarget = DynamicConfiguration.tinyMCEConfig.uploadTarget
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Upload target is not set.")
                )

            val generatedFileName = FileRepository.uploadFile(uploadTarget, fileBytes, fileName)
            if (generatedFileName == null) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Can't upload file."))
            }
            call.respond(
                mapOf(
                    "file" to FileRepository.generateMediaUrl(uploadTarget, generatedFileName.first, call),
                    "message" to "File uploaded successfully."
                )
            )
        }
    }
}