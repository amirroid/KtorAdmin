package translator

import configuration.DynamicConfiguration
import io.ktor.server.application.ApplicationCall
import translator.locals.en.EnglishKtorAdminTranslator
import translator.locals.fa.PersianKtorAdminTranslator

internal val ApplicationCall.translator: KtorAdminTranslator
    get() = DynamicConfiguration.getTranslator(
        request.cookies["current_language"]?.toString()
    )