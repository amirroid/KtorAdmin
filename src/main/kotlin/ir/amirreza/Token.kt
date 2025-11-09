package ir.amirreza

import ir.amirroid.ktoradmin.annotations.date.AutoNowDate
import ir.amirroid.ktoradmin.annotations.exposed.ExposedTable
import ir.amirroid.ktoradmin.annotations.info.ColumnInfo
import ir.amirroid.ktoradmin.annotations.limit.Limits
import ir.amirroid.ktoradmin.annotations.query.AdminQueries
import ir.amirroid.ktoradmin.annotations.references.OneToOneReferences
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

@ExposedTable(
    "token",
    "user_id",
    "token",
    "tokens",
    groupName = "profiles"
)
@AdminQueries(
    filters = ["date"]
)
object TokenTable : Table() {
    @ColumnInfo("user_id")
    @OneToOneReferences("users", "id")
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)

    val token = text("token")

    @ColumnInfo("date")
    @Limits(
        minDateRelativeToNow = 0
    )
    @AutoNowDate(updateOnChange = true)
    val expiredAt = datetime("date")
    override val primaryKey = PrimaryKey(userId)
}