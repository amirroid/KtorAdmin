package ir.amirreza.pages

import io.ktor.server.application.ApplicationCall
import ir.amirroid.ktoradmin.pages.CustomAdminPage

class HelpPage : CustomAdminPage() {
    override val path = "help"
    override val title = "Help & Docs"
    override val description = "Documentation and usage guides"
    override val icon = "/static/images/info.svg"
    override val groupName = "Support"
    override val order = 1

    override suspend fun content(call: ApplicationCall): String {
        return """
            <div style="max-width:700px;">
                <h2 style="margin-bottom:16px;">Help & Documentation</h2>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;margin-bottom:16px;">
                    <h3 style="margin:0 0 8px 0;">Getting Started</h3>
                    <p style="margin:0 0 12px 0;color:var(--transparent-black-50);">Learn how to set up and configure your admin panel.</p>
                    <a href="https://github.com/Amirroid/KtorAdmin" target="_blank" style="color:var(--secondary-color,#9A6C00);font-weight:600;text-decoration:none;">View on GitHub &rarr;</a>
                </div>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;margin-bottom:16px;">
                    <h3 style="margin:0 0 8px 0;">Custom Panels</h3>
                    <p style="margin:0 0 12px 0;color:var(--transparent-black-50);">Create CRUD panels by annotating your Exposed/Hibernate/MongoDB tables.</p>
                    <code style="background:rgba(0,0,0,0.06);padding:4px 8px;border-radius:4px;font-size:0.9rem;">@ExposedTable</code>
                </div>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;margin-bottom:16px;">
                    <h3 style="margin:0 0 8px 0;">Custom Pages</h3>
                    <p style="margin:0 0 12px 0;color:var(--transparent-black-50);">Register standalone pages using class-based or DSL approaches.</p>
                    <code style="background:rgba(0,0,0,0.06);padding:4px 8px;border-radius:4px;font-size:0.9rem;">CustomAdminPage</code>
                </div>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;">
                    <h3 style="margin:0 0 8px 0;">Dashboard</h3>
                    <p style="margin:0 0 12px 0;color:var(--transparent-black-50);">Build a custom dashboard with charts, stats, and recent items.</p>
                </div>
            </div>
        """.trimIndent()
    }
}
