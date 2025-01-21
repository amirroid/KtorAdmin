package ir.amirreza

import annotations.computed_column.ComputedColumn
import annotations.display.TableDisplayFormat
import annotations.enumeration.EnumerationColumn
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.ColumnLimits
import annotations.query.QueryColumns
import annotations.references.References
import annotations.uploads.AwsS3Upload
import annotations.uploads.LocalUpload
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

enum class Priority {
    Low, Medium, High
}


@ExposedTable("tasks", "id", "task", "tasks")
@QueryColumns(
    searches = ["user_id.username", "description"],
)
@TableDisplayFormat(
    format = "{id} - User: {user_id.username}",
)
object Tasks : Table() {
    @IgnoreColumn
    val id = integer("id").autoIncrement()

    @ColumnLimits(
        maxLength = 20,
    )
    val name = varchar("name", length = 150)

    @ColumnLimits(
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

//    @ComputedColumn(
//        compute = "{name}.toLowerCase().replaceAll(' ', '-')"
//    )
//    val slug = varchar("slug", 500)

    @LocalUpload
    @ColumnInfo(nullable = true)
    @ColumnLimits(
        maxBytes = 1024 * 1024
    )
    val file = varchar("file", 1000).nullable()

    override val primaryKey = PrimaryKey(id)
}