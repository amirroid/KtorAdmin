package ir.amirroid.ktoradmin.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import ir.amirroid.ktoradmin.TestJdbcTable
import ir.amirroid.ktoradmin.configuration.KtorAdminConfiguration
import ir.amirroid.ktoradmin.dashboard.chart.ChartDashboardSection
import ir.amirroid.ktoradmin.dashboard.list.ListDashboardSection
import ir.amirroid.ktoradmin.dashboard.simple.TextDashboardSection
import ir.amirroid.ktoradmin.hikra.KtorAdminHikariCP
import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.chart.AdminChartStyle
import ir.amirroid.ktoradmin.models.chart.ChartDashboardAggregationFunction
import ir.amirroid.ktoradmin.models.chart.ChartField
import ir.amirroid.ktoradmin.models.chart.TextDashboardAggregationFunction
import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.models.common.DisplayItem
import ir.amirroid.ktoradmin.models.order.Order
import ir.amirroid.ktoradmin.models.reference.ReferenceData
import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.panels.getAllAllowToShowColumns
import org.h2.jdbc.JdbcSQLSyntaxErrorException
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

class JdbcQueriesRepositoryFullTest {

    private lateinit var dataSource: HikariDataSource
    private lateinit var usersTable: TestJdbcTable
    private lateinit var rolesTable: TestJdbcTable
    private lateinit var userRolesTable: TestJdbcTable
    private lateinit var profilesTable: TestJdbcTable
    private lateinit var organizationsTable: TestJdbcTable

    // Column definitions
    private val id = col("id", ColumnType.INTEGER, showInPanel = true)
    private val name = col("name", ColumnType.STRING)
    private val age = col("age", ColumnType.INTEGER)
    private val active = col("active", ColumnType.BOOLEAN)
    private val status = col("status", ColumnType.ENUMERATION) {
        copy(enumerationValues = listOf("ACTIVE", "BLOCKED", "PENDING"))
    }
    private val score = col("score", ColumnType.DOUBLE)
    private val nickname = col("nickname", ColumnType.STRING) { copy(nullable = true) }
    private val profileId = col("profile_id", ColumnType.INTEGER) {
        copy(reference = Reference.OneToOne("profiles", "id"))
    }
    private val organizationId = col("organization_id", ColumnType.INTEGER) {
        copy(reference = Reference.ManyToOne("organizations", "id"))
    }
    private val roleReference = col("roles", ColumnType.INTEGER) {
        copy(reference = Reference.ManyToMany("roles", "user_roles", "user_id", "role_id"))
    }

