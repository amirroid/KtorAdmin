package ir.amirreza

import annotations.actions.AdminActions
import annotations.chart.DashboardChartConfig
import annotations.computed.Computed
import annotations.display.DisplayFormat
import annotations.enumeration.Enumeration
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.order.DefaultOrder
import annotations.query.AdminQueries
import annotations.references.References
import annotations.roles.AccessRoles
import annotations.status.StatusStyle
import annotations.uploads.LocalUpload
import models.actions.Action
import models.chart.AdminChartStyle
import org.jetbrains.exposed.sql.*

enum class Priority {
    Low, Medium, High
}


@ExposedTable("tasks", "id", "task", "tasks")
@AccessRoles("admin")
@AdminQueries(
    searches = ["user_id.username", "description"],
    filters = ["priority", "user_id", "checked"]
)
@DisplayFormat(
    format = "{id} - User: {user_id.username}",
)
@DefaultOrder(
    "name",
    "DESC"
)
@DashboardChartConfig(
    labelField = "priority",
    valuesFields = ["name", "checked"],
    chartStyle = AdminChartStyle.BAR,
    borderColors = ["#FF5733", "#FF5733"],
    fillColors = ["#FF8D1A", "#FF8D1A"],
    limitCount = 10,
    orderQuery = "priority DESC"
)
@AdminActions(
    actions = [Action.DELETE, Action.ADD],
)
object Tasks : Table("tasks") {
    @IgnoreColumn
    val id = integer("id").autoIncrement()

    @Limits(
        maxLength = 20,
    )
    val name = varchar("name", length = 150)

    @Limits(
        maxLength = 500
    )
    val description = text("description")

    @Enumeration("Low", "Medium", "High")
    @StatusStyle("#5ab071", "#493391", "#d62454")
    val priority = customEnumeration(
        "priority",
        "VARCHAR(50)",
        { Priority.valueOf(it as String) },
        { it.name }
    )

    @ColumnInfo("user_id", verboseName = "User")
    @References("users", "id")
    val userId = integer("user_id").references(Users.id)

    @Computed(
        compute = "{name}.toLowerCase().replaceAll(' ', '-')"
    )
    val slug = varchar("slug", 500)

    @LocalUpload
    @Limits(
        maxBytes = 1024 * 1024 * 20,
        allowedMimeTypes = ["video/mp4"]
    )
    val file = varchar("file", 1000).nullable()

    @ColumnInfo(
        columnName = "thumbnail",
        nullable = true,
        readOnly = true
    )
    @LocalUpload
    val videoThumbnail = varchar("thumbnail", 1000).nullable()

    val data = binary("data").nullable()

    val checked = bool("checked").default(true)

    override val primaryKey = PrimaryKey(id)
}
