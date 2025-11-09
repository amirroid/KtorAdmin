package ir.amirroid.ktoradmin.translator

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import io.ktor.server.application.ApplicationCall

internal val ApplicationCall.translator: KtorAdminTranslator
    get() = DynamicConfiguration.getTranslator(
        request.cookies["current_language"]?.toString()
    )