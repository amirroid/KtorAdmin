package ir.amirreza

import annotations.actions.AdminActions
import annotations.computed.Computed
import annotations.date.AutoNowDate
import annotations.display.DisplayFormat
import annotations.display.PanelDisplayList
import annotations.enumeration.Enumeration
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.order.DefaultOrder
import annotations.preview.Preview
import annotations.query.AdminQueries
import annotations.references.ManyToManyReferences
import annotations.references.ManyToOneReferences
import annotations.references.OneToOneReferences
import annotations.rich_editor.RichEditor
import annotations.roles.AccessRoles
import annotations.status.StatusStyle
import annotations.text_area.TextAreaField
import annotations.type.OverrideColumnType
import annotations.uploads.CustomUpload
import annotations.uploads.S3Upload
import annotations.uploads.LocalUpload
import annotations.value_mapper.ValueMapper
import models.FileDeleteStrategy
import models.actions.Action
import models.reference.EmptyColumn
import models.types.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

enum class Priority {
    Low, Medium, High
}


@AccessRoles("admin")
@AdminQueries(
    searches = ["user_id.username", "description"],
    filters = ["priority", "checked", "user_id", "test"]
)

@DefaultOrder(
    "name",
    "DESC"
)
@PanelDisplayList(
    "name", "priority", "file", "checked", "user_id"
)
@ExposedTable(
    tableName = "tasks",
    primaryKey = "id",
    singularName = "task",
    pluralName = "tasks",
    iconFile = "/static/images/tasks.png",
)
@DisplayFormat(
    format = "{id} - User: {user_id.username}"
)
object Tasks : Table("tasks") {
    @IgnoreColumn
    val id = integer("id").autoIncrement()

    @ValueMapper("test")
    val name = varchar("name", length = 150)


    @Computed(
        compute = "{name}.toLowerCase().replaceAll(' ', '-')"
    )
    val slug = varchar("slug", 500)

    @Limits(
        maxLength = 500
    )
//    @RichEditor
    @TextAreaField
    val description = text("description")

    @Enumeration("Low", "Medium", "High")
    @StatusStyle("#5ab071", "#493391", "#d62454")
    val priority = customEnumeration(
        "priority",
        "VARCHAR(50)",
        { Priority.valueOf(it as String) },
        { it.name }
    )

    @ColumnInfo("user_id", verboseName = "Users")
    @ManyToOneReferences("users", "id")
    val userId = integer("user_id").references(Users.id)

    @ManyToManyReferences("users", "tasks_users", "task_id", "user_id")
    val users = EmptyColumn()


    @ColumnInfo(
        unique = true
    )
//    @ValueMapper("timesTo2")
    val number = integer("number").default(1)

    @Preview(key = "video_preview")
    @Limits(
        maxBytes = 1024 * 1024 * 20,
//        allowedMimeTypes = ["video/mp4"]
    )
    @ColumnInfo(nullable = true)
    @LocalUpload(deleteStrategy = FileDeleteStrategy.KEEP)
    val file = varchar("file", 1000).nullable()

    @AutoNowDate
    val date = date("date").nullable()

    @ColumnInfo("test")
//    @AutoNowDate(updateOnChange = true)
    val createdAt = timestampWithTimeZone("test")

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
