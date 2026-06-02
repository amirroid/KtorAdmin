package ir.amirroid.ktoradmin.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import ir.amirroid.ktoradmin.TestJdbcTable
import ir.amirroid.ktoradmin.hikra.KtorAdminHikariCP
import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.models.order.Order
import ir.amirroid.ktoradmin.models.types.ColumnType
import java.sql.Connection
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JdbcQueriesRepositoryIntegrationTest {
    private lateinit var dataSource: HikariDataSource
    private lateinit var usersTable: TestJdbcTable
    private lateinit var rolesTable: TestJdbcTable
    private lateinit var userRolesTable: TestJdbcTable

    private val id = column("id", ColumnType.INTEGER, showInPanel = true)
    private val name = column("name", ColumnType.STRING)
    private val age = column("age", ColumnType.INTEGER)
    private val active = column("active", ColumnType.BOOLEAN)
    private val status = column("status", ColumnType.ENUMERATION) { copy(enumerationValues = listOf("ACTIVE", "BLOCKED")) }
    private val roleReference = column("roles", ColumnType.INTEGER) {
        copy(reference = Reference.ManyToMany("roles", "user_roles", "user_id", "role_id"))
    }

    @BeforeTest
    fun setUp() {
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
            maximumPoolSize = 2
        })
        KtorAdminHikariCP.defaultCustom(dataSource)
        createSchema()
        usersTable = TestJdbcTable(
            columns = listOf(id, name, age, active, status, roleReference),
            tableName = "users",
            panelListColumns = listOf("id", "name", "age", "active", "status", "roles"),
            filters = listOf("active", "status"),
        )
        rolesTable = TestJdbcTable(
            columns = listOf(column("id", ColumnType.INTEGER), column("label", ColumnType.STRING)),
            tableName = "roles",
            panelListColumns = listOf("id", "label"),
        )
        userRolesTable = TestJdbcTable(
            columns = listOf(column("user_id", ColumnType.INTEGER), column("role_id", ColumnType.INTEGER)),
            tableName = "user_roles",
            panelListColumns = listOf("user_id", "role_id"),
        )
    }

    @AfterTest
    fun tearDown() {
        KtorAdminHikariCP.closeAllConnections()
    }

    @Test
    fun `should insert read update and delete rows using real jdbc repository`() {
        val insertCount = JdbcQueriesRepository.insertData(usersTable, listOf(3, "Grace", 37, true, "ACTIVE"))

        assertEquals(1, insertCount)
        assertEquals(listOf("3", "Grace", "37", "true", "ACTIVE"), JdbcQueriesRepository.getData(usersTable, "3"))
        assertEquals(3, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))

        JdbcQueriesRepository.updateAColumn(usersTable, age, "38", "3")
        assertEquals(listOf("3", "Grace", "38", "true", "ACTIVE"), JdbcQueriesRepository.getData(usersTable, "3"))

        JdbcQueriesRepository.deleteRows(usersTable, listOf("3"))
        assertNull(JdbcQueriesRepository.getData(usersTable, "3"))
        assertEquals(2, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    @Test
    fun `should query all data with search filters ordering and pagination`() {
        val filtered = JdbcQueriesRepository.getAllData(
            table = usersTable,
            tables = listOf(usersTable),
            search = "Ada",
            currentPage = 0,
            filters = mutableListOf(Triple(active, "= ", true)),
            order = Order("age", "DESC"),
        )

        assertEquals(1, filtered.size)
        assertEquals("1", filtered.single().primaryKey)
        assertEquals(listOf("1", "Ada", "36", "true", "ACTIVE"), filtered.single().data)
    }

    @Test
    fun `should generate csv and count tables from real database`() {
        val csv = JdbcQueriesRepository.getAllDataAsCsvFile(usersTable)
        val counts = JdbcQueriesRepository.getCountOfTables(listOf(usersTable, rolesTable))

        assertTrue(csv.contains("Ada, 36, true, ACTIVE"))
        assertTrue(csv.contains("Linus, 54, false, BLOCKED"))
        assertEquals(mapOf("users" to 2L, "roles" to 2L), counts)
    }

    @Test
    fun `should check unique values while excluding current primary key`() {
        assertTrue(JdbcQueriesRepository.checkExistSameData(usersTable, name, "Ada"))
        assertFalse(JdbcQueriesRepository.checkExistSameData(usersTable, name, "Ada", primaryKey = "1"))
        assertFalse(JdbcQueriesRepository.checkExistSameData(usersTable, name, "Missing"))
    }

    @Test
    fun `should read selected columns for ids and return empty for empty inputs`() {
        assertEquals(emptyList(), JdbcQueriesRepository.getSelectedColumnsForIds(usersTable, emptyList(), listOf(name)))
        assertEquals(emptyList(), JdbcQueriesRepository.getSelectedColumnsForIds(usersTable, listOf("1"), emptyList()))

        val rows = JdbcQueriesRepository.getSelectedColumnsForIds(usersTable, listOf("1", "2"), listOf(name, status))

        assertEquals(listOf(listOf("Ada", "ACTIVE"), listOf("Linus", "BLOCKED")), rows)
    }

    @Test
    fun `should read and update many to many reference rows`() {
        assertEquals(listOf(1), JdbcQueriesRepository.getAllSelectedReferenceInListReference(usersTable, roleReference, "1"))

        JdbcQueriesRepository.updateSelectedReferenceInListReference(usersTable, userRolesTable, roleReference, "1", listOf("2"))

        assertEquals(listOf(2), JdbcQueriesRepository.getAllSelectedReferenceInListReference(usersTable, roleReference, "1"))
    }

    @Test
    fun `should fail insert when parameter count does not match upsert columns`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            JdbcQueriesRepository.insertData(usersTable, listOf(4, "Missing age"))
        }

        assertEquals("The number of parameters does not match the number of columns", exception.message)
        assertNull(JdbcQueriesRepository.getData(usersTable, "4"))
    }

    @Test
    fun `should surface database constraint failures and leave prior rows intact`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.insertData(usersTable, listOf(1, "Duplicate", 1, true, "ACTIVE"))
        }

        assertNotNull(JdbcQueriesRepository.getData(usersTable, "1"))
        assertEquals(2, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    private fun createSchema() = dataSource.connection.use { connection ->
        connection.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, age INT NOT NULL, active BOOLEAN NOT NULL, status VARCHAR(20) NOT NULL)")
        connection.execute("CREATE TABLE roles (id INT PRIMARY KEY, label VARCHAR(100) NOT NULL)")
        connection.execute("CREATE TABLE user_roles (user_id INT NOT NULL, role_id INT NOT NULL, PRIMARY KEY (user_id, role_id))")
        connection.execute("INSERT INTO users (id, name, age, active, status) VALUES (1, 'Ada', 36, TRUE, 'ACTIVE')")
        connection.execute("INSERT INTO users (id, name, age, active, status) VALUES (2, 'Linus', 54, FALSE, 'BLOCKED')")
        connection.execute("INSERT INTO roles (id, label) VALUES (1, 'Admin')")
        connection.execute("INSERT INTO roles (id, label) VALUES (2, 'Editor')")
        connection.execute("INSERT INTO user_roles (user_id, role_id) VALUES (1, 1)")
    }

    private fun Connection.execute(sql: String) = createStatement().use { it.execute(sql) }

    private fun column(
        name: String,
        type: ColumnType,
        showInPanel: Boolean = true,
        extra: ColumnSet.() -> ColumnSet = { this },
    ) = ColumnSet(
        columnName = name,
        verboseName = name,
        type = type,
        showInPanel = showInPanel,
    ).extra()
}
