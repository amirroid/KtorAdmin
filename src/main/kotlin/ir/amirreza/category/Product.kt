package ir.amirreza.category

import annotations.date.AutoNowDate
import annotations.display.DisplayFormat
import annotations.display.PanelDisplayList
import annotations.exposed.ExposedTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.preview.Preview
import annotations.query.AdminQueries
import annotations.references.ManyToOneReferences
import annotations.rich_editor.RichEditor
import annotations.type.OverrideColumnType
import annotations.uploads.LocalUpload
import models.types.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

@ExposedTable(
    "product",
    "id",
    groupName = "Products"
)
@PanelDisplayList(
    "name",
    "price",
    "stock_quantity",
)
@AdminQueries(
    searches = ["name"],
    filters = ["category_id"]
)
@DisplayFormat(
    format = "{name}: {price}$"
)
object Product : Table() {
    @IgnoreColumn
    val id = integer("id").autoIncrement()

    @Limits(
        maxLength = 255
    )
    val name = varchar("name", 255)

    @RichEditor
    val description = text("description")

    val price = long("price")

    @ColumnInfo("stock_quantity", defaultValue = "0")
    val stockQuantity = integer("stock_quantity").default(0)

    @ColumnInfo("image_url", nullable = true)
    @LocalUpload
    @Preview("image")
    val imageUrl = varchar("image_url", 255).nullable()

    @ColumnInfo(
        "category_id",
        verboseName = "Category"
    )
    @ManyToOneReferences("category", "id")
    val categoryId = integer("category_id").references(Category.id)

    @ColumnInfo("created_at", verboseName = "Created at")
    @AutoNowDate
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    @ColumnInfo("updated_at")
    @AutoNowDate(updateOnChange = true)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}