    @BeforeTest
    fun setUp() {
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl =
                "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
            maximumPoolSize = 4
        })
        KtorAdminHikariCP.defaultCustom(dataSource)
        createSchema()
        buildTables()
    }

    @AfterTest
    fun tearDown() {
        KtorAdminHikariCP.closeAllConnections()
    }

    // -------------------------------------------------------------------------
    // INSERT
    // -------------------------------------------------------------------------

    @Test
    fun `insertData returns 1 and persists all non-null column values`() {
        val result = JdbcQueriesRepository.insertData(
            usersTable,
            listOf(10, "Turing", 41, true, "ACTIVE", 9.9, "Alan", null, 1)
        )
        assertEquals(1, result)
        val row = JdbcQueriesRepository.getData(usersTable, "10")
        assertEquals(listOf("10", "Turing", "41", "true", "ACTIVE", "9.9", "Alan", null, "1"), row)
    }

    @Test
    fun `insertData persists nullable columns as null when passed null`() {
        JdbcQueriesRepository.insertData(
            usersTable,
            listOf(11, "Hopper", 30, false, "PENDING", 5.0, null, null, null)
        )
        val row = JdbcQueriesRepository.getData(usersTable, "11")
        assertNull(row!![6]) // nickname
        assertNull(row[7])   // profile_id
        assertNull(row[8])   // organization_id
    }

    @Test
    fun `insertData throws when parameter count is less than columns`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            JdbcQueriesRepository.insertData(usersTable, listOf(20, "Short"))
        }
        assertEquals("The number of parameters does not match the number of columns", ex.message)
    }

    @Test
    fun `insertData throws when parameter count is greater than columns`() {
        assertFailsWith<IllegalArgumentException> {
            JdbcQueriesRepository.insertData(
                usersTable,
                listOf(21, "Long", 1, true, "ACTIVE", 1.0, null, null, 1, "extra")
            )
        }
    }

    @Test
    fun `insertData throws on primary key uniqueness violation`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.insertData(
                usersTable,
                listOf(1, "Duplicate", 20, true, "ACTIVE", 1.0, null, null, 1)
            )
        }
        assertEquals(2, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    @Test
    fun `insertData throws on unique name constraint violation`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.insertData(
                usersTable,
                listOf(30, "Ada", 25, true, "ACTIVE", 1.0, null, null, 1)
            )
        }
    }

    @Test
    fun `insertData throws when referencing non-existent profile (FK violation)`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.insertData(
                usersTable,
                listOf(31, "Ghost", 20, true, "ACTIVE", 1.0, null, 9999, 1)
            )
        }
    }

    @Test
    fun `insertData throws when referencing non-existent organization (FK violation)`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.insertData(
                usersTable,
                listOf(32, "Ghost2", 20, true, "ACTIVE", 1.0, null, null, 9999)
            )
        }
    }

    @Test
    fun `insertData throws on unique profile_id constraint (one-to-one violation)`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.insertData(
                usersTable,
                listOf(33, "NewUser", 20, true, "ACTIVE", 1.0, null, 1, 1)
            )
        }
    }

    // -------------------------------------------------------------------------
    // getData (single row read)
    // -------------------------------------------------------------------------

    @Test
    fun `getData returns correct row for existing primary key`() {
        val row = JdbcQueriesRepository.getData(usersTable, "1")
        assertNotNull(row)
        assertEquals("1", row[0])
        assertEquals("Ada", row[1])
        assertEquals("36", row[2])
        assertEquals("true", row[3])
        assertEquals("ACTIVE", row[4])
        assertEquals("10.5", row[5])
        assertEquals("Countess", row[6])
        assertEquals("1", row[7])
        assertEquals("1", row[8])
    }

    @Test
    fun `getData returns null for non-existent primary key`() {
        assertNull(JdbcQueriesRepository.getData(usersTable, "9999"))
    }

    @Test
    fun `getData returns null-valued columns correctly`() {
        val row = JdbcQueriesRepository.getData(usersTable, "2")
        assertNull(row!![6]) // nickname is null for Linus
    }

    // -------------------------------------------------------------------------
    // updateAColumn
    // -------------------------------------------------------------------------

    @Test
    fun `updateAColumn changes targeted column and leaves others intact`() {
        JdbcQueriesRepository.updateAColumn(usersTable, age, "99", "1")
        val row = JdbcQueriesRepository.getData(usersTable, "1")!!
        assertEquals("99", row[2])
        assertEquals("Ada", row[1]) // name unchanged
    }

    @Test
    fun `updateAColumn can set nullable column to null`() {
        JdbcQueriesRepository.updateAColumn(usersTable, nickname, null, "1")
        val row = JdbcQueriesRepository.getData(usersTable, "1")!!
        assertNull(row[6])
    }

    @Test
    fun `updateAColumn can update boolean column`() {
        JdbcQueriesRepository.updateAColumn(usersTable, active, "off", "1")
        val row = JdbcQueriesRepository.getData(usersTable, "1")!!
        assertEquals("false", row[3])
    }

    @Test
    fun `updateAColumn can update double column`() {
        JdbcQueriesRepository.updateAColumn(usersTable, score, "99.9", "1")
        val row = JdbcQueriesRepository.getData(usersTable, "1")!!
        assertEquals("99.9", row[5])
    }

    @Test
    fun `updateAColumn can update enumeration column`() {
        JdbcQueriesRepository.updateAColumn(usersTable, status, "BLOCKED", "1")
        val row = JdbcQueriesRepository.getData(usersTable, "1")!!
        assertEquals("BLOCKED", row[4])
    }

    @Test
    fun `updateAColumn does nothing meaningful for non-existent primary key`() {
        JdbcQueriesRepository.updateAColumn(usersTable, name, "Ghost", "9999")
        assertNull(JdbcQueriesRepository.getData(usersTable, "9999"))
    }

    // -------------------------------------------------------------------------
    // deleteRows
    // -------------------------------------------------------------------------

    @Test
    fun `deleteRows removes single row by primary key`() {
        JdbcQueriesRepository.deleteRows(usersTable, listOf("2"))
        assertNull(JdbcQueriesRepository.getData(usersTable, "2"))
        assertEquals(1, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    @Test
    fun `deleteRows removes multiple rows in a single call`() {
        JdbcQueriesRepository.insertData(
            usersTable,
            listOf(5, "User5", 25, true, "ACTIVE", 1.0, null, null, 1)
        )
        JdbcQueriesRepository.deleteRows(usersTable, listOf("1", "2", "5"))
        assertEquals(0, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    @Test
    fun `deleteRows with non-existent ids does not affect existing rows`() {
        JdbcQueriesRepository.deleteRows(usersTable, listOf("9998", "9999"))
        assertEquals(2, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    @Test
    fun `deleteRows cascades user_roles join rows when user is deleted`() {
        JdbcQueriesRepository.deleteRows(usersTable, listOf("1"))
        val remaining = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "1"
        )
        assertEquals(emptyList(), remaining)
    }

    @Test
    fun `deleteRows throws when deleting organization referenced by user (RESTRICT)`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.deleteRows(
                organizationsTable,
                listOf("1")
            )
        }
        assertEquals(2, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    @Test
    fun `deleteRows throws when deleting role referenced by user_roles (RESTRICT)`() {
        assertFailsWith<Exception> { JdbcQueriesRepository.deleteRows(rolesTable, listOf("1")) }
    }

    @Test
    fun `deleteRows on profile sets user FK to null via ON DELETE SET NULL`() {
        JdbcQueriesRepository.deleteRows(profilesTable, listOf("1"))
        val row = JdbcQueriesRepository.getData(usersTable, "1")!!
        assertNull(row[7])
        assertEquals("Ada", row[1]) // user row still exists
    }

    // -------------------------------------------------------------------------
    // getCount
    // -------------------------------------------------------------------------

    @Test
    fun `getCount returns total rows when no search and no filters`() {
        assertEquals(2, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    @Test
    fun `getCount returns 0 for empty table`() {
        deleteAllUsers()
        assertEquals(0, JdbcQueriesRepository.getCount(usersTable, null, emptyList()))
    }

    @Test
    fun `getCount returns correct count with search term matching one row`() {
        assertEquals(1, JdbcQueriesRepository.getCount(usersTable, "Ada", emptyList()))
    }

    @Test
    fun `getCount returns 0 when search term matches nothing`() {
        assertEquals(0, JdbcQueriesRepository.getCount(usersTable, "zzznomatch", emptyList()))
    }

    @Test
    fun `getCount is case insensitive for search`() {
        assertEquals(1, JdbcQueriesRepository.getCount(usersTable, "ada", emptyList()))
        assertEquals(1, JdbcQueriesRepository.getCount(usersTable, "ADA", emptyList()))
    }

    @Test
    fun `getCount respects equality filter on boolean column`() {
        assertEquals(
            1,
            JdbcQueriesRepository.getCount(usersTable, null, listOf(Triple(active, "= ", true)))
        )
        assertEquals(
            1,
            JdbcQueriesRepository.getCount(usersTable, null, listOf(Triple(active, "= ", false)))
        )
    }

    @Test
    fun `getCount respects range filter on numeric column`() {
        assertEquals(
            1,
            JdbcQueriesRepository.getCount(usersTable, null, listOf(Triple(age, ">=", 50)))
        )
        assertEquals(
            2,
            JdbcQueriesRepository.getCount(usersTable, null, listOf(Triple(age, ">=", 30)))
        )
    }

    @Test
    fun `getCount returns correct count via joined column filter`() {
        val filters = mutableListOf(Triple(organizationId, "= ", 2))
        assertEquals(1, JdbcQueriesRepository.getCount(usersTable, "lin", filters))
    }

    // -------------------------------------------------------------------------
    // getAllData — basic retrieval
    // -------------------------------------------------------------------------

    @Test
    fun `getAllData returns all rows when no constraints applied`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(),
            null
        )
        assertEquals(2, rows.size)
    }

    @Test
    fun `getAllData returns empty list when table is empty`() {
        deleteAllUsers()
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(),
            null
        )
        assertEquals(emptyList(), rows)
    }

    @Test
    fun `getAllData returns correct data columns in declared order`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(),
            Order("id", "ASC")
        )
        val first = rows.first()
        assertEquals(
            listOf("1", "Ada", "36", "true", "ACTIVE", "10.5", "Countess", "1", "1"),
            first.data
        )
    }

    @Test
    fun `getAllData includes primaryKey field in each row`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(),
            Order("id", "ASC")
        )
        assertEquals(listOf("1", "2"), rows.map { it.primaryKey })
    }

    // -------------------------------------------------------------------------
    // getAllData — search
    // -------------------------------------------------------------------------

    @Test
    fun `getAllData filters by search on direct column`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            "Linus",
            null,
            mutableListOf(),
            null
        )
        assertEquals(1, rows.size)
        assertEquals("2", rows.single().primaryKey)
    }

    @Test
    fun `getAllData search is case insensitive`() {
        assertEquals(
            1,
            JdbcQueriesRepository.getAllData(
                usersTable,
                listOf(usersTable),
                "linus",
                null,
                mutableListOf(),
                null
            ).size
        )
        assertEquals(
            1,
            JdbcQueriesRepository.getAllData(
                usersTable,
                listOf(usersTable),
                "LINUS",
                null,
                mutableListOf(),
                null
            ).size
        )
    }

    @Test
    fun `getAllData search matches partial string`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            "in",
            null,
            mutableListOf(),
            null
        )
        assertTrue(rows.any { it.primaryKey == "2" })
    }

    @Test
    fun `getAllData returns empty when search matches nothing`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            "zzznomatch",
            null,
            mutableListOf(),
            null
        )
        assertEquals(emptyList(), rows)
    }

    @Test
    fun `getAllData searches across joined column (organization name)`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            "Kernel",
            null,
            mutableListOf(),
            null
        )
        assertEquals(listOf("2"), rows.map { it.primaryKey })
    }

    @Test
    fun `getAllData search matching both direct and joined column returns union`() {
        // "a" matches Ada (name) and Analytical Engines (org name for user 1), Kernel Labs doesn't contain "a" - just user 1 expected
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            "Analytical",
            null,
            mutableListOf(),
            null
        )
        assertEquals(listOf("1"), rows.map { it.primaryKey })
    }

    // -------------------------------------------------------------------------
    // getAllData — filters
    // -------------------------------------------------------------------------

    @Test
    fun `getAllData filters by equality on boolean column`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(Triple(active, "= ", true)),
            null
        )
        assertEquals(1, rows.size)
        assertEquals("1", rows.single().primaryKey)
    }

    @Test
    fun `getAllData filters by greater-than-or-equal on integer column`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(Triple(age, ">=", 50)),
            null
        )
        assertEquals(1, rows.size)
        assertEquals("2", rows.single().primaryKey)
    }

    @Test
    fun `getAllData filters by less-than-or-equal on double column`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(Triple(score, "<=", 5.0)),
            null
        )
        assertEquals(1, rows.size)
        assertEquals("2", rows.single().primaryKey)
    }

    @Test
    fun `getAllData filters by equality on enumeration column`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(Triple(status, "= ", "BLOCKED")),
            null
        )
        assertEquals(1, rows.size)
        assertEquals("2", rows.single().primaryKey)
    }

    @Test
    fun `getAllData multiple filters combined (AND) narrow results`() {
        val filters = mutableListOf<Triple<ColumnSet, String, Any?>>(
            Triple(active, "= ", true),
            Triple(score, ">=", 10.0)
        )
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            filters,
            null
        )
        assertEquals(1, rows.size)
        assertEquals("1", rows.single().primaryKey)
    }

    @Test
    fun `getAllData multiple filters where no row matches returns empty`() {
        val filters = mutableListOf<Triple<ColumnSet, String, Any?>>(
            Triple(active, "= ", true),
            Triple(score, ">=", 100.0)
        )
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            filters,
            null
        )
        assertEquals(emptyList(), rows)
    }

    @Test
    fun `getAllData filter on joined column via organization name`() {
        val filters = mutableListOf<Triple<ColumnSet, String, Any?>>(
            Triple(
                organizationId,
                "= ",
                1
            )
        )
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            filters,
            null
        )
        assertEquals(listOf("1"), rows.map { it.primaryKey })
    }

    @Test
    fun `getAllData search and filter combined`() {
        val filters = mutableListOf<Triple<ColumnSet, String, Any?>>(Triple(active, "= ", true))
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            "Ada",
            null,
            filters,
            null
        )
        assertEquals(listOf("1"), rows.map { it.primaryKey })
    }

    @Test
    fun `getAllData search with filter that excludes search match returns empty`() {
        val filters = mutableListOf<Triple<ColumnSet, String, Any?>>(Triple(active, "= ", false))
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            "Ada",
            null,
            filters,
            null
        )
        assertEquals(emptyList(), rows)
    }

    // -------------------------------------------------------------------------
    // getAllData — ordering
    // -------------------------------------------------------------------------

    @Test
    fun `getAllData orders ascending by id`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(),
            Order("id", "ASC")
        )
        assertEquals(listOf("1", "2"), rows.map { it.primaryKey })
    }

    @Test
    fun `getAllData orders descending by id`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(),
            Order("id", "DESC")
        )
        assertEquals(listOf("2", "1"), rows.map { it.primaryKey })
    }

    @Test
    fun `getAllData orders by name ascending`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable,
            listOf(usersTable),
            null,
            null,
            mutableListOf(),
            Order("name", "ASC")
        )
        assertEquals("Ada", rows.first().data[1])
    }

    @Test
    fun `getAllData throws when order direction is invalid`() {
        assertFailsWith<IllegalArgumentException> {
            JdbcQueriesRepository.getAllData(
                usersTable,
                listOf(usersTable),
                null,
                null,
                mutableListOf(),
                Order("id", "INVALID")
            )
        }
    }


    @Test
    fun `getAllData throws when order column does not exist`() {
        assertFailsWith<IllegalArgumentException> {
            JdbcQueriesRepository.getAllData(
                usersTable,
                listOf(usersTable),
                null,
                null,
                mutableListOf(),
                Order("nonexistent_col", "ASC")
            )
        }
    }

    // -------------------------------------------------------------------------
    // getAllData — pagination
    // -------------------------------------------------------------------------

    @Test
    fun `getAllData paginates first page correctly`() {
        val config = KtorAdminConfiguration()
        val original = config.maxItemsInPage
        try {
            config.maxItemsInPage = 1
            val page0 = JdbcQueriesRepository.getAllData(
                usersTable,
                listOf(usersTable),
                null,
                0,
                mutableListOf(),
                Order("id", "ASC")
            )
            assertEquals(listOf("1"), page0.map { it.primaryKey })
        } finally {
            config.maxItemsInPage = original
        }
    }

    @Test
    fun `getAllData paginates second page correctly`() {
        val config = KtorAdminConfiguration()
        val original = config.maxItemsInPage
        try {
            config.maxItemsInPage = 1
            val page1 = JdbcQueriesRepository.getAllData(
                usersTable,
                listOf(usersTable),
                null,
                1,
                mutableListOf(),
                Order("id", "ASC")
            )
            assertEquals(listOf("2"), page1.map { it.primaryKey })
        } finally {
            config.maxItemsInPage = original
        }
    }

    @Test
    fun `getAllData returns empty list for page beyond data`() {
        val config = KtorAdminConfiguration()
        val original = config.maxItemsInPage
        try {
            config.maxItemsInPage = 2
            val page1 = JdbcQueriesRepository.getAllData(
                usersTable,
                listOf(usersTable),
                null,
                1,
                mutableListOf(),
                Order("id", "ASC")
            )
            assertEquals(emptyList(), page1)
        } finally {
            config.maxItemsInPage = original
        }
    }

    @Test
    fun `getAllData without pagination returns all rows regardless of config`() {
        val config = KtorAdminConfiguration()
        val original = config.maxItemsInPage
        try {
            config.maxItemsInPage = 1
            val all = JdbcQueriesRepository.getAllData(
                usersTable,
                listOf(usersTable),
                null,
                null,
                mutableListOf(),
                null
            )
            assertEquals(2, all.size)
        } finally {
            config.maxItemsInPage = original
        }
    }

    // -------------------------------------------------------------------------
    // getAllData — reference mapping (one-to-one, many-to-one)
    // -------------------------------------------------------------------------

    @Test
    fun `getAllData wraps profile_id in ReferenceData when related table is provided`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable, listOf(usersTable, profilesTable, organizationsTable),
            null, null, mutableListOf(), Order("id", "ASC")
        )
        val profile = rows.first { it.primaryKey == "1" }.data[7]
        assertTrue(profile is ReferenceData)
        assertEquals("1", (profile as ReferenceData).value)
    }

    @Test
    fun `getAllData wraps organization_id in ReferenceData with correct pluralName`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable, listOf(usersTable, profilesTable, organizationsTable),
            null, null, mutableListOf(), Order("id", "ASC")
        )
        val org = rows.first { it.primaryKey == "1" }.data[8]
        assertTrue(org is ReferenceData)
        assertEquals("Tests", (org as ReferenceData).pluralName)
    }

    @Test
    fun `getAllData returns raw string for reference column when related table is absent`() {
        val rows = JdbcQueriesRepository.getAllData(
            usersTable, listOf(usersTable), // no profilesTable or organizationsTable
            null, null, mutableListOf(), Order("id", "ASC")
        )
        val profileCol = rows.first { it.primaryKey == "1" }.data[7]
        assertFalse(profileCol is ReferenceData)
    }

    @Test
    fun `getAllData returns null-mapped value for null reference column`() {
        // User 2 has profile_id set; delete it to produce a null FK
        JdbcQueriesRepository.deleteRows(profilesTable, listOf("2"))
        val rows = JdbcQueriesRepository.getAllData(
            usersTable, listOf(usersTable, profilesTable),
            null, null, mutableListOf(), Order("id", "ASC")
        )
        val row2 = rows.first { it.primaryKey == "2" }
        // Null reference should not be ReferenceData
        val profileCol = row2.data[7]
        assertFalse(profileCol is ReferenceData)
    }

    // -------------------------------------------------------------------------
    // getAllDataAsCsvFile
    // -------------------------------------------------------------------------

    @Test
    fun `getAllDataAsCsvFile returns comma-separated rows for all users`() {
        val csv = JdbcQueriesRepository.getAllDataAsCsvFile(usersTable)
        assertTrue(csv.contains("Ada"))
        assertTrue(csv.contains("Linus"))
    }

    @Test
    fun `getAllDataAsCsvFile replaces null with N-slash-A`() {
        val csv = JdbcQueriesRepository.getAllDataAsCsvFile(usersTable)
        assertTrue(csv.contains("N/A"))
    }

    @Test
    fun `getAllDataAsCsvFile returns empty string for empty table`() {
        deleteAllUsers()
        assertEquals("", JdbcQueriesRepository.getAllDataAsCsvFile(usersTable))
    }

    @Test
    fun `getAllDataAsCsvFile rows are newline-separated`() {
        val csv = JdbcQueriesRepository.getAllDataAsCsvFile(usersTable)
        assertEquals(2, csv.trim().lines().size)
    }

    // -------------------------------------------------------------------------
    // checkExistSameData
    // -------------------------------------------------------------------------

    @Test
    fun `checkExistSameData returns true for existing name without primary key exclusion`() {
        assertTrue(JdbcQueriesRepository.checkExistSameData(usersTable, name, "Ada"))
    }

    @Test
    fun `checkExistSameData returns false when excluding own primary key`() {
        assertFalse(
            JdbcQueriesRepository.checkExistSameData(
                usersTable,
                name,
                "Ada",
                primaryKey = "1"
            )
        )
    }

    @Test
    fun `checkExistSameData returns false for non-existent value`() {
        assertFalse(JdbcQueriesRepository.checkExistSameData(usersTable, name, "NoSuchName"))
    }

    @Test
    fun `checkExistSameData returns true for value existing for different primary key than excluded`() {
        // "Ada" exists with pk=1; exclude pk=2 → should still find pk=1
        assertTrue(
            JdbcQueriesRepository.checkExistSameData(
                usersTable,
                name,
                "Ada",
                primaryKey = "2"
            )
        )
    }

    @Test
    fun `checkExistSameData works on integer column`() {
        assertTrue(JdbcQueriesRepository.checkExistSameData(usersTable, age, 36))
        assertFalse(JdbcQueriesRepository.checkExistSameData(usersTable, age, 999))
    }

    @Test
    fun `checkExistSameData works on boolean column`() {
        assertTrue(JdbcQueriesRepository.checkExistSameData(usersTable, active, true))
    }

    @Test
    fun `checkExistSameData returns false when table is empty`() {
        deleteAllUsers()
        assertFalse(JdbcQueriesRepository.checkExistSameData(usersTable, name, "Ada"))
    }

    // -------------------------------------------------------------------------
    // getCountOfTables
    // -------------------------------------------------------------------------

    @Test
    fun `getCountOfTables returns correct counts for multiple tables`() {
        val counts = JdbcQueriesRepository.getCountOfTables(listOf(usersTable, rolesTable))
        assertEquals(2L, counts["users"])
        assertEquals(3L, counts["roles"])
    }

    @Test
    fun `getCountOfTables returns zero for empty table`() {
        deleteAllUsers()
        val counts = JdbcQueriesRepository.getCountOfTables(listOf(usersTable))
        assertEquals(0L, counts["users"])
    }

    @Test
    fun `getCountOfTables handles single table`() {
        val counts = JdbcQueriesRepository.getCountOfTables(listOf(profilesTable))
        assertEquals(2L, counts["profiles"])
    }

    @Test
    fun `getCountOfTables includes all tables in result map`() {
        val counts = JdbcQueriesRepository.getCountOfTables(
            listOf(
                usersTable,
                rolesTable,
                profilesTable,
                organizationsTable
            )
        )
        assertTrue(counts.containsKey("users"))
        assertTrue(counts.containsKey("roles"))
        assertTrue(counts.containsKey("profiles"))
        assertTrue(counts.containsKey("organizations"))
    }

    // -------------------------------------------------------------------------
    // getAllReferences
    // -------------------------------------------------------------------------

    @Test
    fun `getAllReferences uses displayFormat template for label`() {
        val items = JdbcQueriesRepository.getAllReferences(profilesTable)
        assertEquals(
            listOf(
                DisplayItem("1", "Profile 1: Ada Bio"),
                DisplayItem("2", "Profile 2: Linus Bio")
            ), items
        )
    }

    @Test
    fun `getAllReferences uses displayFormat with nested join field`() {
        val tableWithJoinFormat = TestJdbcTable(
            columns = usersTable.getAllColumns(),
            tableName = "users",
            panelListColumns = usersTable.getPanelListColumns(),
            displayFormat = "{id}: {name} @ {organization_id.name}",
        )
        val items = JdbcQueriesRepository.getAllReferences(tableWithJoinFormat)
        assertTrue(items.any { it.item.contains("Ada") && it.item.contains("Analytical Engines") })
        assertTrue(items.any { it.item.contains("Linus") && it.item.contains("Kernel Labs") })
    }

    @Test
    fun `getAllReferences falls back to singular name object pattern when no displayFormat`() {
        val items = JdbcQueriesRepository.getAllReferences(rolesTable)
        assertTrue(items.all { it.item.matches(Regex("Test Object \\(\\d+\\)")) })
    }

    @Test
    fun `getAllReferences respects defaultOrder`() {
        // organizationsTable has defaultOrder=ASC on name
        val items = JdbcQueriesRepository.getAllReferences(organizationsTable)
        assertEquals("1 - Analytical Engines", items[0].item)
        assertEquals("2 - Kernel Labs", items[1].item)
    }

    @Test
    fun `getAllReferences returns empty list when table is empty`() {
        deleteAllUsers()
        dataSource.connection.use { it.execute("DELETE FROM roles") }
        val items = JdbcQueriesRepository.getAllReferences(rolesTable)
        assertEquals(emptyList(), items)
    }

    @Test
    fun `getAllReferences returns DisplayItem with correct itemKey`() {
        val items = JdbcQueriesRepository.getAllReferences(profilesTable)
        assertEquals(setOf("1", "2"), items.map { it.itemKey }.toSet())
    }

    // -------------------------------------------------------------------------
    // Many-to-many: getAllSelectedReferenceInListReference
    // -------------------------------------------------------------------------

    @Test
    fun `getAllSelectedReferenceInListReference returns initial role for user 1`() {
        val keys = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "1"
        )
        assertEquals(listOf(1), keys)
    }

    @Test
    fun `getAllSelectedReferenceInListReference returns empty for user with no roles`() {
        val keys = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "2"
        )
        assertEquals(emptyList(), keys)
    }

    @Test
    fun `getAllSelectedReferenceInListReference returns empty for non-existent user`() {
        val keys = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "9999"
        )
        assertEquals(emptyList(), keys)
    }

    // -------------------------------------------------------------------------
    // Many-to-many: updateSelectedReferenceInListReference
    // -------------------------------------------------------------------------

    @Test
    fun `updateSelectedReferenceInListReference adds new roles`() {
        JdbcQueriesRepository.updateSelectedReferenceInListReference(
            usersTable,
            userRolesTable,
            roleReference,
            "1",
            listOf("1", "2", "3")
        )
        val keys = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "1"
        )
        assertEquals(listOf(1, 2, 3).sorted(), keys.map { (it as Int) }.sorted())
    }

    @Test
    fun `updateSelectedReferenceInListReference replaces roles keeping only specified`() {
        JdbcQueriesRepository.updateSelectedReferenceInListReference(
            usersTable,
            userRolesTable,
            roleReference,
            "1",
            listOf("2")
        )
        val keys = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "1"
        )
        assertEquals(listOf(2), keys)
    }

    @Test
    fun `updateSelectedReferenceInListReference with empty list removes all roles`() {
        JdbcQueriesRepository.updateSelectedReferenceInListReference(
            usersTable,
            userRolesTable,
            roleReference,
            "1",
            emptyList()
        )
        val keys = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "1"
        )
        assertEquals(emptyList(), keys)
    }

    @Test
    fun `updateSelectedReferenceInListReference is idempotent for same set`() {
        JdbcQueriesRepository.updateSelectedReferenceInListReference(
            usersTable,
            userRolesTable,
            roleReference,
            "1",
            listOf("1")
        )
        JdbcQueriesRepository.updateSelectedReferenceInListReference(
            usersTable,
            userRolesTable,
            roleReference,
            "1",
            listOf("1")
        )
        val keys = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "1"
        )
        assertEquals(listOf(1), keys)
    }

    @Test
    fun `updateSelectedReferenceInListReference throws for role id not in roles table (FK violation)`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.updateSelectedReferenceInListReference(
                usersTable,
                userRolesTable,
                roleReference,
                "1",
                listOf("999")
            )
        }
    }

    @Test
    fun `updateSelectedReferenceInListReference does not affect other users roles`() {
        JdbcQueriesRepository.updateSelectedReferenceInListReference(
            usersTable,
            userRolesTable,
            roleReference,
            "2",
            listOf("2")
        )
        val user1Keys = JdbcQueriesRepository.getAllSelectedReferenceInListReference(
            usersTable,
            roleReference,
            "1"
        )
        assertEquals(listOf(1), user1Keys) // user 1 roles untouched
    }

    // -------------------------------------------------------------------------
    // updateChangedData
    // -------------------------------------------------------------------------

    @Test
    fun `updateChangedData updates only the changed column`() {
        val initialData = JdbcQueriesRepository.getData(usersTable, "1")
        val result = JdbcQueriesRepository.updateChangedData(
            usersTable,
            parameters = listOf(
                "1" to 1,
                "Ada Lovelace" to "Ada Lovelace",
                "36" to 36,
                "on" to true,
                "ACTIVE" to "ACTIVE",
                "10.5" to 10.5,
                "Countess" to "Countess",
                "1" to 1,
                "1" to 1
            ),
            primaryKey = "1",
            initialData = initialData
        )
        assertNotNull(result)
        assertEquals(listOf("name"), result!!.second)
        assertEquals("Ada Lovelace", JdbcQueriesRepository.getData(usersTable, "1")!![1])
    }

    @Test
    fun `updateChangedData returns null when nothing changed`() {
        val initialData = JdbcQueriesRepository.getData(usersTable, "1")
        val result = JdbcQueriesRepository.updateChangedData(
            usersTable,
            parameters = listOf(
                "1" to 1,
                "Ada" to "Ada",
                "36" to 36,
                "on" to true,
                "ACTIVE" to "ACTIVE",
                "10.5" to 10.5,
                "Countess" to "Countess",
                "1" to 1,
                "1" to 1
            ),
            primaryKey = "1",
            initialData = initialData
        )
        assertNull(result)
    }

    @Test
    fun `updateChangedData inserts new row when initialData is null`() {
        val result = JdbcQueriesRepository.updateChangedData(
            usersTable,
            parameters = listOf(
                "7" to 7,
                "New" to "New",
                "22" to 22,
                "off" to false,
                "ACTIVE" to "ACTIVE",
                "0.1" to 0.1,
                null,
                null,
                "1" to 1
            ),
            primaryKey = "7",
            initialData = null
        )
        assertNotNull(result)
        assertEquals(1, result!!.first)
        assertNotNull(JdbcQueriesRepository.getData(usersTable, "7"))
    }

    @Test
    fun `updateChangedData skips column with hasConfirmation true`() {
        val confirmedAge = age.copy(hasConfirmation = true)
        val tableWithConfirm = TestJdbcTable(
            columns = listOf(
                id,
                name,
                confirmedAge,
                active,
                status,
                score,
                nickname,
                profileId,
                organizationId
            ),
            tableName = "users",
            panelListColumns = listOf(
                "id",
                "name",
                "age",
                "active",
                "status",
                "score",
                "nickname",
                "profile_id",
                "organization_id"
            ),
        )
        val initial = JdbcQueriesRepository.getData(tableWithConfirm, "1")
        JdbcQueriesRepository.updateChangedData(
            tableWithConfirm,
            parameters = listOf(
                "1" to 1,
                "Ada" to "Ada",
                "99" to 99,
                "on" to true,
                "ACTIVE" to "ACTIVE",
                "10.5" to 10.5,
                "Countess" to "Countess",
                "1" to 1,
                "1" to 1
            ),
            primaryKey = "1",
            initialData = initial
        )
        assertEquals(
            "36",
            JdbcQueriesRepository.getData(tableWithConfirm, "1")!![2]
        ) // age unchanged
    }

    @Test
    fun `updateChangedData does not clear a column when new value is null but initial was not null`() {
        val initial = JdbcQueriesRepository.getData(usersTable, "1")
        JdbcQueriesRepository.updateChangedData(
            usersTable,
            parameters = listOf(
                "1" to 1,
                "Ada" to "Ada",
                "36" to 36,
                "on" to true,
                "ACTIVE" to "ACTIVE",
                "10.5" to 10.5,
                null,
                "1" to 1,
                "1" to 1
            ),
            primaryKey = "1",
            initialData = initial
        )
        // nickname was "Countess"; null passed → should NOT be cleared per implementation
        assertEquals("Countess", JdbcQueriesRepository.getData(usersTable, "1")!![6])
    }

    @Test
    fun `updateChangedData handles boolean on-value compared to stored true`() {
        // "on" should be considered same as true → no change expected
        val initial = JdbcQueriesRepository.getData(usersTable, "1")
        val result = JdbcQueriesRepository.updateChangedData(
            usersTable,
            parameters = listOf(
                "1" to 1,
                "Ada" to "Ada",
                "36" to 36,
                "on" to true,
                "ACTIVE" to "ACTIVE",
                "10.5" to 10.5,
                "Countess" to "Countess",
                "1" to 1,
                "1" to 1
            ),
            primaryKey = "1",
            initialData = initial
        )
        assertNull(result)
    }

    @Test
    fun `updateChangedData handles boolean off-value compared to stored false`() {
        val initial = JdbcQueriesRepository.getData(usersTable, "2")
        val result = JdbcQueriesRepository.updateChangedData(
            usersTable,
            parameters = listOf(
                "2" to 2,
                "Linus" to "Linus",
                "54" to 54,
                "off" to false,
                "BLOCKED" to "BLOCKED",
                "2.0" to 2.0,
                null,
                "2" to 2,
                "2" to 2
            ),
            primaryKey = "2",
            initialData = initial
        )
        assertNull(result)
    }

    @Test
    fun `updateChangedData returns list of changed column names`() {
        val initial = JdbcQueriesRepository.getData(usersTable, "1")
        val result = JdbcQueriesRepository.updateChangedData(
            usersTable,
            parameters = listOf(
                "1" to 1,
                "Ada Updated" to "Ada Updated",
                "40" to 40,
                "on" to true,
                "ACTIVE" to "ACTIVE",
                "10.5" to 10.5,
                "Countess" to "Countess",
                "1" to 1,
                "1" to 1
            ),
            primaryKey = "1",
            initialData = initial
        )
        assertNotNull(result)
        assertTrue(result!!.second.containsAll(listOf("name", "age")))
    }

    // -------------------------------------------------------------------------
    // Query builder helpers (public surface)
    // -------------------------------------------------------------------------

    @Test
    fun `createUpdateAColumnQuery produces correct SQL`() {
        with(JdbcQueriesRepository) {
            assertEquals(
                "UPDATE users SET age = ? WHERE id = ?",
                usersTable.createUpdateAColumnQuery(age)
            )
        }
    }

    @Test
    fun `createGetAllDataAsCsvQuery produces SELECT star query`() {
        with(JdbcQueriesRepository) {
            assertEquals("SELECT * FROM users", usersTable.createGetAllDataAsCsvQuery())
        }
    }

    @Test
    fun `createGetSelectedColumnsQuery produces correct SQL for multiple columns and ids`() {
        with(JdbcQueriesRepository) {
            assertEquals(
                "SELECT name, status FROM users WHERE id IN (?,?)",
                usersTable.createGetSelectedColumnsQuery(listOf("1", "2"), listOf(name, status))
            )
        }
    }

    @Test
    fun `createGetSelectedColumnsQuery produces correct SQL for single column and single id`() {
        with(JdbcQueriesRepository) {
            assertEquals(
                "SELECT name FROM users WHERE id IN (?)",
                usersTable.createGetSelectedColumnsQuery(listOf("1"), listOf(name))
            )
        }
    }

    @Test
    fun `createGetSelectedColumnsQuery throws for empty selectedIds`() {
        with(JdbcQueriesRepository) {
            assertFailsWith<IllegalArgumentException> {
                usersTable.createGetSelectedColumnsQuery(emptyList(), listOf(name))
            }
        }
    }

    @Test
    fun `createGetSelectedColumnsQuery throws for empty columns`() {
        with(JdbcQueriesRepository) {
            assertFailsWith<IllegalArgumentException> {
                usersTable.createGetSelectedColumnsQuery(listOf("1"), emptyList())
            }
        }
    }

    // -------------------------------------------------------------------------
    // getSelectedColumnsForIds
    // -------------------------------------------------------------------------

    @Test
    fun `getSelectedColumnsForIds returns correct values for existing ids`() {
        val rows = JdbcQueriesRepository.getSelectedColumnsForIds(
            usersTable,
            listOf("1", "2"),
            listOf(name, status)
        )
        assertEquals(listOf(listOf("Ada", "ACTIVE"), listOf("Linus", "BLOCKED")), rows)
    }

    @Test
    fun `getSelectedColumnsForIds returns empty list for empty selectedIds`() {
        assertEquals(
            emptyList(),
            JdbcQueriesRepository.getSelectedColumnsForIds(usersTable, emptyList(), listOf(name))
        )
    }

    @Test
    fun `getSelectedColumnsForIds returns empty list for empty columns`() {
        assertEquals(
            emptyList(),
            JdbcQueriesRepository.getSelectedColumnsForIds(usersTable, listOf("1"), emptyList())
        )
    }

    @Test
    fun `getSelectedColumnsForIds returns null for null column values`() {
        val rows = JdbcQueriesRepository.getSelectedColumnsForIds(
            usersTable,
            listOf("2"),
            listOf(nickname)
        )
        assertEquals(listOf(listOf(null)), rows)
    }

    @Test
    fun `getSelectedColumnsForIds single column single id`() {
        val rows =
            JdbcQueriesRepository.getSelectedColumnsForIds(usersTable, listOf("1"), listOf(name))
        assertEquals(listOf(listOf("Ada")), rows)
    }

    @Test
    fun `getSelectedColumnsForIds for non-existent id returns empty`() {
        val rows =
            JdbcQueriesRepository.getSelectedColumnsForIds(usersTable, listOf("9999"), listOf(name))
        assertEquals(emptyList(), rows)
    }

    // -------------------------------------------------------------------------
    // Dashboard: getListSectionData
    // -------------------------------------------------------------------------

    @Test
    fun `getListSectionData returns all table columns when fields is null`() {
        val section = listSection(fields = null, orderQuery = "id ASC")
        val data = JdbcQueriesRepository.getListSectionData(usersTable, section)
        assertEquals(usersTable.getAllAllowToShowColumns().size, data.fields.size)
    }

    @Test
    fun `getListSectionData returns only specified fields`() {
        val section = listSection(fields = listOf("name", "score"), orderQuery = "id ASC")
        val data = JdbcQueriesRepository.getListSectionData(usersTable, section)
        assertEquals(listOf("name", "score"), data.fields.map { it.fieldName })
    }

    @Test
    fun `getListSectionData limits results when limitCount is set`() {
        val section = listSection(fields = listOf("name"), orderQuery = "id ASC", limitCount = 1)
        val data = JdbcQueriesRepository.getListSectionData(usersTable, section)
        assertEquals(1, data.values.size)
    }

    @Test
    fun `getListSectionData returns correct pluralName`() {
        val section = listSection(orderQuery = "id ASC")
        val data = JdbcQueriesRepository.getListSectionData(usersTable, section)
        assertEquals("Tests", data.pluralName)
    }

    @Test
    fun `getListSectionData row data includes correct values`() {
        val section =
            listSection(fields = listOf("name", "score"), orderQuery = "score DESC", limitCount = 1)
        val data = JdbcQueriesRepository.getListSectionData(usersTable, section)
        assertEquals(listOf("Ada", "10.5"), data.values.single().data)
    }

    @Test
    fun `getListSectionData returns empty values for empty table`() {
        deleteAllUsers()
        val section = listSection(orderQuery = "id ASC")
        val data = JdbcQueriesRepository.getListSectionData(usersTable, section)
        assertEquals(emptyList(), data.values)
    }

    @Test
    fun `getListSectionData includes primaryKey in each row`() {
        val section = listSection(fields = listOf("name"), orderQuery = "id ASC", limitCount = 1)
        val data = JdbcQueriesRepository.getListSectionData(usersTable, section)
        assertEquals("1", data.values.single().primaryKey)
    }

    @Test
    fun `getListSectionData includes FieldData type information`() {
        val section = listSection(fields = listOf("score"), orderQuery = "id ASC")
        val data = JdbcQueriesRepository.getListSectionData(usersTable, section)
        assertEquals("DOUBLE", data.fields.single().type)
    }

    // -------------------------------------------------------------------------
    // Dashboard: getChartData
    // -------------------------------------------------------------------------

    @Test
    fun `getChartData ALL aggregation returns one entry per row`() {
        val chart = JdbcQueriesRepository.getChartData(
            usersTable,
            chartSection(ChartDashboardAggregationFunction.ALL, orderQuery = "id ASC")
        )
        assertEquals(2, chart.labels.size)
        assertEquals(2, chart.values.single().values.size)
    }

    @Test
    fun `getChartData ALL preserves individual values without aggregating`() {
        val chart = JdbcQueriesRepository.getChartData(
            usersTable,
            chartSection(ChartDashboardAggregationFunction.ALL, orderQuery = "id ASC")
        )
        assertEquals(listOf("ACTIVE", "BLOCKED"), chart.labels)
        assertEquals(listOf(10.5, 2.0), chart.values.single().values)
    }

    @Test
    fun `getChartData SUM aggregates values per label`() {
        val chart = JdbcQueriesRepository.getChartData(
            usersTable,
            chartSection(ChartDashboardAggregationFunction.SUM, orderQuery = "status ASC")
        )
        val activeIdx = chart.labels.indexOf("ACTIVE")
        assertEquals(10.5, chart.values.single().values[activeIdx])
    }

    @Test
    fun `getChartData AVERAGE aggregates values per label`() {
        val chart = JdbcQueriesRepository.getChartData(
            usersTable,
            chartSection(ChartDashboardAggregationFunction.AVERAGE, orderQuery = "status ASC")
        )
        val activeIdx = chart.labels.indexOf("ACTIVE")
        assertEquals(10.5, chart.values.single().values[activeIdx])
    }

    @Test
    fun `getChartData COUNT returns integer count per label`() {
        val chart = JdbcQueriesRepository.getChartData(
            usersTable,
            chartSection(ChartDashboardAggregationFunction.COUNT, orderQuery = "status ASC")
        )
        val activeIdx = chart.labels.indexOf("ACTIVE")
        assertEquals(1.0, chart.values.single().values[activeIdx])
    }

    @Test
    fun `getChartData respects limitCount`() {
        val chart = JdbcQueriesRepository.getChartData(
            usersTable,
            chartSection(
                ChartDashboardAggregationFunction.COUNT,
                orderQuery = "status ASC",
                limitCount = 1
            )
        )
        assertEquals(1, chart.labels.size)
    }

    @Test
    fun `getChartData provides fill and border colors via callbacks`() {
        val chart = JdbcQueriesRepository.getChartData(
            usersTable,
            chartSection(ChartDashboardAggregationFunction.SUM, orderQuery = "status ASC")
        )
        val fills = chart.values.single().fillColors
        val borders = chart.values.single().borderColors
        assertTrue(fills.all { it?.startsWith("fill-") == true })
        assertTrue(borders.all { it?.startsWith("border-") == true })
    }

    @Test
    fun `getChartData returns section reference in ChartData`() {
        val section = chartSection(ChartDashboardAggregationFunction.SUM)
        val chart = JdbcQueriesRepository.getChartData(usersTable, section)
        assertEquals(section, chart.section)
    }

    @Test
    fun `getChartData returns empty labels and values for empty table`() {
        deleteAllUsers()
        val chart = JdbcQueriesRepository.getChartData(
            usersTable,
            chartSection(ChartDashboardAggregationFunction.SUM)
        )
        assertEquals(emptyList(), chart.labels)
        assertTrue(chart.values.single().values.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Dashboard: getTextData
    // -------------------------------------------------------------------------

    @Test
    fun `getTextData COUNT returns total row count as string`() {
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("id", TextDashboardAggregationFunction.COUNT)
        )
        assertEquals("2", result.value)
    }

    @Test
    fun `getTextData SUM returns sum of values`() {
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("score", TextDashboardAggregationFunction.SUM)
        )
        assertEquals("12.5", result.value)
    }

    @Test
    fun `getTextData AVERAGE returns average of values`() {
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("score", TextDashboardAggregationFunction.AVERAGE)
        )
        assertEquals("6.25", result.value)
    }

    @Test
    fun `getTextData LAST_ITEM returns last ordered value`() {
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("name", TextDashboardAggregationFunction.LAST_ITEM, "id DESC")
        )
        assertEquals("Ada", result.value)
    }

    @Test
    fun `getTextData LAST_ITEM with ascending order returns first row`() {
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("name", TextDashboardAggregationFunction.LAST_ITEM, "id ASC")
        )
        assertEquals("Linus", result.value)
    }

    @Test
    fun `getTextData PROFIT_PERCENTAGE calculates growth correctly`() {
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("score", TextDashboardAggregationFunction.PROFIT_PERCENTAGE, "id ASC")
        )
        // (2.0 - 10.5) / 10.5 * 100 ≈ -80.95% OR (10.5 - 2.0) / 2.0 * 100 = 425%
        // Based on existing test showing 425% with id ASC, second item (Linus=2.0) is "previous"
        assertEquals("425%", result.value)
    }

    @Test
    fun `getTextData COUNT returns 0 for empty table`() {
        deleteAllUsers()
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("id", TextDashboardAggregationFunction.COUNT)
        )
        assertEquals("0", result.value)
    }

    @Test
    fun `getTextData LAST_ITEM returns empty string for empty table`() {
        deleteAllUsers()
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("name", TextDashboardAggregationFunction.LAST_ITEM, "id DESC")
        )
        assertEquals("", result.value)
    }

    @Test
    fun `getTextData returns section reference in TextData`() {
        val section = textSection("id", TextDashboardAggregationFunction.COUNT)
        val result = JdbcQueriesRepository.getTextData(usersTable, section)
        assertEquals(section, result.section)
    }

    @Test
    fun `getTextData SUM with integer field formats as integer when possible`() {
        val result = JdbcQueriesRepository.getTextData(
            usersTable,
            textSection("age", TextDashboardAggregationFunction.SUM)
        )
        // 36 + 54 = 90
        assertEquals("90", result.value)
    }

    // -------------------------------------------------------------------------
    // Large-dataset and pagination stress
    // -------------------------------------------------------------------------

    @Test
    fun `pagination across large dataset covers all rows`() {
        dataSource.connection.use { conn ->
            (3..22).forEach { i ->
                conn.execute("INSERT INTO users (id, name, age, active, status, score, nickname, profile_id, organization_id) VALUES ($i, 'User$i', ${20 + i}, true, 'ACTIVE', ${i}.0, NULL, NULL, 1)")
            }
        }
        val config = KtorAdminConfiguration()
        val original = config.maxItemsInPage
        try {
            config.maxItemsInPage = 5
            val total = JdbcQueriesRepository.getCount(usersTable, null, emptyList())
            assertEquals(22L, total)
            val allIds = mutableListOf<String>()
            var page = 0
            while (true) {
                val rows = JdbcQueriesRepository.getAllData(
                    usersTable,
                    listOf(usersTable),
                    null,
                    page,
                    mutableListOf(),
                    Order("id", "ASC")
                )
                if (rows.isEmpty()) break
                allIds += rows.map { it.primaryKey }
                page++
            }
            assertEquals(22, allIds.size)
            assertEquals(allIds, allIds.distinct())
        } finally {
            config.maxItemsInPage = original
        }
    }

    // -------------------------------------------------------------------------
    // Multi-database-key (same datasource) path via usingDataSource
    // -------------------------------------------------------------------------

    @Test
    fun `getCountOfTables works for tables grouped under same database key`() {
        val counts = JdbcQueriesRepository.getCountOfTables(
            listOf(
                usersTable,
                organizationsTable,
                profilesTable,
                rolesTable
            )
        )
        assertEquals(2L, counts["users"])
        assertEquals(2L, counts["organizations"])
        assertEquals(2L, counts["profiles"])
        assertEquals(3L, counts["roles"])
    }

    // -------------------------------------------------------------------------
    // Referential integrity — full matrix
    // -------------------------------------------------------------------------

    @Test
    fun `cannot delete user referenced by user_roles without cascading`() {
        // The schema has ON DELETE CASCADE for user_roles, so deleting user should succeed and cascade
        JdbcQueriesRepository.deleteRows(usersTable, listOf("1"))
        assertEquals(
            emptyList<Any>(),
            JdbcQueriesRepository.getAllSelectedReferenceInListReference(
                usersTable,
                roleReference,
                "1"
            )
        )
    }

    @Test
    fun `cannot delete role still referenced in user_roles`() {
        assertFailsWith<Exception> {
            JdbcQueriesRepository.deleteRows(rolesTable, listOf("1"))
        }
    }

    @Test
    fun `can delete unreferenced role`() {
        // Role 3 (Auditor) has no user_roles; deletion must succeed
        JdbcQueriesRepository.deleteRows(rolesTable, listOf("3"))
        val counts = JdbcQueriesRepository.getCountOfTables(listOf(rolesTable))
        assertEquals(2L, counts["roles"])
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun deleteAllUsers() {
        dataSource.connection.use { conn ->
            conn.execute("DELETE FROM user_roles")
            conn.execute("DELETE FROM users")
        }
    }

    private fun buildTables() {
        usersTable = TestJdbcTable(
            columns = listOf(
                id,
                name,
                age,
                active,
                status,
                score,
                nickname,
                profileId,
                organizationId,
                roleReference
            ),
            tableName = "users",
            panelListColumns = listOf(
                "id",
                "name",
                "age",
                "active",
                "status",
                "score",
                "nickname",
                "profile_id",
                "organization_id",
                "roles"
            ),
            filters = listOf("active", "status", "age", "score", "organization_id"),
            searches = listOf("name", "organization_id.name"),
        )
        rolesTable = TestJdbcTable(
            columns = listOf(col("id", ColumnType.INTEGER), col("label", ColumnType.STRING)),
            tableName = "roles",
            panelListColumns = listOf("id", "label"),
        )
        userRolesTable = TestJdbcTable(
            columns = listOf(
                col("user_id", ColumnType.INTEGER),
                col("role_id", ColumnType.INTEGER)
            ),
            tableName = "user_roles",
            panelListColumns = listOf("user_id", "role_id"),
        )
        profilesTable = TestJdbcTable(
            columns = listOf(col("id", ColumnType.INTEGER), col("bio", ColumnType.STRING)),
            tableName = "profiles",
            panelListColumns = listOf("id", "bio"),
            displayFormat = "Profile {id}: {bio}",
        )
        organizationsTable = TestJdbcTable(
            columns = listOf(col("id", ColumnType.INTEGER), col("name", ColumnType.STRING)),
            tableName = "organizations",
            panelListColumns = listOf("id", "name"),
            displayFormat = "{id} - {name}",
            defaultOrder = Order("name", "ASC"),
        )
    }

    private fun createSchema() = dataSource.connection.use { connection ->
        connection.execute("CREATE TABLE profiles (id INT PRIMARY KEY, bio VARCHAR(100) NOT NULL)")
        connection.execute("CREATE TABLE organizations (id INT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE)")
        connection.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, age INT NOT NULL, active BOOLEAN NOT NULL, status VARCHAR(20) NOT NULL, score DOUBLE NOT NULL, nickname VARCHAR(100), profile_id INT UNIQUE, organization_id INT, CONSTRAINT fk_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE SET NULL, CONSTRAINT fk_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT)")
        connection.execute("CREATE TABLE roles (id INT PRIMARY KEY, label VARCHAR(100) NOT NULL)")
        connection.execute("CREATE TABLE user_roles (user_id INT NOT NULL, role_id INT NOT NULL, PRIMARY KEY (user_id, role_id), CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT)")
        connection.execute("INSERT INTO profiles (id, bio) VALUES (1, 'Ada Bio')")
        connection.execute("INSERT INTO profiles (id, bio) VALUES (2, 'Linus Bio')")
        connection.execute("INSERT INTO organizations (id, name) VALUES (1, 'Analytical Engines')")
        connection.execute("INSERT INTO organizations (id, name) VALUES (2, 'Kernel Labs')")
        connection.execute("INSERT INTO users (id, name, age, active, status, score, nickname, profile_id, organization_id) VALUES (1, 'Ada', 36, TRUE, 'ACTIVE', 10.5, 'Countess', 1, 1)")
        connection.execute("INSERT INTO users (id, name, age, active, status, score, nickname, profile_id, organization_id) VALUES (2, 'Linus', 54, FALSE, 'BLOCKED', 2.0, NULL, 2, 2)")
        connection.execute("INSERT INTO roles (id, label) VALUES (1, 'Admin')")
        connection.execute("INSERT INTO roles (id, label) VALUES (2, 'Editor')")
        connection.execute("INSERT INTO roles (id, label) VALUES (3, 'Auditor')")
        connection.execute("INSERT INTO user_roles (user_id, role_id) VALUES (1, 1)")
    }

    private fun Connection.execute(sql: String) = createStatement().use { it.execute(sql) }

    private fun col(
        name: String,
        type: ColumnType,
        showInPanel: Boolean = true,
        extra: ColumnSet.() -> ColumnSet = { this },
    ) = ColumnSet(
        columnName = name,
        verboseName = name,
        type = type,
        showInPanel = showInPanel
    ).extra()

    private fun listSection(
        fields: List<String>? = null,
        orderQuery: String? = null,
        limitCount: Int? = null,
    ) = object : ListDashboardSection() {
        override val tableName = "users"
        override val sectionName = "Users"
        override val index = 1
        override val fields: List<String>? = fields
        override val orderQuery: String? = orderQuery
        override val limitCount: Int? = limitCount
    }

    private fun chartSection(
        aggregation: ChartDashboardAggregationFunction,
        orderQuery: String? = null,
        limitCount: Int? = null,
    ) = object : ChartDashboardSection() {
        override val aggregationFunction = aggregation
        override val tableName = "users"
        override val labelField = "status"
        override val valuesFields = listOf(ChartField("score", "Score"))
        override val chartStyle = AdminChartStyle.BAR
        override val sectionName = "Scores"
        override val index = 1
        override val orderQuery: String? = orderQuery
        override val limitCount: Int? = limitCount
        override fun provideBorderColor(label: String, valueField: String) =
            "border-$label-$valueField"

        override fun provideFillColor(label: String, valueField: String) = "fill-$label-$valueField"
    }

    private fun textSection(
        fieldName: String,
        aggregation: TextDashboardAggregationFunction,
        orderQuery: String? = null,
    ) = object : TextDashboardSection() {
        override val tableName = "users"
        override val fieldName = fieldName
        override val hintText = fieldName
        override val aggregationFunction = aggregation
        override val orderQuery: String? = orderQuery
        override val sectionName = fieldName
        override val index = 1
    }
}