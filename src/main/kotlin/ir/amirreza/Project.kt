package ir.amirreza

import ir.amirroid.ktoradmin.annotations.exposed.ExposedTable
import ir.amirroid.ktoradmin.annotations.info.ColumnInfo
import ir.amirroid.ktoradmin.annotations.info.IgnoreColumns
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction


@ExposedTable(
    tableName = "projects",
    primaryKey = "id"
)
@IgnoreColumns(columnNames = ["id"])
object Projects : UuidTable("projects") {
    val name = varchar("name", 100)
    val description = text("description")

    @ColumnInfo("is_active")
    val isActive = bool("is_active").default(true)
    val budget = decimal("budget", 12, 2)

    @ColumnInfo("created_at")
    val createdAt = timestamp("created_at")
}


class ProjectsService(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(Projects)
        }
    }
}