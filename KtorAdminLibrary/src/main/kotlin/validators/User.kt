package validators

import authentication.KtorAdminPrincipal
import io.ktor.server.application.*
import io.ktor.server.auth.*
import panels.AdminPanel
import utils.forbidden

internal suspend fun ApplicationCall.checkHasRole(panel: AdminPanel, build: suspend ApplicationCall.() -> Unit) {
    val user = principal<KtorAdminPrincipal>()
    val accessRoles = panel.getAccessRoles()
    if (accessRoles != null && user?.roles?.any { it in accessRoles } != true) {
        forbidden("Access denied: You do not have permission to perform this action.")
        return
    }
    build.invoke(this)
}