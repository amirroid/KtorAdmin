package ir.amirroid.ktoradmin.modules

import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import io.ktor.server.routing.*
import ir.amirroid.ktoradmin.modules.actions.handleActions
import ir.amirroid.ktoradmin.modules.add.handleAddRequest
import ir.amirroid.ktoradmin.modules.confirmation.handleSaveConfirmation
import ir.amirroid.ktoradmin.modules.update.handleUpdateRequest
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.utils.Constants
import ir.amirroid.ktoradmin.utils.withAuthenticate

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