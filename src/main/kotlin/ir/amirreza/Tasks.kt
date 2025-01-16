package ir.amirreza

import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import org.jetbrains.exposed.sql.Table

enum class Priority {
    Low, Medium, High
}

@ExposedTable("task", "tasks")
object Tasks : Table() {
    val id = integer("id").autoIncrement()
    @ColumnInfo("name", false)
    val name = varchar("name", length = 150)
    val description = text("description")
    val priority = customEnumeration(
        "priority",
        "VARCHAR(50)",
        { Priority.valueOf(it as String) },
        { it.name }
    )
    val userId = integer("user_id")

    override val primaryKey = PrimaryKey(id)
}