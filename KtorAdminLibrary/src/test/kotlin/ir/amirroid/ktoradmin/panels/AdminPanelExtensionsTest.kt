package ir.amirroid.ktoradmin.panels

import ir.amirroid.ktoradmin.TestJdbcTable
import ir.amirroid.ktoradmin.adminPanel
import ir.amirroid.ktoradmin.column
import ir.amirroid.ktoradmin.configuration.KtorAdminConfiguration
import ir.amirroid.ktoradmin.models.actions.Action
import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.models.date.AutoNowDate
import ir.amirroid.ktoradmin.models.types.ColumnType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminPanelExtensionsTest {
    @Test
    fun `should find panel by plural name`() {
        val panels = listOf(adminPanel("Users"), adminPanel("Posts"))

        assertEquals("Posts", panels.findWithPluralName("Posts")?.getPluralName())
        assertEquals(null, panels.findWithPluralName("Missing"))
    }

    @Test
    fun `should expose add and edit default action flags`() {
        val panel = adminPanel("Users", defaultActions = listOf(Action.ADD, Action.EDIT))

        assertTrue(panel.hasAddAction)
        assertTrue(panel.hasEditAction)
        assertFalse(adminPanel("Logs").hasAddAction)
    }

    @Test
    fun `should include registered custom actions and delete action`() {
        val action = object : ir.amirroid.ktoradmin.action.CustomAdminAction {
            override var key: String = "EXPORT"
            override val displayText: String = "Export"
            override suspend fun performAction(name: String, selectedIds: List<String>) = Unit
        }
        KtorAdminConfiguration().registerCustomAdminAction(action)
        val panel = adminPanel("Users", customActions = listOf("EXPORT"), defaultActions = listOf(Action.DELETE))

        val actions = panel.getAllCustomActions(deleteActionDisplayText = "Delete")

        assertTrue(actions.map { it.key }.containsAll(listOf("EXPORT", "DELETE")))
        assertEquals("DELETE", actions.last().key)
    }

    @Test
    fun `should fail when panel references unregistered custom action`() {
        val panel = adminPanel("Users", customActions = listOf("MISSING"))

        val exception = assertFailsWith<IllegalStateException> { panel.getAllCustomActions() }

        assertTrue(exception.message!!.contains("One or more custom actions are not registered"))
    }

    @Test
    fun `should filter jdbc columns for list and upsert screens`() {
        val id = column("id", ColumnType.INTEGER)
        val hidden = column("secret", showInPanel = false)
        val manyToMany = column("roles") { copy(reference = Reference.ManyToMany("roles", "user_roles", "user_id", "role_id")) }
        val autoNow = column("updated_at", ColumnType.DATETIME) { copy(autoNowDate = AutoNowDate(updateOnChange = true)) }
        val table = TestJdbcTable(columns = listOf(id, hidden, manyToMany, autoNow), panelListColumns = listOf("id", "secret", "roles", "updated_at"))

        assertEquals(listOf("id", "updated_at"), table.getAllAllowToShowColumns().map { it.columnName })
        assertEquals(listOf("id"), table.getAllAllowToShowColumnsInUpsert().map { it.columnName })
        assertEquals(listOf("id", "roles"), table.getAllAllowToShowColumnsInUpsertView().map { it.columnName })
        assertEquals(listOf("updated_at"), table.getAllAutoNowDateUpdateColumns().map { it.columnName })
    }
}
