package ir.amirreza.panels

import models.*
import models.actions.Action
import models.common.Reference
import models.order.Order
import models.types.ColumnType
import panels.AdminJdbcTable

class CustomManualPanel : AdminJdbcTable {
    override fun getSearches(): List<String> = listOf()
    override fun getFilters(): List<String> = listOf()
    override fun getDefaultOrder(): Order? = null
    override fun getAccessRoles(): List<String>? = null
    override fun getCustomActions(): List<String> = listOf()
    override fun getDefaultActions(): List<Action> = listOf(Action.ADD, Action.EDIT, Action.DELETE)
    override fun getDisplayFormat(): String? = null
    override fun isShowInAdminPanel(): Boolean = true
    override fun getPrimaryKey(): String = "id"
    override fun getIconFile(): String? = null
    override fun getDatabaseKey(): String? = null
    override fun getTableName(): String = "post"
    override fun getPanelListColumns(): List<String> {
        return emptyList()
    }

    override fun getGroupName(): String? = null
    override fun getPluralName(): String = "Posts2"
    override fun getSingularName(): String = "Post"

    override fun getAllColumns(): List<ColumnSet> = listOf(
        ColumnSet(
            columnName = "id",
            verboseName = "id",
            type = ColumnType.LONG,
            nullable = false,
            showInPanel = false,
            readOnly = false,
            blank = true,
            unique = false
        ),
        ColumnSet(
            columnName = "titleContent",
            verboseName = "titleContent",
            type = ColumnType.STRING,
            nullable = false,
            showInPanel = true,
            readOnly = false,
            blank = true,
            unique = false
        ),
        ColumnSet(
            columnName = "content",
            verboseName = "content",
            type = ColumnType.STRING,
            nullable = false,
            showInPanel = true,
            readOnly = false,
            blank = true,
            unique = false
        ),
        ColumnSet(
            columnName = "priority",
            verboseName = "Priority",
            type = ColumnType.ENUMERATION,
            nullable = false,
            showInPanel = true,
            enumerationValues = listOf("Low", "Medium", "High"),
            readOnly = false,
            blank = true,
            unique = false
        ),
        ColumnSet(
            columnName = "file",
            verboseName = "file",
            type = ColumnType.FILE,
            nullable = true,
            showInPanel = true,
            uploadTarget = UploadTarget.LocalFile(path = null),
            readOnly = false,
            blank = true,
            unique = false
        ),
        ColumnSet(
            columnName = "author_id",
            verboseName = "author_id",
            type = ColumnType.LONG,
            nullable = false,
            showInPanel = true,
            reference = Reference.ManyToOne(
                relatedTable = "authors",
                foreignKey = "id"
            ),
            readOnly = false,
            blank = true,
            unique = false
        )
    )
}