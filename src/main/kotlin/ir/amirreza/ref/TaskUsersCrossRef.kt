package ir.amirreza.ref

import ir.amirroid.ktoradmin.annotations.exposed.ExposedTable
import ir.amirroid.ktoradmin.annotations.info.ColumnInfo
import org.jetbrains.exposed.sql.Table

@ExposedTable("tasks_users", "task_id", showInAdminPanel = false)
object TaskUsersCrossRef : Table("tasks_users") {
    @ColumnInfo("task_id")
    val taskId = integer("task_id")

    @ColumnInfo("user_id")
    val userId = integer("user_id")

    override val primaryKey: PrimaryKey = PrimaryKey(taskId, userId)
}