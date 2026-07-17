package ir.amirreza.pages

import io.ktor.server.application.ApplicationCall
import ir.amirroid.ktoradmin.pages.CustomAdminPage
import java.lang.management.ManagementFactory

class SystemStatusPage : CustomAdminPage() {
    override val path = "system/status"
    override val title = "System Status"
    override val description = "Real-time system health and status monitoring"
    override val icon = "/static/images/health.svg"
    override val groupName = "Management"
    override val order = 2

    override suspend fun content(call: ApplicationCall): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val processors = runtime.availableProcessors()
        val os = ManagementFactory.getOperatingSystemMXBean()
        val loadAverage = os.systemLoadAverage

        return """
            <div style="max-width:700px;">
                <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px;">
                    <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;">
                        <h4 style="margin:0 0 4px 0;color:var(--transparent-black-50);font-size:0.85rem;text-transform:uppercase;letter-spacing:0.5px;">Memory Used</h4>
                        <p style="margin:0;font-size:1.8rem;font-weight:700;">${usedMemory / (1024 * 1024)} MB</p>
                    </div>
                    <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;">
                        <h4 style="margin:0 0 4px 0;color:var(--transparent-black-50);font-size:0.85rem;text-transform:uppercase;letter-spacing:0.5px;">Max Memory</h4>
                        <p style="margin:0;font-size:1.8rem;font-weight:700;">${maxMemory / (1024 * 1024)} MB</p>
                    </div>
                    <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;">
                        <h4 style="margin:0 0 4px 0;color:var(--transparent-black-50);font-size:0.85rem;text-transform:uppercase;letter-spacing:0.5px;">Available Processors</h4>
                        <p style="margin:0;font-size:1.8rem;font-weight:700;">$processors</p>
                    </div>
                    <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;">
                        <h4 style="margin:0 0 4px 0;color:var(--transparent-black-50);font-size:0.85rem;text-transform:uppercase;letter-spacing:0.5px;">System Load</h4>
                        <p style="margin:0;font-size:1.8rem;font-weight:700;">${if (loadAverage >= 0) "%.2f".format(loadAverage) else "N/A"}</p>
                    </div>
                </div>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;margin-bottom:16px;">
                    <h3 style="margin:0 0 8px 0;">JVM Version</h3>
                    <p style="margin:0;color:var(--transparent-black-50);">${System.getProperty("java.version") ?: "N/A"}</p>
                </div>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;">
                    <h3 style="margin:0 0 8px 0;">Operating System</h3>
                    <p style="margin:0;color:var(--transparent-black-50);">${System.getProperty("os.name")} ${System.getProperty("os.version")}</p>
                </div>
            </div>
        """.trimIndent()
    }
}
