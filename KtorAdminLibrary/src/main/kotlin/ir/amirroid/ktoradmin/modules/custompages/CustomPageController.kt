package ir.amirroid.ktoradmin.modules.custompages

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import ir.amirroid.ktoradmin.authentication.KtorAdminPrincipal
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.models.PanelGroup
import ir.amirroid.ktoradmin.template.TemplateModel
import ir.amirroid.ktoradmin.utils.Constants
import ir.amirroid.ktoradmin.utils.addCommonModels
import ir.amirroid.ktoradmin.utils.forbidden
import ir.amirroid.ktoradmin.utils.notFound
import ir.amirroid.ktoradmin.utils.serverError

internal suspend fun ApplicationCall.handleCustomPage(
    panelGroups: List<PanelGroup>,
    resolvedPath: String? = null,
) {
    runCatching {
        val pagePath =
            resolvedPath
                ?: parameters.getAll("pagePath")?.joinToString("/")
                ?: return notFound("Custom page not found.")

        val page =
            DynamicConfiguration.getCustomPage(pagePath)
                ?: return notFound("Custom page '$pagePath' not found.")

        if (!page.visible) {
            return notFound("Custom page '$pagePath' is not available.")
        }

        val user = principal<KtorAdminPrincipal>()
        val username = user?.name

        if (page.permissions != null && user != null) {
            val userRoles = user.roles ?: emptyList()
            if (page.permissions.none { it in userRoles }) {
                return forbidden("You do not have permission to access this page.")
            }
        }

        val pageContent = page.renderer(this)

        val adminPath = DynamicConfiguration.adminPath
        val resourceUrl = "/$adminPath/${Constants.RESOURCES_PATH}/${page.path}"

        val model =
            TemplateModel(
                mutableMapOf(
                    "panelGroups" to panelGroups,
                    "pageContent" to pageContent,
                    "pageTitle" to page.title,
                    "pageDescription" to (page.description ?: ""),
                    "currentPagePath" to page.path,
                    "resourceUrl" to resourceUrl,
                ).apply {
                    username?.let { put("username", it) }
                }.addCommonModels(null, panelGroups, applicationCall = this),
            )

        DynamicConfiguration.template.renderCustomPage(this, model)
    }.onFailure {
        serverError(it.message.orEmpty(), it)
    }
}
