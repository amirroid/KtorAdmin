package ir.amirreza.dashboard

import dashboard.ChartDashboardSection
import dashboard.DashboardAggregationFunction
import dashboard.KtorAdminDashboard
import ir.amirreza.Priority
import models.chart.AdminChartStyle
import models.chart.ChartField

class CustomDashboard : KtorAdminDashboard() {
    override fun KtorAdminDashboard.configure() {
        addSection(TaskChartSection())
        addSection(Task2ChartSection())
    }
}

class TaskChartSection : ChartDashboardSection() {
    override val aggregationFunction: DashboardAggregationFunction
        get() = DashboardAggregationFunction.ALL
    override val tableName: String
        get() = "tasks"
    override val labelField: String
        get() = "priority"
    override val valuesFields: List<ChartField>
        get() = listOf(
            ChartField(
                fieldName = "number"
            )
        )

    override fun provideBorderColor(label: String, valueField: String): String? {
        return "black"
    }

    override fun provideFillColor(label: String, valueField: String): String? {
        return when (label) {
            Priority.Low.name -> "purple"
            Priority.Medium.name -> "blue"
            Priority.High.name -> "wheat"
            else -> null
        }
    }

    override val orderQuery: String?
        get() = "id ASC"

    override val chartStyle: AdminChartStyle
        get() = AdminChartStyle.LINE
    override val sectionName: String
        get() = "TEst"
    override val index: Int
        get() = 1


    override val borderWidth: Float
        get() = 2f
}
class Task2ChartSection : ChartDashboardSection() {
    override val aggregationFunction: DashboardAggregationFunction
        get() = DashboardAggregationFunction.SUM
    override val tableName: String
        get() = "tasks"
    override val labelField: String
        get() = "priority"
    override val valuesFields: List<ChartField>
        get() = listOf(
            ChartField(
                fieldName = "number"
            )
        )

    override fun provideBorderColor(label: String, valueField: String): String? {
        return "black"
    }

    override fun provideFillColor(label: String, valueField: String): String? {
        return when (label) {
            Priority.Low.name -> "purple"
            Priority.Medium.name -> "blue"
            Priority.High.name -> "wheat"
            else -> null
        }
    }

    override val chartStyle: AdminChartStyle
        get() = AdminChartStyle.PIE
    override val sectionName: String
        get() = "Tasks 2"
    override val index: Int
        get() = 2


    override val borderWidth: Float
        get() = 2f
}