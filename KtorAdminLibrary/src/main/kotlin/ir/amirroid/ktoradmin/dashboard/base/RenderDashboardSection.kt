package ir.amirroid.ktoradmin.dashboard.base

/**
 * Base class for a custom-rendered dashboard section.
 *
 * This class allows you to generate arbitrary HTML on the backend
 * during dashboard rendering. The rendered HTML is injected directly
 * into the dashboard template without additional processing.
 *
 * You can perform suspend operations (such as database queries or
 * HTTP calls) inside the [render] function to build dynamic content.
 *
 * ### **Required Overrides:**
 * - **[index]** → A unique integer identifier for this section.
 * - **[sectionName]** → The display name shown in the section header.
 * - **[render]** → A suspend function that returns the HTML string to display.
 *
 * ### **Example Usage:**
 * ```kotlin
 * class ServerStatusSection : RenderDashboardSection() {
 *     override val index: Int = 100
 *     override val sectionName: String = "Server Status"
 *
 *     override suspend fun render(): String {
 *         val uptime = fetchUptimeFromDatabase()
 *         return """
 *             <div class="status-card">
 *                 <h3>Server Uptime</h3>
 *                 <p>$uptime</p>
 *             </div>
 *         """.trimIndent()
 *     }
 * }
 * ```
 */
abstract class RenderDashboardSection : DashboardSection {
    override val sectionType: String
        get() = SECTION_TYPE

    /**
     * Generates the HTML content for this dashboard section.
     *
     * This function is called during dashboard rendering and can
     * perform suspend operations such as database queries or
     * external API calls.
     *
     * @return The HTML string to be injected into the dashboard template.
     */
    abstract suspend fun render(): String

    companion object {
        private const val SECTION_TYPE = "render"
    }
}
