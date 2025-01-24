package ir.amirreza

import annotations.display.DisplayFormat
import annotations.exposed.ExposedTable
import annotations.info.IgnoreColumn
import annotations.limit.Limits
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
object Users : Table() {
    @IgnoreColumn
    val id = integer("id").autoIncrement()
    val username = varchar("username", length = 100)

    @Limits(
        regexPattern = """[A-z0-9]*@[A-z0-9]*.[A-z0-9]*"""
    )
    val email = varchar("email", length = 150)
    val password = text("password")

    override val primaryKey = PrimaryKey(id)
}