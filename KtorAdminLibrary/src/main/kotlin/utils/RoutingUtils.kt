package utils

import configuration.DynamicConfiguration
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.respondBack(pluralName: String?) {
    respondRedirect(
        "/${DynamicConfiguration.adminPath}/${Constants.RESOURCES_PATH}/${pluralName.orEmpty()}"
    )
}