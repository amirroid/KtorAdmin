package ir.amirroid.ktoradmin.translator

import io.ktor.server.application.ApplicationCall
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration

internal val ApplicationCall.translator: KtorAdminTranslator
    get() =
        DynamicConfiguration.getTranslator(
            request.cookies["current_language"],
        )
