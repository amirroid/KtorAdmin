package ir.amirroid.ktoradmin.modules.update

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import ir.amirroid.ktoradmin.TestJdbcTable
import ir.amirroid.ktoradmin.column
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.csrf.CSRF_TOKEN_HEADER_NAME
import ir.amirroid.ktoradmin.csrf.CsrfManager
import ir.amirroid.ktoradmin.hikra.KtorAdminHikariCP
import ir.amirroid.ktoradmin.listener.AdminEventListener
import ir.amirroid.ktoradmin.models.actions.Action
import ir.amirroid.ktoradmin.models.events.ColumnEvent
import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.modules.configureSavesRouting
import ir.amirroid.ktoradmin.modules.configureTemplating
import ir.amirroid.ktoradmin.ratelimit.configureRateLimit
import java.util.UUID
import kotlin.test.*

class InlineEditingTest {
    private lateinit var dataSource: HikariDataSource
    private lateinit var productsTable: TestJdbcTable
    private lateinit var readOnlyTable: TestJdbcTable
    private lateinit var noEditActionTable: TestJdbcTable

    private val idCol = column("id", ColumnType.INTEGER, showInPanel = true)
    private val nameCol = column("name", ColumnType.STRING, showInPanel = true)
    private val priceCol = column("price", ColumnType.INTEGER, showInPanel = true)
    private val activeCol = column("active", ColumnType.BOOLEAN, showInPanel = true)
    private val readOnlyCol = column("code", ColumnType.STRING, showInPanel = true) { copy(readOnly = true) }

