package ir.amirreza

import annotations.computed.Computed
import annotations.display.DisplayFormat
import annotations.enumeration.Enumeration
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.query.AdminQueries
import annotations.references.References
import annotations.uploads.LocalUpload
import org.jetbrains.exposed.sql.*

enum class Priority {
    Low, Medium, High
}


@ExposedTable("tasks", "id", "task", "tasks")
@AdminQueries(
    searches = ["user_id.username", "description"],
    filters = ["priority", "user_id", "checked"]
)
@DisplayFormat(
    format = "{id} - User: {user_id.username}",
)
object Tasks : Table("tasks") {
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
