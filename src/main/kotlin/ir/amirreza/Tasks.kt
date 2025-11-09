package ir.amirreza

import ir.amirroid.ktoradmin.annotations.computed.Computed
import ir.amirroid.ktoradmin.annotations.date.AutoNowDate
import ir.amirroid.ktoradmin.annotations.display.DisplayFormat
import ir.amirroid.ktoradmin.annotations.display.PanelDisplayList
import ir.amirroid.ktoradmin.annotations.enumeration.Enumeration
import ir.amirroid.ktoradmin.annotations.exposed.ExposedTable
import ir.amirroid.ktoradmin.annotations.info.ColumnInfo
import ir.amirroid.ktoradmin.annotations.info.IgnoreColumn
import ir.amirroid.ktoradmin.annotations.limit.Limits
import ir.amirroid.ktoradmin.annotations.order.DefaultOrder
import ir.amirroid.ktoradmin.annotations.preview.Preview
import ir.amirroid.ktoradmin.annotations.query.AdminQueries
import ir.amirroid.ktoradmin.annotations.references.ManyToManyReferences
import ir.amirroid.ktoradmin.annotations.references.ManyToOneReferences
import ir.amirroid.ktoradmin.annotations.roles.AccessRoles
import ir.amirroid.ktoradmin.annotations.status.StatusStyle
import ir.amirroid.ktoradmin.annotations.text_area.TextAreaField
import ir.amirroid.ktoradmin.annotations.uploads.LocalUpload
import ir.amirroid.ktoradmin.annotations.value_mapper.ValueMapper
import ir.amirroid.ktoradmin.models.FileDeleteStrategy
import ir.amirroid.ktoradmin.models.reference.EmptyColumn
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
