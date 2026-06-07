package ir.amirroid.ktoradmin.filters

import io.ktor.http.Parameters
import ir.amirroid.ktoradmin.models.actions.Action
import ir.amirroid.ktoradmin.models.field.FieldSet
import ir.amirroid.ktoradmin.models.filters.FilterTypes
import ir.amirroid.ktoradmin.models.order.Order
import ir.amirroid.ktoradmin.models.types.FieldType
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MongoFiltersTest {
    @Test
    fun `should create mongo filter metadata for supported fields`() {
        val fields =
            listOf(
                FieldSet("created", type = FieldType.Date),
                FieldSet("updated", type = FieldType.DateTime),
                FieldSet("status", type = FieldType.Enumeration, enumerationValues = listOf("ACTIVE")),
                FieldSet("enabled", type = FieldType.Boolean),
            )
        val panel = mongoPanel(fields = fields, filters = fields.map { it.fieldName!! })

        val filters = MongoFilters.findFiltersData(panel)

        assertEquals(listOf(FilterTypes.DATE, FilterTypes.DATETIME, FilterTypes.ENUMERATION, FilterTypes.BOOLEAN), filters.map { it.type })
        assertEquals(listOf("ACTIVE"), filters[2].values)
    }

    @Test
    fun `should reject unsupported mongo filter metadata`() {
        val panel = mongoPanel(fields = listOf(FieldSet("name", type = FieldType.String)), filters = listOf("name"))

        assertFailsWith<IllegalArgumentException> { MongoFilters.findFiltersData(panel) }
    }

    @Test
    fun `should extract empty mongo bson filter when no parameters match`() {
        val panel = mongoPanel(fields = listOf(FieldSet("enabled", type = FieldType.Boolean)))

        val filter = MongoFilters.extractMongoFilters(panel, Parameters.Empty)

        assertTrue(filter.toBsonDocument().isEmpty())
    }

    private fun mongoPanel(
        fields: List<FieldSet>,
        filters: List<String> = emptyList(),
    ) = object : AdminMongoCollection {
        override fun getAllFields(): List<FieldSet> = fields

        override fun getCollectionName(): String = "items"

        override fun getPanelListFields(): List<String> = fields.mapNotNull { it.fieldName }

        override fun getSingularName(): String = "Item"

        override fun getPluralName(): String = "Items"

        override fun getGroupName(): String? = null

        override fun getDatabaseKey(): String? = null

        override fun getPrimaryKey(): String = "_id"

        override fun getDisplayFormat(): String? = null

        override fun getSearches(): List<String> = emptyList()

        override fun getFilters(): List<String> = filters

        override fun getAccessRoles(): List<String>? = null

        override fun getDefaultOrder(): Order? = null

        override fun getDefaultActions(): List<Action> = emptyList()

        override fun getCustomActions(): List<String> = emptyList()

        override fun getIconFile(): String? = null

        override fun isShowInAdminPanel(): Boolean = true
    }
}
