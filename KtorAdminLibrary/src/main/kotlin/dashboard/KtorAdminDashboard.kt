package dashboard

abstract class KtorAdminDashboard {
    internal val sections = mutableListOf<DashboardSection>()

    init {
        configure()
    }

    abstract fun KtorAdminDashboard.configure()

    open fun addSection(section: DashboardSection) {
        sections.add(section)
    }
}