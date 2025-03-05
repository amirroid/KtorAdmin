package modules

import configuration.DynamicConfiguration
import io.ktor.server.routing.*
import modules.actions.handleActions
import modules.add.handleAddRequest
import modules.confirmation.handleSaveConfirmation
import modules.update.handleUpdateRequest
import panels.AdminPanel
import utils.*

internal fun Routing.configureSavesRouting(panels: List<AdminPanel>, authenticateName: String? = null) {
    withAuthenticate(authenticateName) {
        route("/${DynamicConfiguration.adminPath}/") {
            post("${Constants.RESOURCES_PATH}/{pluralName}/add") {
                handleAddRequest(panels)
            }
            post("${Constants.RESOURCES_PATH}/{pluralName}/{primaryKey}") {
                handleUpdateRequest(panels)
            }
            post("${Constants.RESOURCES_PATH}/{pluralName}/{primaryKey}/{field}") {
                handleSaveConfirmation(panels)
            }
            post("${Constants.ACTIONS_PATH}/{pluralName}/{actionName}") {
                handleActions(panels)
            }
        }
    }
}