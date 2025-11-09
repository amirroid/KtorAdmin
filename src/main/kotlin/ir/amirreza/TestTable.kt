package ir.amirreza

import ir.amirroid.ktoradmin.annotations.exposed.ExposedTable
import ir.amirroid.ktoradmin.annotations.info.ColumnInfo
import ir.amirroid.ktoradmin.annotations.info.IgnoreColumn
import ir.amirroid.ktoradmin.annotations.references.OneToOneReferences
import org.jetbrains.exposed.sql.ReferenceOption
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
//        nullable = true
    )
    val taskId = integer("task_id").references(Tasks.id, onDelete = ReferenceOption.SET_NULL).nullable()

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(id)
}