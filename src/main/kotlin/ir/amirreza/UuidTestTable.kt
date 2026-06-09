package ir.amirreza

import ir.amirreza.uuid_table.AdminUuidTable
import ir.amirroid.ktoradmin.annotations.exposed.ExposedTable
import ir.amirroid.ktoradmin.annotations.info.ColumnInfo
import ir.amirroid.ktoradmin.annotations.info.IgnoreColumns
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@ExposedTable(
    tableName = "test_t",
    primaryKey = "id"
)
@IgnoreColumns(["id"])
object UuidTestTable : IntIdTable("test_t") {
    @ColumnInfo("test")
    val uuid = uuid("test")
}

class UuidTestService(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(UuidTestTable)
        }
    }
}