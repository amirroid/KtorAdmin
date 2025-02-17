package ir.amirreza.ref

import annotations.info.ColumnInfo
import org.jetbrains.exposed.sql.Table

object TaskUsersCrossRef : Table("tasks_users") {
    @ColumnInfo("task_id")
    val taskId = integer("task_id")

    @ColumnInfo("user_id")
    val userId = integer("user_id")

    override val primaryKey: PrimaryKey = PrimaryKey(taskId, userId)
}