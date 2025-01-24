package utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.badRequest(message: String) {
    respondText(status = HttpStatusCode.BadRequest) {
        message
    }
}

suspend fun ApplicationCall.serverError(message: String) {
    respondText(status = HttpStatusCode.InternalServerError) {
        message
    }
}

suspend fun ApplicationCall.notFound(message: String) {
    respondText(status = HttpStatusCode.NotFound) {
        message
    }
}