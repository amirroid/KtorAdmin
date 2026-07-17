package ir.amirreza.pages

import io.ktor.server.application.ApplicationCall
import ir.amirroid.ktoradmin.pages.CustomAdminPage

class AboutPage : CustomAdminPage() {
    override val path = "about"
    override val title = "About"
    override val description = "About this application"
    override val icon = "/static/images/info.svg"
    override val groupName = "Support"
    override val order = 2

    override suspend fun content(call: ApplicationCall): String {
        return """
            <div style="max-width:500px;">
                <h2 style="margin-bottom:16px;">About</h2>
                <div style="background:var(--white-transparent-60);padding:24px;border-radius:12px;text-align:center;">
                    <h3 style="margin:0 0 8px 0;">KtorAdmin</h3>
                    <p style="margin:0 0 4px 0;color:var(--transparent-black-50);">Admin panel framework for Ktor</p>
                    <p style="margin:0;color:var(--transparent-black-50);font-size:0.85rem;">Built with Kotlin & Ktor</p>
                </div>
            </div>
        """.trimIndent()
    }
}
