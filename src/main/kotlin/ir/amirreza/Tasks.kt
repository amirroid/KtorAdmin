package ir.amirreza

import annotations.enumeration.EnumerationColumn
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.ColumnLimits
import annotations.references.References
import annotations.uploads.AwsS3Upload
import annotations.uploads.LocalUpload
import jdk.nashorn.internal.ir.annotations.Reference
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

enum class Priority {
    Low, Medium, High
}

@ExposedTable("tasks", "id", "task", "tasks")
object Tasks : Table() {
    @IgnoreColumn
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
    @References("users", "id")
    val userId = integer("user_id").references(Users.id)

    @LocalUpload("tasks/")
    @ColumnInfo(nullable = true)
    val file = varchar("file", 1000).nullable()

    override val primaryKey = PrimaryKey(id)
}