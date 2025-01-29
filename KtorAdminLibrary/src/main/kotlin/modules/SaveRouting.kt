package modules

import io.ktor.server.routing.*
import modules.actions.handleActions
import modules.add.handleAddRequest
import modules.update.handleUpdateRequest
import panels.AdminPanel
import utils.*

internal fun Routing.configureSavesRouting(panels: List<AdminPanel>, authenticateName: String? = null) {
    withAuthenticate(authenticateName) {
        route("/admin/") {
            post("{pluralName}/add") {
                handleAddRequest(panels)
            }

            post("{pluralName}/{primaryKey}") {
                handleUpdateRequest(panels)
            }
            post("{pluralName}/action/{actionName}") {
                handleActions(panels)
            }
        }
    }
}