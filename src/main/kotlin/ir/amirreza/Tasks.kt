package ir.amirreza

import annotations.computed.Computed
import annotations.display.DisplayFormat
import annotations.enumeration.EnumerationColumn
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.query.QueryColumns
import annotations.references.References
import annotations.uploads.LocalUpload
import org.jetbrains.exposed.sql.*

enum class Priority {
    Low, Medium, High
}


@ExposedTable("tasks", "id", "task", "tasks")
@QueryColumns(
    searches = ["user_id.username", "description"],
)
@DisplayFormat(
    format = "{id} - User: {user_id.username}",
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

    @EnumerationColumn("Low", "Medium", "High")
    val priority = customEnumeration(
        "priority",
        "VARCHAR(50)",
        { Priority.valueOf(it as String) },
        { it.name }
    )

    @ColumnInfo("user_id")
    @References("users", "id")
    val userId = integer("user_id").references(Users.id)

    @Computed(
        compute = "{name}.toLowerCase().replaceAll(' ', '-')"
    )
    val slug = varchar("slug", 500)

    @LocalUpload
    @ColumnInfo(nullable = true)
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

    override val primaryKey = PrimaryKey(id)
}
