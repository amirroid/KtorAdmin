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
        addRow("200px") {
            addSection(TaskTextSection())
            addSection(Task2TextSection())
            addSection(Task3TextSection())
            addSection(Task4TextSection())
            addSection(Task5TextSection())
        }
        addRow {
            addSection(2f, TaskChartSection())
            addSection(Task2ChartSection(2))
        }
        addRow {
            addSection(Task2ChartSection(3))
            addSection(Task3ChartSection())
            addSection(Task4ChartSection())
        }
        addRow {
            addSection(Task5ChartSection())
            addSection(Task6ChartSection())
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
            ),
        )

    override fun provideBorderColor(label: String, valueField: String): String? {
        return "black"
    }

    override fun provideFillColor(label: String, valueField: String): String? {
        return when (label) {
//            Priority.Low.name -> "purple"
//            Priority.Medium.name -> "blue"
            else -> "wheat"
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
        get() = "The last item of numbers"
}

class Task2TextSection : TextDashboardSection() {
    override val tableName: String
        get() = "tasks"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.AVERAGE

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "Sample Section"
    override val index: Int
        get() = 6
    override val hintText: String
        get() = "The average of numbers"
}

class Task3TextSection : TextDashboardSection() {
    override val tableName: String
        get() = "tasks"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.SUM

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "Sample Section"
    override val index: Int
        get() = 7
    override val hintText: String
        get() = "The sum of numbers"
}

class Task4TextSection : TextDashboardSection() {
    override val tableName: String
        get() = "tasks"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.PROFIT_PERCENTAGE

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "Sample Section"
    override val index: Int
        get() = 8
    override val hintText: String
        get() = "The profit precentage of numbers"
}


class Task5TextSection : TextDashboardSection() {
    override val tableName: String
        get() = "tasks"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.COUNT

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "Sample Section"
    override val index: Int
        get() = 20
    override val hintText: String
        get() = "The count of numbers"
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

class Task3ChartSection() : ChartDashboardSection() {
    override val aggregationFunction: ChartDashboardAggregationFunction
        get() = ChartDashboardAggregationFunction.AVERAGE
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
        get() = AdminChartStyle.RADAR
    override val sectionName: String
        get() = "Tasks 2"
    override val index: Int
        get() = 12


    override val borderWidth: Float
        get() = 2f
}

class Task4ChartSection() : ChartDashboardSection() {
    override val aggregationFunction: ChartDashboardAggregationFunction
        get() = ChartDashboardAggregationFunction.AVERAGE
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
        get() = AdminChartStyle.DOUGHNUT
    override val sectionName: String
        get() = "Tasks 4"
    override val index: Int
        get() = 15


    override val borderWidth: Float
        get() = 2f
}

class Task5ChartSection() : ChartDashboardSection() {
    override val aggregationFunction: ChartDashboardAggregationFunction
        get() = ChartDashboardAggregationFunction.COUNT
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
        get() = AdminChartStyle.POLAR_AREA
    override val sectionName: String
        get() = "Tasks 4"
    override val index: Int
        get() = 16


    override val borderWidth: Float
        get() = 2f
}

class Task6ChartSection() : ChartDashboardSection() {
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

    override val chartStyle: AdminChartStyle
        get() = AdminChartStyle.BAR
    override val sectionName: String
        get() = "Tasks 4"
    override val index: Int
        get() = 17


    override val borderWidth: Float
        get() = 2f
}