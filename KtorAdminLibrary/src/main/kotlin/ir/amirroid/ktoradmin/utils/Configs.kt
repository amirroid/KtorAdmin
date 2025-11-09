package ir.amirroid.ktoradmin.utils

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import ir.amirroid.ktoradmin.repository.FileRepository


internal val ApplicationCall.baseUrl: String
    get() = "${request.origin.scheme}://${request.origin.serverHost}:${request.origin.serverPort}"

internal val ApplicationCall.mediaUrl: String
    get() = "$baseUrl/${FileRepository.mediaRoot}"
