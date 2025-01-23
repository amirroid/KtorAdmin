package modules

import annotations.errors.badRequest
import annotations.errors.notFound
import annotations.errors.serverError
import configuration.DynamicConfiguration
import getters.toTypedValue
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import models.events.ColumnEvent
import models.ColumnSet
import models.ColumnType
import models.events.FileEvent
import modules.add.handleAddRequest
import modules.update.handleUpdateRequest
import repository.JdbcQueriesRepository
import repository.FileRepository
import tables.AdminJdbcTable
import tables.AdminPanel
import tables.findWithPluralName
import tables.getAllAllowToShowColumns
import utils.*

internal fun Routing.configureSavesRouting(tables: List<AdminPanel>, authenticateName: String? = null) {
    withAuthenticate(authenticateName) {
        route("/admin/") {
            post("{pluralName}/add") {
                handleAddRequest(tables)
            }

            post("{pluralName}/{primaryKey}") {
                handleUpdateRequest(tables)
            }
        }
    }
}