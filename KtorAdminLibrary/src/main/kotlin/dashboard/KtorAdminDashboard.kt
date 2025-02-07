package dashboard

import dashboard.row.Row

abstract class KtorAdminDashboard {
    internal val rows = mutableListOf<Row>()

    init {
        configure()
    }

    abstract fun KtorAdminDashboard.configure()

    fun addRow(rowHeight: String = "350px", builder: Row.() -> Unit): Row {
        val builder = Row(rowHeight).apply(builder)
        rows += builder
        return builder
    }
}