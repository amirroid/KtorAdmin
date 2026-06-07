package ir.amirroid.ktoradmin

import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.actions.Action
import ir.amirroid.ktoradmin.models.order.Order
import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.panels.AdminJdbcTable
import ir.amirroid.ktoradmin.panels.AdminPanel

internal class TestJdbcTable(
    private val columns: Collection<ColumnSet> = emptyList(),
    private val tableName: String = "test_table",
    private val panelListColumns: List<String> = columns.map { it.columnName },
    private val filters: List<String> = emptyList(),
    private val searches: List<String> = emptyList(),
    private val displayFormat: String? = null,
    private val defaultOrder: Order? = null,
    private val databaseKey: String? = null,
    private val defaultActions: List<Action> = emptyList(),
    private val customActions: List<String> = emptyList(),
) : AdminJdbcTable {
    override fun getAllColumns(): Collection<ColumnSet> = columns

    override fun getTableName(): String = tableName

    override fun getPanelListColumns(): List<String> = panelListColumns

    override fun getSingularName(): String = "Test"

    override fun getPluralName(): String = "Tests"

    override fun getGroupName(): String? = null

    override fun getDatabaseKey(): String? = databaseKey

    override fun getPrimaryKey(): String = "id"

    override fun getDisplayFormat(): String? = displayFormat

    override fun getSearches(): List<String> = searches

    override fun getFilters(): List<String> = filters

    override fun getAccessRoles(): List<String>? = null

    override fun getDefaultOrder(): Order? = defaultOrder

    override fun getDefaultActions(): List<Action> = defaultActions

    override fun getCustomActions(): List<String> = customActions

    override fun getIconFile(): String? = null

    override fun isShowInAdminPanel(): Boolean = true
}

internal fun column(
    name: String,
    type: ColumnType = ColumnType.STRING,
    showInPanel: Boolean = true,
    nullable: Boolean = false,
    extra: ColumnSet.() -> ColumnSet = { this },
): ColumnSet =
    ColumnSet(
        columnName = name,
        verboseName = name.replaceFirstChar { it.uppercase() },
        type = type,
        showInPanel = showInPanel,
        nullable = nullable,
    ).extra()

internal fun adminPanel(
    pluralName: String,
    customActions: List<String> = emptyList(),
    defaultActions: List<Action> = emptyList(),
): AdminPanel =
    object : AdminPanel {
        override fun getSingularName(): String = pluralName.removeSuffix("s")

        override fun getPluralName(): String = pluralName

        override fun getGroupName(): String? = null

        override fun getDatabaseKey(): String? = null

        override fun getPrimaryKey(): String = "id"

        override fun getDisplayFormat(): String? = null

        override fun getSearches(): List<String> = emptyList()

        override fun getFilters(): List<String> = emptyList()

        override fun getAccessRoles(): List<String>? = null

        override fun getDefaultOrder(): Order? = null

        override fun getDefaultActions(): List<Action> = defaultActions

        override fun getCustomActions(): List<String> = customActions

        override fun getIconFile(): String? = null

        override fun isShowInAdminPanel(): Boolean = true
    }
