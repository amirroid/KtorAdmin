package modules

import authentication.KtorAdminPrincipal
import io.ktor.server.application.*
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import models.PanelGroup
import models.toTableGroups
import modules.add.handleAddNewItem
import modules.list.handlePanelList
import modules.update.handleEditItem
import panels.AdminPanel
import utils.Constants
import utils.withAuthenticate


internal fun Routing.configureGetRouting(tables: List<AdminPanel>, authenticateName: String? = null) {
    val panelGroups = tables.toTableGroups()
    withAuthenticate(authenticateName) {
        route("/admin") {
            get {
                call.renderAdminPanel(panelGroups)
            }
            route("/{pluralName}") {
                get {
                    call.handlePanelList(tables, panelGroups)
                }
                get("add") {
                    call.handleAddNewItem(tables, panelGroups)
                }
                get("/{primaryKey}") {
                    call.handleEditItem(tables, panelGroups)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.renderAdminPanel(panelGroups: List<PanelGroup>) {
    respond(
        VelocityContent(
            "${Constants.TEMPLATES_PREFIX_PATH}/admin_dashboard.vm",
            model = mutableMapOf(
                "panelGroups" to panelGroups,
                "username" to principal<KtorAdminPrincipal>()!!.name
            )
        )
    )
}
