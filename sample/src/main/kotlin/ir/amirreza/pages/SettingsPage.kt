package ir.amirreza.pages

import io.ktor.server.application.ApplicationCall
import ir.amirroid.ktoradmin.pages.CustomAdminPage

class SettingsPage : CustomAdminPage() {
    override val path = "settings"
    override val title = "Settings"
    override val description = "Application settings and configuration"
    override val icon = "/static/images/settings.svg"
    override val groupName = "Management"
    override val order = 1

    override suspend fun content(call: ApplicationCall): String {
        return """
            <div style="max-width:700px;">
                <h2 style="margin-bottom:16px;">Application Settings</h2>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;margin-bottom:16px;">
                    <h3 style="margin:0 0 8px 0;">General</h3>
                    <p style="margin:0;color:var(--transparent-black-50);">Configure general application settings here.</p>
                </div>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;margin-bottom:16px;">
                    <h3 style="margin:0 0 8px 0;">Notifications</h3>
                    <p style="margin:0;color:var(--transparent-black-50);">Manage email and push notification preferences.</p>
                </div>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;">
                    <h3 style="margin:0 0 8px 0;">Security</h3>
                    <p style="margin:0;color:var(--transparent-black-50);">Configure authentication and access control settings.</p>
                </div>
            </div>
        """.trimIndent()
    }
}
