package ir.amirreza

import annotations.enumeration.EnumerationColumn
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.limit.ColumnLimits
import annotations.uploads.AwsS3Upload
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

enum class Priority {
    Low, Medium, High
}

@ExposedTable("tasks", "task", "tasks")
object Tasks : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 150)
    val description = text("description")

    @EnumerationColumn("Low", "Medium", "High")
    val priority = customEnumeration(
        "priority",
        "VARCHAR(50)",
        { Priority.valueOf(it as String) },
        { it.name }
    )

    @ColumnInfo("user_id")
    val userId = integer("user_id")

    override val primaryKey = PrimaryKey(id)
}