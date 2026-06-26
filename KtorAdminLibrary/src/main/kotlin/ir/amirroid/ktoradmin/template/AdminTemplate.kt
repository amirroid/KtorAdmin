package ir.amirroid.ktoradmin.template

import io.ktor.server.application.ApplicationCall

/**
 * Interface that defines the rendering contract for an admin panel template.
 *
 * Every method corresponds to a distinct page or view in the admin panel.
 * Implementations decide how each view is rendered (Velocity, Thymeleaf, Ktor HTML DSL, etc.).
 */
interface AdminTemplate {
    /**
     * Renders the main dashboard page.
     *
     * @param call The current application call.
     * @param model The data model for the dashboard view.
     */
    suspend fun renderDashboard(
        call: ApplicationCall,
        model: TemplateModel,
    )

    /**
     * Renders the panel list (table) page.
     *
     * @param call The current application call.
     * @param model The data model for the list view.
     */
    suspend fun renderPanelList(
        call: ApplicationCall,
        model: TemplateModel,
    )

    /**
     * Renders the JDBC upsert (add/edit) page.
     *
     * @param call The current application call.
     * @param model The data model for the upsert view.
     */
    suspend fun renderJdbcUpsert(
        call: ApplicationCall,
        model: TemplateModel,
    )

    /**
     * Renders the MongoDB upsert (add/edit) page.
     *
     * @param call The current application call.
     * @param model The data model for the upsert view.
     */
    suspend fun renderMongoUpsert(
        call: ApplicationCall,
        model: TemplateModel,
    )

    /**
     * Renders the login page.
     *
     * @param call The current application call.
     * @param model The data model for the login view.
     */
    suspend fun renderLogin(
        call: ApplicationCall,
        model: TemplateModel,
    )

    /**
     * Renders the JDBC confirmation page.
     *
     * @param call The current application call.
     * @param model The data model for the confirmation view.
     */
    suspend fun renderJdbcConfirmation(
        call: ApplicationCall,
        model: TemplateModel,
    )

    /**
     * Renders the MongoDB confirmation page.
     *
     * @param call The current application call.
     * @param model The data model for the confirmation view.
     */
    suspend fun renderMongoConfirmation(
        call: ApplicationCall,
        model: TemplateModel,
    )
}
