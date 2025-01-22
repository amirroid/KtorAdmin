package ir.amirreza

import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.ColumnLimits
import annotations.references.References
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.time

@ExposedTable(
    "token",
    "user_id",
    "token",
    "tokens",
    groupName = "profiles"
)
object TokenTable : Table() {
    @ColumnInfo("user_id")
    @References("users", "id")
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val token = text("token")
    @ColumnInfo("date")
    @ColumnLimits(
        minDateRelativeToNow = 0
    )
    val expiredAt = datetime("date")
    override val primaryKey = PrimaryKey(userId)
}