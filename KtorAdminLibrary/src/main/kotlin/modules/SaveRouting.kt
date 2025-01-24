package modules

import io.ktor.server.routing.*
import modules.add.handleAddRequest
import modules.update.handleUpdateRequest
import panels.AdminPanel
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