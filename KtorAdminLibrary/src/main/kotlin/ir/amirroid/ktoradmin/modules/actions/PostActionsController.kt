package ir.amirroid.ktoradmin.modules.actions

import ir.amirroid.ktoradmin.csrf.CsrfManager
import io.ktor.http.HttpHeaders
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.panels.getAllCustomActions
import ir.amirroid.ktoradmin.utils.Constants
import ir.amirroid.ktoradmin.utils.badRequest
import ir.amirroid.ktoradmin.utils.invalidateRequest
import ir.amirroid.ktoradmin.utils.notFound
import ir.amirroid.ktoradmin.validators.checkHasRole
import kotlinx.serialization.json.Json

/**
 * Handles custom actions in the admin panel.
 * This function processes bulk actions on selected items in admin panels.
 *
 * @param panels List of available admin panels
 */
internal suspend fun RoutingContext.handleActions(panels: List<AdminPanel>) {
    // Extract route parameters
    val pluralName = call.parameters["pluralName"]
    val actionName = call.parameters["actionName"]

    // Find the target panel by its plural name
    val panel = panels.find { it.getPluralName() == pluralName }

    // Find the requested action in the panel
    val action = panel?.getAllCustomActions()?.firstOrNull { actionName == it.key }

    when {
        // Handle case when panel is not found
        panel == null || panel.isShowInAdminPanel().not() -> {
            call.notFound("No panel found with plural name: $pluralName")
        }
        // Handle case when action is not found
        action == null -> {
            call.notFound("Action '$actionName' not found in panel with plural name: $pluralName")
        }
        // Process the action if both panel and action exist
        else -> {
            call.checkHasRole(panel) {
                runCatching {
                    // Parse form data and validate CSRF token
                    val form = call.receiveParameters()
                    val csrfToken = form["_csrf"]
                    if (csrfToken?.let { CsrfManager.validateToken(it) } != true) {
                        call.invalidateRequest()
                        return@runCatching
                    }

                    // Validate and parse selected item IDs
                    val idsForm = form["ids"] ?: run {
                        badRequest("The 'ids' field is required but not found in the form.")
                        return@runCatching
                    }

                    // Parse JSON array of IDs and validate
                    val ids = Json.decodeFromString<List<String>>(idsForm)
                    if (ids.isEmpty()) {
                        badRequest("Please select at least one item.")
                        return@runCatching
                    }

                    // Determine the collection/table name based on panel type
                    val name = when (panel) {
                        is AdminMongoCollection -> panel.getCollectionName()
                        is AdminJdbcTable -> panel.getTableName()
                        else -> panel.getPluralName()
                    }

                    // Execute the action and redirect
                    action.performAction(name, ids)
                    val previewsUrl =
                        call.request.headers[HttpHeaders.Referrer] ?: "/admin/${Constants.RESOURCES_PATH}/$pluralName"
                    call.respondRedirect(previewsUrl)
                }.onFailure {
                    badRequest("Error: ${it.message}", it)
                }
            }
        }
    }
}