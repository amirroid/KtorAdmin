package ir.amirreza.category

import annotations.display.DisplayFormat
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.query.AdminQueries
import annotations.references.ManyToOneReferences
import annotations.text_area.TextAreaField
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

@ExposedTable(
    tableName = "category",
    primaryKey = "id",
    singularName = "Category",
    pluralName = "Categories",
    groupName = "Products"
)
@AdminQueries(
    searches = ["name", "description"],
    filters = ["parent_category_id"]
)
@DisplayFormat(
    format = "{parent_category_id.name} -> {id}: {name}"
)
object Category : Table() {
    @IgnoreColumn
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100).uniqueIndex()

    @TextAreaField
    val description = text("description").nullable()

    @ManyToOneReferences("category", "id")
    @ColumnInfo("parent_category_id", nullable = true)
    val parentCategoryId = integer("parent_category_id").references(id, onDelete = ReferenceOption.SET_NULL).nullable()

    override val primaryKey = PrimaryKey(id)
}