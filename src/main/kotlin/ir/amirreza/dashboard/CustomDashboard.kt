package ir.amirreza.dashboard

import dashboard.chart.ChartDashboardSection
import models.chart.ChartDashboardAggregationFunction
import dashboard.KtorAdminDashboard
import dashboard.simple.TextDashboardSection
import ir.amirreza.Priority
import models.chart.AdminChartStyle
import models.chart.ChartField
import models.chart.TextDashboardAggregationFunction

class CustomDashboard : KtorAdminDashboard() {
    override fun KtorAdminDashboard.configure() {
        addRow {
            addSection(2f, TaskChartSection())
            addSection(Task2ChartSection(2))
        }
        addRow {
            addSection(TaskTextSection())
            addSection(Task2ChartSection(3))
        }
    }
}

class TaskChartSection : ChartDashboardSection() {
    override val aggregationFunction: ChartDashboardAggregationFunction
        get() = ChartDashboardAggregationFunction.ALL
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

    override val tension: Float
        get() = 0.5f


    override val borderWidth: Float
        get() = 2f
}

class TaskTextSection : TextDashboardSection() {
    override val tableName: String
        get() = "tasks"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.LAST_ITEM

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "Sample Section"
    override val index: Int
        get() = 5
    override val hintText: String
        get() = "The data of numbers"
}

class Task2ChartSection(override val index: Int) : ChartDashboardSection() {
    override val aggregationFunction: ChartDashboardAggregationFunction
        get() = ChartDashboardAggregationFunction.SUM
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


    override val borderWidth: Float
        get() = 2f
}