    @BeforeTest
    fun setup() {
        DynamicConfiguration.adminPath = "admin"

        val dbName = "test_db_" + UUID.randomUUID().toString().replace("-", "")
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1"
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 5
        }
        dataSource = HikariDataSource(config)
        KtorAdminHikariCP.defaultCustom(dataSource)

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE products (
                        id INT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        price INT NOT NULL,
                        active BOOLEAN NOT NULL,
                        code VARCHAR(255) NOT NULL
                    )
                    """.trimIndent()
                )
                stmt.execute(
                    """
                    INSERT INTO products (id, name, price, active, code)
                    VALUES (1, 'Laptop', 999, true, 'SKU-001')
                    """.trimIndent()
                )
            }
        }

        productsTable = TestJdbcTable(
            columns = listOf(idCol, nameCol, priceCol, activeCol, readOnlyCol),
            tableName = "products",
            pluralName = "Products",
            defaultActions = listOf(Action.ADD, Action.EDIT, Action.DELETE),
        )

        readOnlyTable = TestJdbcTable(
            columns = listOf(idCol, readOnlyCol),
            tableName = "products",
            pluralName = "ReadOnlyProducts",
            defaultActions = listOf(Action.ADD, Action.EDIT, Action.DELETE),
        )

        noEditActionTable = TestJdbcTable(
            columns = listOf(idCol, nameCol),
            tableName = "products",
            pluralName = "NoEditProducts",
            defaultActions = listOf(Action.ADD, Action.DELETE),
        )
    }

    @AfterTest
    fun tearDown() {
        DynamicConfiguration.currentEventListener = null
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }

    private fun Application.setupTestRouting(tables: List<TestJdbcTable>) {
        configureRateLimit()
        configureTemplating()
        routing {
            configureSavesRouting(tables, authenticateName = null)
        }
    }

    @Test
    fun `should successfully inline update a string column via JSON PATCH`() = testApplication {
        application { setupTestRouting(listOf(productsTable)) }

        val validCsrf = CsrfManager.generateToken()
        val response = client.patch("/admin/resources/Products/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(CSRF_TOKEN_HEADER_NAME, validCsrf)
            setBody("{\"field\": \"name\", \"value\": \"Gaming Laptop\"}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("status"))
        assertTrue(body.contains("success"))
        assertTrue(body.contains("name"))
        assertTrue(body.contains("Gaming Laptop"))

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT name FROM products WHERE id = 1")
                assertTrue(rs.next())
                assertEquals("Gaming Laptop", rs.getString("name"))
            }
        }
    }

    @Test
    fun `should successfully inline update an integer column via JSON PATCH`() = testApplication {
        application { setupTestRouting(listOf(productsTable)) }

        val validCsrf = CsrfManager.generateToken()
        val response = client.patch("/admin/resources/Products/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(CSRF_TOKEN_HEADER_NAME, validCsrf)
            setBody("{\"field\": \"price\", \"value\": \"1299\"}")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT price FROM products WHERE id = 1")
                assertTrue(rs.next())
                assertEquals(1299, rs.getInt("price"))
            }
        }
    }

    @Test
    fun `should successfully inline update a boolean column via JSON PATCH`() = testApplication {
        application { setupTestRouting(listOf(productsTable)) }

        val validCsrf = CsrfManager.generateToken()
        val response = client.patch("/admin/resources/Products/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(CSRF_TOKEN_HEADER_NAME, validCsrf)
            setBody("{\"field\": \"active\", \"value\": \"false\"}")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT active FROM products WHERE id = 1")
                assertTrue(rs.next())
                assertFalse(rs.getBoolean("active"))
            }
        }
    }

    @Test
    fun `should successfully inline update via form-urlencoded PATCH`() = testApplication {
        application { setupTestRouting(listOf(productsTable)) }

        val validCsrf = CsrfManager.generateToken()
        val response = client.patch("/admin/resources/Products/1") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            header(CSRF_TOKEN_HEADER_NAME, validCsrf)
            setBody("field=name&value=Ultrabook")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT name FROM products WHERE id = 1")
                assertTrue(rs.next())
                assertEquals("Ultrabook", rs.getString("name"))
            }
        }
    }

    @Test
    fun `should reject PATCH with invalid CSRF token`() = testApplication {
        application { setupTestRouting(listOf(productsTable)) }

        val response = client.patch("/admin/resources/Products/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(CSRF_TOKEN_HEADER_NAME, "invalid-csrf-token")
            setBody("{\"field\": \"name\", \"value\": \"Hacked Laptop\"}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Invalid CSRF token"))
    }

    @Test
    fun `should reject PATCH on read-only column`() = testApplication {
        application { setupTestRouting(listOf(productsTable)) }

        val validCsrf = CsrfManager.generateToken()
        val response = client.patch("/admin/resources/Products/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(CSRF_TOKEN_HEADER_NAME, validCsrf)
            setBody("{\"field\": \"code\", \"value\": \"NEW-SKU\"}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("cannot be edited inline"))
    }

    @Test
    fun `should reject PATCH when edit action is disabled`() = testApplication {
        application { setupTestRouting(listOf(noEditActionTable)) }

        val validCsrf = CsrfManager.generateToken()
        val response = client.patch("/admin/resources/NoEditProducts/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(CSRF_TOKEN_HEADER_NAME, validCsrf)
            setBody("{\"field\": \"name\", \"value\": \"Should Fail\"}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Edit action is disabled"))
    }

    @Test
    fun `should reject PATCH with invalid data type value`() = testApplication {
        application { setupTestRouting(listOf(productsTable)) }

        val validCsrf = CsrfManager.generateToken()
        val response = client.patch("/admin/resources/Products/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(CSRF_TOKEN_HEADER_NAME, validCsrf)
            setBody("{\"field\": \"price\", \"value\": \"not-a-number\"}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `should notify event listener on inline update`() = testApplication {
        application { setupTestRouting(listOf(productsTable)) }

        var receivedEvent: ColumnEvent? = null
        var updatedTable: String? = null
        var updatedPk: String? = null

        DynamicConfiguration.currentEventListener = object : AdminEventListener() {
            override suspend fun onUpdateJdbcData(tableName: String, objectPrimaryKey: String, events: List<ColumnEvent>) {
                updatedTable = tableName
                updatedPk = objectPrimaryKey
                receivedEvent = events.firstOrNull()
            }
        }

        val validCsrf = CsrfManager.generateToken()
        val response = client.patch("/admin/resources/Products/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(CSRF_TOKEN_HEADER_NAME, validCsrf)
            setBody("{\"field\": \"name\", \"value\": \"EventListenerTest\"}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("products", updatedTable)
        assertEquals("1", updatedPk)
        assertNotNull(receivedEvent)
        assertEquals("name", receivedEvent?.columnSet?.columnName)
        assertEquals("EventListenerTest", receivedEvent?.value)
    }
}
