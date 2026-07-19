package ir.amirreza.dashboard

import ir.amirroid.ktoradmin.dashboard.KtorAdminDashboard
import ir.amirroid.ktoradmin.dashboard.base.RenderDashboardSection
import java.lang.management.ManagementFactory

/**
 * Class-based dashboard registered via the DSL's `register()` method.
 *
 * Demonstrates the traditional approach: subclass [KtorAdminDashboard],
 * override metadata properties, and configure the layout in [configure].
 */
class SystemOverviewDashboard : KtorAdminDashboard() {
    override val title = "System Overview"
    override val path = "system-overview"
    override val icon = "/static/images/info.svg"
    override val groupName = "Operations"
    override val order = 0

    override fun KtorAdminDashboard.configure() {
        configureLayout {
            addSection(section = ServerStatusSection(), height = "200px")
            addSection(section = QuickLinksSection(), height = "200px")
            media(maxWidth = "600px", template = listOf(1))
        }
    }
}

class ServerStatusSection : RenderDashboardSection() {
    override val index: Int = 1000
    override val sectionName: String = "Server Status"

    override suspend fun render(): String {
        val uptime = ManagementFactory.getRuntimeMXBean().uptime
        val hours = uptime / 3600000
        val minutes = (uptime % 3600000) / 60000
        return """
            <div style="padding: 16px; font-family: var(--font-family);">
                <p style="font-size: 2rem; font-weight: 700; margin: 0;">${hours}h ${minutes}m</p>
                <p style="color: #666; margin: 4px 0 0 0;">Uptime</p>
            </div>
        """.trimIndent()
    }
}

class QuickLinksSection : RenderDashboardSection() {
    override val index: Int = 1001
    override val sectionName: String = "Quick Links"

    override suspend fun render(): String {
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMb = runtime.maxMemory() / (1024 * 1024)
        return """
            <div style="padding: 16px; font-family: var(--font-family);">
                <p style="font-size: 2rem; font-weight: 700; margin: 0;">${usedMb}MB / ${totalMb}MB</p>
                <p style="color: #666; margin: 4px 0 0 0;">Heap Memory</p>
            </div>
        """.trimIndent()
    }
}
