package modules.file

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import repository.FileRepository
import utils.badRequest
import utils.withAuthenticate

fun Routing.handleGenerateFileUrl(panels: List<AdminPanel>, authenticateName: String?) {
    withAuthenticate(authenticateName) {
        post("/admin/file_handler/generate/") {
            val parameters = call.receiveParameters()
            val fileName = parameters["fileName"]
            val field = parameters["field"]?.split(".")
            if (fileName == null) {
                call.respond(message = mapOf("error" to "File name is required"), status = HttpStatusCode.BadRequest)
                return@post
            }
            if (field == null || field.count() != 2) {
                call.respond(message = mapOf("error" to "Field is not valid"), status = HttpStatusCode.BadRequest)
                return@post
            }
            val (pluralName, itemName) = field
            val itemUploadTarget = panels.firstOrNull {
                it.getPluralName() == pluralName
            }?.let { panel ->
                when (panel) {
                    is AdminJdbcTable -> panel.getAllColumns().firstOrNull {
                        it.columnName == itemName
                    }?.uploadTarget

                    is AdminMongoCollection -> panel.getAllFields().firstOrNull {
                        it.fieldName == itemName
                    }?.uploadTarget

                    else -> null
                }
            }
            if (itemUploadTarget == null) {
                call.respond(message = mapOf("error" to "Field does not exist"), status = HttpStatusCode.BadRequest)
                return@post
            }
            FileRepository.generateMediaUrl(
                fileName = fileName,
                uploadTarget = itemUploadTarget,
                call = call
            ).let {
                call.respond(
                    mapOf("url" to it)
                )
            }
        }
    }
}