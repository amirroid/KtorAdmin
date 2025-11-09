package ir.amirreza

import ir.amirroid.ktoradmin.annotations.confirmation.Confirmation
import ir.amirroid.ktoradmin.annotations.display.DisplayFormat
import ir.amirroid.ktoradmin.annotations.exposed.ExposedTable
import ir.amirroid.ktoradmin.annotations.info.IgnoreColumn
import ir.amirroid.ktoradmin.annotations.limit.Limits
import ir.amirroid.ktoradmin.annotations.order.DefaultOrder
import ir.amirroid.ktoradmin.annotations.references.ManyToManyReferences
import ir.amirroid.ktoradmin.models.reference.EmptyColumn
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
    val password = text("password")


    @ManyToManyReferences("tasks", "tasks_users", "user_id", "task_id")
    val tasks = EmptyColumn()

    override val primaryKey = PrimaryKey(id)
}