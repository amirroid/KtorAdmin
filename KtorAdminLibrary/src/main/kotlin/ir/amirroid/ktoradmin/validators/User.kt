package ir.amirroid.ktoradmin.validators

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import ir.amirroid.ktoradmin.authentication.KtorAdminPrincipal
import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.utils.forbidden

internal suspend fun ApplicationCall.checkHasRole(
    panel: AdminPanel,
    build: suspend ApplicationCall.() -> Unit,
) {
    val user = principal<KtorAdminPrincipal>()
    val accessRoles = panel.getAccessRoles()
    if (accessRoles != null && user != null && user.roles?.any { it in accessRoles } != true) {
        forbidden("Access denied: You do not have permission to perform this action.")
        return
    }
    build.invoke(this)
}
