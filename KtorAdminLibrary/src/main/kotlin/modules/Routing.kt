package modules

import annotations.errors.badRequest
import annotations.errors.notFound
import annotations.errors.serverError
import authentication.KtorAdminPrincipal
import configuration.DynamicConfiguration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import models.*
import models.ReferenceItem
import models.TableGroup
import models.toTableGroups
import repository.FileRepository
import repository.JdbcQueriesRepository
import utils.AdminTable
import utils.Constants
import utils.getAllAllowToShowColumns

fun Application.configureRouting(
    authenticateName: String?,
    tables: List<AdminTable>
) {
    routing {
        staticResources("/static", "static")
        configureLoginRouting()
        configureGetRouting(tables, authenticateName)
        configureSavesRouting(tables, authenticateName)
    }
}