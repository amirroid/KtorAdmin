package ir.amirreza.dashboard

import dashboard.chart.ChartDashboardSection
import models.chart.ChartDashboardAggregationFunction
import dashboard.KtorAdminDashboard
import dashboard.list.ListDashboardSection
import dashboard.simple.TextDashboardSection
import ir.amirreza.Priority
import models.chart.AdminChartStyle
import models.chart.ChartField
import models.chart.TextDashboardAggregationFunction
import kotlin.math.max

class CustomDashboard : KtorAdminDashboard() {
    override fun KtorAdminDashboard.configure() {
        configureLayout {
            addSection(section = TaskTextSection(), height = "200px")
            addSection(section = Task2TextSection(), height = "200px")
            addSection(section = Task3TextSection(), height = "200px")
            addSection(section = Task4TextSection(), height = "200px")
            addSection(span = 2, section = TaskChartSection())
            addSection(span = 2, section = UserFilesChartSection())
            addSection(span = 3, section = TaskListChartSection())
            media(maxWidth = "600px", template = listOf(1))
            addSection(section = UserFileTextSection())
            addSection(section = UserFile2TextSection(), height = "200px")
            addSection(section = UserFile3TextSection(), height = "200px")
            addSection(section = UserFile4TextSection(), height = "200px")
            addSection(section = UserFile5TextSection(), height = "200px")
            addSection(Task5TextSection())
            addSection(Task2ChartSection(2))
            addSection(Task2ChartSection(3))
            addSection(Task4ChartSection())
            addSection(Task5ChartSection())
            addSection(Task3ChartSection())
            addSection(2, Task6ChartSection())
            addSection(4, UserFileListChartSection())
        }
    }
}



class TaskChartSection : ChartDashboardSection() {
    override val aggregationFunction: ChartDashboardAggregationFunction
        get() = ChartDashboardAggregationFunction.ALL
    override val tableName: String
        get() = "tasks"
    override val labelField: String
        get() = "date"
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
        get() = 0.49f


    override val borderWidth: Float
        get() = 2f
}

class UserFilesChartSection : ChartDashboardSection() {
    override val aggregationFunction: ChartDashboardAggregationFunction
        get() = ChartDashboardAggregationFunction.ALL
    override val tableName: String
        get() = "user_files"
    override val labelField: String
        get() = "createdAt"
    override val valuesFields: List<ChartField>
        get() = listOf(
            ChartField(
                fieldName = "number"
            ),
        )

    override fun provideBorderColor(label: String, valueField: String): String? {
        return "black"
    }

    override val orderQuery: String?
        get() = "_id ASC"


    override fun provideFillColor(label: String, valueField: String): String? {
        return when (label) {
//            Priority.Low.name -> "purple"
//            Priority.Medium.name -> "blue"
            else -> "wheat"
        }
    }

    override val chartStyle: AdminChartStyle
        get() = AdminChartStyle.LINE
    override val sectionName: String
        get() = "User files"
    override val index: Int
        get() = 112

    override val tension: Float
        get() = 0.49f


    override val borderWidth: Float
        get() = 2f
}

class TaskListChartSection : ListDashboardSection() {
    override val tableName: String
        get() = "tasks"
    override val sectionName: String
        get() = "TASKS"
    override val index: Int
        get() = 25
    override val fields: List<String>?
        get() = listOf(
            "name", "priority", "file", "number",
            "checked"
        )

    override val limitCount: Int?
        get() = 2

    override val orderQuery: String?
        get() = "id DESC"
}


class UserFileListChartSection : ListDashboardSection() {
    override val tableName: String
        get() = "user_files"
    override val sectionName: String
        get() = "Users"
    override val index: Int
        get() = 123

    override val fields: List<String>?
        get() = listOf("title", "file", "number", "createdAt")

    override val limitCount: Int?
        get() = 3

    override val orderQuery: String?
        get() = "_id DESC"
}

class TaskTextSection : TextDashboardSection() {
    override val tableName: String
        get() = "tasks"
    override val fieldName: String
        get() = "priority"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.LAST_ITEM

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "Sample Section"
    override val index: Int
        get() = 5
    override val hintText: String
        get() = "The last item of priorities"
}

class UserFileTextSection : TextDashboardSection() {
    override val tableName: String
        get() = "user_files"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.LAST_ITEM

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "User files section"
    override val index: Int
        get() = 521
    override val hintText: String
        get() = "The last item of User files"
}

class UserFile2TextSection : TextDashboardSection() {
    override val tableName: String
        get() = "user_files"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.AVERAGE

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "User files section"
    override val index: Int
        get() = 522
    override val hintText: String
        get() = "The average of User files"
}

class UserFile3TextSection : TextDashboardSection() {
    override val tableName: String
        get() = "user_files"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.SUM

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "User files section"
    override val index: Int
        get() = 5243
    override val hintText: String
        get() = "The sum of User files"
}

class UserFile4TextSection : TextDashboardSection() {
    override val tableName: String
        get() = "user_files"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.PROFIT_PERCENTAGE

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "User files section"
    override val index: Int
        get() = 52432
    override val hintText: String
        get() = "The profit of User files"
}

class UserFile5TextSection : TextDashboardSection() {
    override val tableName: String
        get() = "user_files"
    override val fieldName: String
        get() = "number"
    override val aggregationFunction: TextDashboardAggregationFunction
        get() = TextDashboardAggregationFunction.COUNT

    override val orderQuery: String?
        get() = "id DESC"
    override val sectionName: String
        get() = "User files section"
    override val index: Int
        get() = 524313
    override val hintText: String
        get() = "The count of User files"
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