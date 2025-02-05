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
import panels.AdminJdbcTable
import panels.AdminPanel
import repository.JdbcQueriesRepository
import utils.Constants
import utils.serverError
import utils.withAuthenticate


internal fun Routing.configureGetRouting(panels: List<AdminPanel>, authenticateName: String? = null) {
    val panelGroups = panels.toTableGroups()
    withAuthenticate(authenticateName) {
        route("/admin") {
            get {
                call.renderAdminPanel(panelGroups, panels)
            }
            route("/{pluralName}") {
                get {
                    call.handlePanelList(panels, panelGroups)
                }
                get("add") {
                    call.handleAddNewItem(panels, panelGroups)
                }
                get("/{primaryKey}") {
                    call.handleEditItem(panels, panelGroups)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.renderAdminPanel(panelGroups: List<PanelGroup>, panels: List<AdminPanel>) {
    runCatching {
        val chartData = panels.mapNotNull { panel ->
            when (panel) {
                is AdminJdbcTable -> JdbcQueriesRepository.getChartData(panel).takeIf { it.isNotEmpty() }
                else -> null
            }
        }.flatten()
        respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_dashboard.vm",
                model = mutableMapOf(
                    "panelGroups" to panelGroups,
                    "username" to principal<KtorAdminPrincipal>()!!.name,
                    "chartData" to chartData
                )
            )
        )
    }.onFailure {
        serverError(it.message.orEmpty(), it)
    }
}
