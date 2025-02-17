package ir.amirreza

import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.references.OneToOneReferences
import org.jetbrains.exposed.sql.Table

@ExposedTable(
    tableName = "test",
    primaryKey = "id"
)
object TestTable : Table("test") {
    @IgnoreColumn
    val id = integer("id").autoIncrement()
    @OneToOneReferences(
        "tasks",
        "id"
    )
    @ColumnInfo(
        "task_id",
    )
    val taskId = integer("task_id") references Tasks.id

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(id)
}