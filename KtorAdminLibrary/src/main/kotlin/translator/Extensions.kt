package translator

import io.ktor.server.application.ApplicationCall
import translator.locals.en.EnglishKtorAdminTranslator

val ApplicationCall.translator: KtorAdminTranslator
    get() = EnglishKtorAdminTranslator