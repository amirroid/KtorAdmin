//package ir.amirroid.ktoradmin.repository
//
//import ir.amirroid.ktoradmin.models.actions.Action
//import ir.amirroid.ktoradmin.models.field.FieldSet
//import ir.amirroid.ktoradmin.models.order.Order
//import ir.amirroid.ktoradmin.models.types.FieldType
//import ir.amirroid.ktoradmin.mongo.MongoServerAddress
//import ir.amirroid.ktoradmin.panels.AdminMongoCollection
//import kotlinx.coroutines.runBlocking
//import kotlin.test.AfterTest
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertFailsWith
//
//class MongoClientRepositoryTest {
//    @AfterTest
//    fun tearDown() {
//        MongoClientRepository.closeAllConnections()
//    }
//
//    @Test
//    fun `should normalize null mongo database key to default`() {
//        assertEquals("default", MongoClientRepository.getActualKey(null))
//        assertEquals("analytics", MongoClientRepository.getActualKey("analytics"))
//    }
//
//    @Test
//    fun `should fail repository reads when mongo client is not registered`() = runBlocking {
//        val panel = mongoPanel(databaseKey = "missing")
//
//        val exception = assertFailsWith<IllegalArgumentException> {
//            MongoClientRepository.getCount(panel, com.mongodb.client.model.Filters.empty())
//        }
//
//        assertEquals("Client for database key missing not found.", exception.message)
//    }
//
//    @Test
//    fun `should fail object id operations for malformed primary key before database access`() = runBlocking {
//        val panel = mongoPanel(databaseKey = "missing", primaryKey = "_id", primaryKeyType = FieldType.ObjectId)
//
//        assertFailsWith<IllegalArgumentException> {
//            MongoClientRepository.deleteRows(panel, listOf("not-an-object-id"))
//        }
//    }
//
//    @Test
//    fun `should allow repeated registration attempts for same key without throwing`() {
//        MongoClientRepository.registerNewClient("unit-key", "db", MongoServerAddress("127.0.0.1", 27017))
//        MongoClientRepository.registerNewClient("unit-key", "db", MongoServerAddress("127.0.0.1", 27017))
//    }
//
//    private fun mongoPanel(
//        databaseKey: String?,
//        primaryKey: String = "id",
//        primaryKeyType: FieldType = FieldType.String,
//    ) = object : AdminMongoCollection {
//        private val fields = listOf(
//            FieldSet(primaryKey, type = primaryKeyType),
//            FieldSet("name", type = FieldType.String),
//        )
//
//        override fun getAllFields(): List<FieldSet> = fields
//        override fun getCollectionName(): String = "items"
//        override fun getPanelListFields(): List<String> = listOf("id", "name")
//        override fun getSingularName(): String = "Item"
//        override fun getPluralName(): String = "Items"
//        override fun getGroupName(): String? = null
//        override fun getDatabaseKey(): String? = databaseKey
//        override fun getPrimaryKey(): String = primaryKey
//        override fun getDisplayFormat(): String? = null
//        override fun getSearches(): List<String> = emptyList()
//        override fun getFilters(): List<String> = emptyList()
//        override fun getAccessRoles(): List<String>? = null
//        override fun getDefaultOrder(): Order? = null
//        override fun getDefaultActions(): List<Action> = emptyList()
//        override fun getCustomActions(): List<String> = emptyList()
//        override fun getIconFile(): String? = null
//        override fun isShowInAdminPanel(): Boolean = true
//    }
//}
