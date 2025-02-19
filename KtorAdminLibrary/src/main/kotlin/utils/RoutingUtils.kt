package utils

import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.respondBack(pluralName: String?) {
    respondRedirect(
        "/admin/${Constants.RESOURCES_PATH}/${pluralName.orEmpty()}"
    )
}