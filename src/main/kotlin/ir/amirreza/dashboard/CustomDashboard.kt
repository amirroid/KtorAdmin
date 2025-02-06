package ir.amirreza.dashboard

import dashboard.ChartDashboardSection
import dashboard.DashboardAggregationFunction
import dashboard.KtorAdminDashboard
import ir.amirreza.Priority
import models.chart.AdminChartStyle

class CustomDashboard : KtorAdminDashboard() {
    override fun KtorAdminDashboard.configure() {
        addSection(TaskChartSection())
    }
}

class TaskChartSection : ChartDashboardSection() {
    override val aggregationFunction: DashboardAggregationFunction
        get() = DashboardAggregationFunction.SUM
    override val tableName: String
        get() = "tasks"
    override val labelField: String
        get() = "priority"
    override val valuesFields: List<String>
        get() = listOf("number")

    override fun provideBorderColor(label: String, valueField: String): String? {
        return "black"
    }

    override fun provideFillColor(label: String, valueField: String): String? {
        return when (label) {
            Priority.Low.name -> "red"
            Priority.Medium.name -> "blue"
            Priority.High.name -> "wheat"
            else -> null
        }
    }

    override val chartStyle: AdminChartStyle
        get() = AdminChartStyle.LINE
    override val sectionName: String
        get() = "TEst"
    override val index: Int
        get() = 1


    override val borderWidth: Float
        get() = 2f
}