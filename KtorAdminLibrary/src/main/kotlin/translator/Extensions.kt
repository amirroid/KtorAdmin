package translator

import io.ktor.server.application.ApplicationCall
import translator.locals.en.EnglishKtorAdminTranslator
import translator.locals.fa.PersianKtorAdminTranslator

internal val ApplicationCall.translator: KtorAdminTranslator
    get() = PersianKtorAdminTranslator