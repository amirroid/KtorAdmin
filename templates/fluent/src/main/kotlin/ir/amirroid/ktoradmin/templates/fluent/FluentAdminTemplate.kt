@file:Suppress("ktlint:standard:string-template-indent")

package ir.amirroid.ktoradmin.templates.fluent

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.velocity.VelocityContent
import ir.amirroid.ktoradmin.template.AdminTemplate
import ir.amirroid.ktoradmin.template.TemplateModel

class FluentAdminTemplate(
    val settings: FluentAdminTemplateSettings = FluentAdminTemplateSettings(),
) : AdminTemplate {
    private val templatesPath = "/templates/fluent_admin"

    private val cssVariables by lazy { generateCssVariables() }

    private fun generateCssVariables(): String {
        val c = settings.colors
        val t = settings.typography
        val font = t.font
        val s = settings.shapes
        val sp = settings.spacing
        val dc = settings.darkModeColors
        val sidebar = settings.sidebar
        return """
                :root {
                    --fluent-brand: ${c.brandColor};
                    --fluent-brand-hover: ${c.brandHoverColor};
                    --fluent-brand-pressed: ${c.brandPressedColor};
                    --fluent-surface: ${c.surfaceColor};
                    --fluent-surface-hover: ${c.surfaceHoverColor};
                    --fluent-background: ${c.backgroundLayerColor};
                    --fluent-subtle: ${c.subtleColor};
                    --fluent-border: ${c.defaultBorderColor};
                    --fluent-border-strong: ${c.strongBorderColor};
                    --fluent-error: ${c.errorColor};
                    --fluent-success: ${c.successColor};
                    --fluent-warning: ${c.warningColor};
                    --fluent-info: ${c.infoColor};
                    --fluent-radius-sm: ${s.borderRadiusSmall};
                    --fluent-radius-md: ${s.borderRadiusMedium};
                    --fluent-radius-lg: ${s.borderRadiusLarge};
                    --fluent-sidebar-width: ${sidebar.width};
                    --fluent-content-padding: ${sp.contentPadding};
                    --fluent-header-height: ${settings.header.height};
                    --fluent-font-family: ${font.cssFamily};
                    --fluent-transition: ${settings.animations.transitionDuration} ${settings.animations.transitionTiming};
                    --fluent-shadow-2: 0 1px 2px rgba(0, 0, 0, 0.14), 0 0 2px rgba(0, 0, 0, 0.12);
                    --fluent-shadow-4: 0 2px 4px rgba(0, 0, 0, 0.14), 0 0 2px rgba(0, 0, 0, 0.12);
                    --fluent-shadow-8: 0 4px 8px rgba(0, 0, 0, 0.14), 0 0 2px rgba(0, 0, 0, 0.12);
                    --fluent-shadow-16: 0 8px 16px rgba(0, 0, 0, 0.14), 0 0 2px rgba(0, 0, 0, 0.12);
                    --fluent-shadow-28: 0 14px 28px rgba(0, 0, 0, 0.18), 0 0 2px rgba(0, 0, 0, 0.12);
                    --fluent-text-primary: #242424;
                    --fluent-text-secondary: #616161;
                    --fluent-text-disabled: #BDBDBD;
                }
                :root.theme-dark {
                    --fluent-brand: ${dc.brandColor};
                    --fluent-brand-hover: ${dc.brandHoverColor};
                    --fluent-brand-pressed: ${dc.brandPressedColor};
                    --fluent-surface: ${dc.surfaceColor};
                    --fluent-surface-hover: ${dc.surfaceHoverColor};
                    --fluent-background: ${dc.backgroundLayerColor};
                    --fluent-subtle: ${dc.subtleColor};
                    --fluent-border: ${dc.defaultBorderColor};
                    --fluent-border-strong: ${dc.strongBorderColor};
                    --fluent-error: ${dc.errorColor};
                    --fluent-success: ${dc.successColor};
                    --fluent-warning: ${dc.warningColor};
                    --fluent-info: ${dc.infoColor};
                    --fluent-text-primary: #FFFFFF;
                    --fluent-text-secondary: #ADADAD;
                    --fluent-text-disabled: #616161;
                }
                body {
                    font-family: ${font.cssFamily};
                    padding: ${sp.bodyPadding};
                    margin: 0;
                    background-color: var(--fluent-background);
                }
                ${if (!settings.animations.enabled) {
            """
            * { transition: none !important; animation: none !important; }
            """
        } else {
            ""
        }}
            """.trimIndent()
    }

    private fun TemplateModel.withTemplateSettings(): TemplateModel {
        val newData = data.toMutableMap()
        newData["customCss"] = cssVariables
        val font = settings.typography.font
        newData["fontFamily"] = font.cssFamily
        font.stylesheet?.let { newData["fontStylesheet"] = it }
        val header = settings.header
        when (val content = header.content) {
            is FluentAdminTemplateSettings.HeaderContent.Text -> {
                newData["headerContentType"] = "text"
                newData["headerTextPrefix"] = content.prefix
                newData["headerTextContent"] = content.text
            }
            is FluentAdminTemplateSettings.HeaderContent.Image -> {
                newData["headerContentType"] = "image"
                newData["headerImageUrl"] = content.url
                newData["headerImageAlt"] = content.altText
                newData["headerImageHeight"] = content.height
            }
        }
        return TemplateModel(newData)
    }

    @Suppress("UNCHECKED_CAST")
    private fun TemplateModel.toVelocityModel(): Map<String, Any> = withTemplateSettings().data as Map<String, Any>

    override suspend fun renderDashboard(
        call: ApplicationCall,
        model: TemplateModel,
    ) {
        call.respond(
            VelocityContent(
                "$templatesPath/admin_dashboard.vm",
                model = model.toVelocityModel(),
            ),
        )
    }

    override suspend fun renderPanelList(
        call: ApplicationCall,
        model: TemplateModel,
    ) {
        call.respond(
            VelocityContent(
                "$templatesPath/admin_panel_list.vm",
                model = model.toVelocityModel(),
            ),
        )
    }

    override suspend fun renderJdbcUpsert(
        call: ApplicationCall,
        model: TemplateModel,
    ) {
        call.respond(
            VelocityContent(
                "$templatesPath/admin_panel_upsert.vm",
                model = model.toVelocityModel(),
            ),
        )
    }

    override suspend fun renderMongoUpsert(
        call: ApplicationCall,
        model: TemplateModel,
    ) {
        call.respond(
            VelocityContent(
                "$templatesPath/admin_panel_no_sql_upsert.vm",
                model = model.toVelocityModel(),
            ),
        )
    }

    override suspend fun renderLogin(
        call: ApplicationCall,
        model: TemplateModel,
    ) {
        call.respond(
            VelocityContent(
                "$templatesPath/admin_panel_login.vm",
                model = model.toVelocityModel(),
            ),
        )
    }

    override suspend fun renderJdbcConfirmation(
        call: ApplicationCall,
        model: TemplateModel,
    ) {
        call.respond(
            VelocityContent(
                "$templatesPath/admin_panel_upsert.vm",
                model = model.toVelocityModel(),
            ),
        )
    }

    override suspend fun renderMongoConfirmation(
        call: ApplicationCall,
        model: TemplateModel,
    ) {
        call.respond(
            VelocityContent(
                "$templatesPath/admin_panel_no_sql_upsert.vm",
                model = model.toVelocityModel(),
            ),
        )
    }

    override suspend fun renderCustomPage(
        call: ApplicationCall,
        model: TemplateModel,
    ) {
        call.respond(
            VelocityContent(
                "$templatesPath/admin_custom_page.vm",
                model = model.toVelocityModel(),
            ),
        )
    }
}
