package ir.amirroid.ktoradmin.template

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.velocity.VelocityContent
import ir.amirroid.ktoradmin.utils.Constants

/**
 * The default admin template that uses Velocity templates with customizable settings.
 *
 * All existing Velocity templates continue to work unchanged.
 * CSS custom properties are generated from [settings] and injected into every page,
 * so users can override colors, typography, shapes, and layout without editing templates.
 *
 * Usage:
 * ```kotlin
 * install(KtorAdmin) {
 *     template = DefaultAdminTemplate(
 *         settings = DefaultAdminTemplateSettings(
 *             colors = DefaultAdminTemplateSettings.Colors(
 *                 secondaryColor = "#007AFF"
 *             )
 *         )
 *     )
 * }
 * ```
 */
class DefaultAdminTemplate(
    val settings: DefaultAdminTemplateSettings = DefaultAdminTemplateSettings(),
) : AdminTemplate {

    private val cssVariables by lazy { generateCssVariables() }

    private fun generateCssVariables(): String {
        val c = settings.colors
        val t = settings.typography
        val s = settings.shapes
        val sp = settings.spacing
        val dc = c.darkMode
        return """
            :root {
                --primary-color: ${c.primaryColor};
                --secondary-color: ${c.secondaryColor};
                --background-gradient-start: ${c.backgroundGradientStart};
                --background-gradient-end: ${c.backgroundGradientEnd};
                --highlight-color: ${c.highlightColor};
                --error-color: ${c.errorColor};
                --white: #fff;
                --white-transparent-60: rgba(255, 255, 255, 0.6);
                --white-transparent-70: rgba(255, 255, 255, 0.7);
                --white-transparent-80: rgba(255, 255, 255, 0.8);
                --border-color: hsla(213, 10%, 18%, 0.1);
                --transparent-black-10: rgba(0, 0, 0, 0.1);
                --transparent-black-20: rgba(0, 0, 0, 0.2);
                --transparent-black-50: rgba(0, 0, 0, 0.5);
                --background-color-f4: #F4F4F4;
                --background-gradient-start-30: rgba(243, 231, 203, 0.3);
                --secondary-color-20: rgba(154, 108, 0, 0.2);
                --table-even-row-color: ${c.evenRowColor};
                --table-odd-row-color: ${c.oddRowColor};
                --table-hover-row-color: ${c.hoverRowColor};
                --sidebar-border-radius: ${s.sidebarBorderRadius};
                --menu-item-border-radius: ${s.menuItemBorderRadius};
                --dropdown-border-radius: ${s.dropdownBorderRadius};
                --sidebar-width: ${sp.sidebarWidth};
                --sidebar-margin: ${sp.sidebarMargin};
                --body-padding: ${sp.bodyPadding};
                --font-family: ${t.fontFamily};
                --font-scale: ${t.fontScale};
                --sidebar-backdrop-blur: ${settings.sidebar.backdropBlur};
                --transition-duration: ${settings.animations.transitionDuration};
                --transition-timing: ${settings.animations.transitionTiming};
            }
            :root.theme-dark {
                --primary-color: ${dc.primaryColor};
                --secondary-color: ${dc.secondaryColor};
                --background-gradient-start: ${dc.backgroundGradientStart};
                --background-gradient-end: ${dc.backgroundGradientEnd};
                --highlight-color: ${dc.highlightColor};
                --error-color: ${dc.errorColor};
                --white: #2A2D32;
                --white-transparent-60: rgba(40, 44, 52, 0.6);
                --white-transparent-70: rgba(40, 44, 52, 0.7);
                --white-transparent-80: rgba(40, 44, 52, 0.8);
                --border-color: hsla(213, 10%, 80%, 0.1);
                --transparent-black-10: rgba(255, 255, 255, 0.1);
                --transparent-black-20: rgba(255, 255, 255, 0.2);
                --background-color-f4: #2C2C2C;
                --background-gradient-start-30: rgba(255, 184, 77, 0.15);
                --secondary-color-20: rgba(255, 184, 77, 0.2);
                --table-even-row-color: ${dc.evenRowColor};
                --table-odd-row-color: ${dc.oddRowColor};
                --table-hover-row-color: ${dc.hoverRowColor};
            }
            body {
                font-family: ${t.fontFamily};
                padding: ${sp.bodyPadding};
            }
            .sidebar {
                width: calc(${sp.sidebarWidth} - ${sp.bodyPadding} * 2);
                height: calc(100% - ${sp.bodyPadding} * 2);
                margin: ${sp.sidebarMargin};
                border-radius: ${s.sidebarBorderRadius};
                backdrop-filter: blur(${settings.sidebar.backdropBlur});
            }
            .menu-item {
                border-radius: ${s.menuItemBorderRadius};
            }
            .dropdown-content {
                border-radius: ${s.dropdownBorderRadius};
            }
            ${if (t.fontScale != 1.0) """
            body { font-size: ${t.fontScale}rem; }
            """ else ""}
            ${if (!settings.animations.enabled) """
            * { transition: none !important; animation: none !important; }
            """ else ""}
        """.trimIndent()
    }

    private fun TemplateModel.withCustomCss(): TemplateModel {
        val newData = data.toMutableMap()
        newData["customCss"] = cssVariables
        return TemplateModel(newData)
    }

    @Suppress("UNCHECKED_CAST")
    private fun TemplateModel.toVelocityModel(): Map<String, Any> =
        withCustomCss().data as Map<String, Any>

    override suspend fun renderDashboard(call: ApplicationCall, model: TemplateModel) {
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_dashboard.vm",
                model = model.toVelocityModel()
            )
        )
    }

    override suspend fun renderPanelList(call: ApplicationCall, model: TemplateModel) {
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_list.vm",
                model = model.toVelocityModel()
            )
        )
    }

    override suspend fun renderJdbcUpsert(call: ApplicationCall, model: TemplateModel) {
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_upsert.vm",
                model = model.toVelocityModel()
            )
        )
    }

    override suspend fun renderMongoUpsert(call: ApplicationCall, model: TemplateModel) {
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_no_sql_upsert.vm",
                model = model.toVelocityModel()
            )
        )
    }

    override suspend fun renderLogin(call: ApplicationCall, model: TemplateModel) {
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_login.vm",
                model = model.toVelocityModel()
            )
        )
    }

    override suspend fun renderJdbcConfirmation(call: ApplicationCall, model: TemplateModel) {
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_upsert.vm",
                model = model.toVelocityModel()
            )
        )
    }

    override suspend fun renderMongoConfirmation(call: ApplicationCall, model: TemplateModel) {
        call.respond(
            VelocityContent(
                "${Constants.TEMPLATES_PREFIX_PATH}/admin_panel_no_sql_upsert.vm",
                model = model.toVelocityModel()
            )
        )
    }
}
