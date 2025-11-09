package ir.amirroid.ktoradmin.modules.file

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.repository.FileRepository
import ir.amirroid.ktoradmin.utils.notFound
import ir.amirroid.ktoradmin.utils.withAuthenticate

fun Routing.handleGenerateFileUrl(panels: List<AdminPanel>, authenticateName: String?) {
    withAuthenticate(authenticateName) {
        post("/${DynamicConfiguration.adminPath}/file_handler/generate/") {
            val parameters = call.receiveParameters()
            val fileName = parameters["fileName"]
            val field = parameters["field"]?.split(".")
            if (fileName == null) {
                call.respond(
                    message = mapOf("ir/amirroid/ktoradmin/errorirroid/ktoradmin/error" to "File name is required"),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (field == null || field.count() != 2) {
                call.respond(message = mapOf("ir/amirroid/ktoradmin/errorirroid/ktoradmin/error" to "Field is not valid"), status = HttpStatusCode.BadRequest)
                return@post
            }
            val (pluralName, itemName) = field
            val itemUploadTarget = panels.firstOrNull {
                it.getPluralName() == pluralName
            }?.let { panel ->
                if (panel.isShowInAdminPanel().not()) {
                    return@post call.notFound("No table found with plural name: $pluralName")
                }
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
                call.respond(message = mapOf("ir/amirroid/ktoradmin/errorirroid/ktoradmin/error" to "Field does not exist"), status = HttpStatusCode.BadRequest)
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