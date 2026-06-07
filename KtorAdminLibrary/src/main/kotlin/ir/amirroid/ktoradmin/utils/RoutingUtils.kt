package ir.amirroid.ktoradmin.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondRedirect
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration

suspend fun ApplicationCall.respondBack(pluralName: String?) {
    respondRedirect(
        "/${DynamicConfiguration.adminPath}/${Constants.RESOURCES_PATH}/${pluralName.orEmpty()}",
    )
}
