package ir.amirreza

import annotations.confirmation.Confirmation
import annotations.display.DisplayFormat
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.order.DefaultOrder
import annotations.references.ManyToManyReferences
import annotations.value_mapper.ValueMapper
import models.reference.EmptyColumn
import org.jetbrains.exposed.sql.Table

@ExposedTable(
    "users",
    "id",
    "user",
    "users",
    "profiles"
)
@DisplayFormat(
    format = "{id} - {username}"
)
@DefaultOrder("id", "DESC")
object Users : Table() {
    @IgnoreColumn
    val id = integer("id").autoIncrement()
    val username = varchar("username", length = 100)

    @Limits(
        regexPattern = """[A-z0-9]*@[A-z0-9]*.[A-z0-9]*"""
    )
    val email = varchar("email", length = 150)

    @Confirmation
    @ValueMapper(key = "password")
    val password = text("password")


    @ManyToManyReferences("tasks", "tasks_users", "user_id", "task_id")
    val tasks = EmptyColumn()

    override val primaryKey = PrimaryKey(id)
}