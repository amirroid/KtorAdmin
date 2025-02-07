package dashboard

abstract class KtorAdminDashboard {
    internal val rows = mutableListOf<Row>()

    init {
        configure()
    }

    abstract fun KtorAdminDashboard.configure()

    fun addRow(builder: Row.() -> Unit): Row {
        val builder = Row().apply(builder)
        rows += builder
        return builder
    }
}