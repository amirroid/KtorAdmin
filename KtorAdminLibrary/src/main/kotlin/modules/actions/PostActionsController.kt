package modules.actions

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import panels.getAllCustomActions
import utils.badRequest
import utils.notFound
import validators.checkHasRole

internal suspend fun RoutingContext.handleActions(panels: List<AdminPanel>) {
    val pluralName = call.parameters["pluralName"]
    val actionName = call.parameters["actionName"]
    val panel = panels.find { it.getPluralName() == pluralName }
    val action = panel?.getAllCustomActions()?.firstOrNull { actionName == it.key }
    when {
        panel == null -> call.notFound("No panel found with plural name: $pluralName")
        action == null -> call.notFound("Action '$actionName' not found in panel with plural name: $pluralName")
        else -> {
            call.checkHasRole(panel) {
                runCatching {
                    val form = call.receiveParameters()
                    val idsForm = form["ids"]
                    if (idsForm == null) {
                        badRequest("The 'ids' field is required but not found in the form.")
                        return@runCatching
                    }
                    val ids = Json.decodeFromString<List<String>>(idsForm)
                    if (ids.isEmpty()) {
                        badRequest("Please select at least one item.")
                        return@runCatching
                    }
                    val name = when (panel) {
                        is AdminMongoCollection -> panel.getCollectionName()
                        is AdminJdbcTable -> panel.getTableName()
                        else -> panel.getPluralName()
                    }
                    action.performAction(name, ids)
                    call.respondRedirect("/admin/$pluralName")
                }.onFailure {
                    badRequest("Error: ${it.message}", it)
                }
            }
        }
    }
}