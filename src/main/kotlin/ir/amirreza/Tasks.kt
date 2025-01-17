package ir.amirreza

import annotations.enumeration.EnumerationColumn
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.limit.ColumnLimits
import annotations.uploads.AwsS3Upload
import org.jetbrains.exposed.sql.Table

enum class Priority {
    Low, Medium, High
}

@ExposedTable("task", "tasks")
object Tasks : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 150)

    @ColumnLimits(
        maxLength = 500,
        regexPattern = "."
    )
    val description = text("description")
    @EnumerationColumn("Low", "Medium", "High")
    val priority = customEnumeration(
        "priority",
        "VARCHAR(50)",
        { Priority.valueOf(it as String) },
        { it.name }
    )
    val isDone = bool("is_done")
    @AwsS3Upload
    val file = text("file")

    @ColumnInfo("user_id", defaultValue = "2")
    val userId = integer("user_id")

    override val primaryKey = PrimaryKey(id)
}