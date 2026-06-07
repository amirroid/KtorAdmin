package ir.amirroid.ktoradmin.filters

import io.ktor.http.Parameters
import ir.amirroid.ktoradmin.TestJdbcTable
import ir.amirroid.ktoradmin.column
import ir.amirroid.ktoradmin.configuration.KtorAdminConfiguration
import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.models.filters.FilterTypes
import ir.amirroid.ktoradmin.models.types.ColumnType
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JdbcFiltersTest {
    @Test
    fun `should create filter metadata for date datetime boolean and enumeration columns`() {
        val columns =
            listOf(
                column("created", ColumnType.DATE),
                column("updated", ColumnType.DATETIME),
                column("enabled", ColumnType.BOOLEAN),
                column("status", ColumnType.ENUMERATION) { copy(enumerationValues = listOf("ACTIVE", "BLOCKED")) },
            )
        val table = TestJdbcTable(columns = columns, filters = columns.map { it.columnName })

        val filters = JdbcFilters.findFiltersData(table, listOf(table))

        assertEquals(listOf(FilterTypes.DATE, FilterTypes.DATETIME, FilterTypes.BOOLEAN, FilterTypes.ENUMERATION), filters.map { it.type })
        assertEquals(listOf("ACTIVE", "BLOCKED"), filters.last().values)
    }

    @Test
    fun `should reject unsupported jdbc filter types`() {
        val table = TestJdbcTable(columns = listOf(column("name", ColumnType.STRING)), filters = listOf("name"))

        assertFailsWith<IllegalArgumentException> {
            JdbcFilters.findFiltersData(table, listOf(table))
        }
    }

    @Test
    fun `should extract date range boolean enum and reference filters`() {
        val originalZone = KtorAdminConfiguration().zoneId
        KtorAdminConfiguration().zoneId = ZoneId.of("UTC")
        try {
            val created = column("created", ColumnType.DATE)
            val enabled = column("enabled", ColumnType.BOOLEAN)
            val status = column("status", ColumnType.ENUMERATION) { copy(enumerationValues = listOf("ACTIVE")) }
            val userId = column("user_id", ColumnType.INTEGER) { copy(reference = Reference.ManyToOne("users", "id")) }
            val table =
                TestJdbcTable(
                    columns = listOf(created, enabled, status, userId),
                    filters = listOf("created", "enabled", "status", "user_id"),
                )
            val parameters =
                Parameters.build {
                    append("filters.created-start", Instant.parse("2025-01-01T00:00:00Z").toEpochMilli().toString())
                    append("filters.created-end", Instant.parse("2025-01-31T00:00:00Z").toEpochMilli().toString())
                    append("filters.enabled", "on")
                    append("filters.status", "ACTIVE")
                    append("filters.user_id", "42")
                }

            val filters = JdbcFilters.extractFilters(table, listOf(table), parameters)

            assertEquals(listOf(">=", "<=", "= ", "= ", "= "), filters.map { it.second })
            assertEquals(listOf("created", "created", "enabled", "status", "user_id"), filters.map { it.first.columnName })
            assertEquals(true, filters[2].third)
            assertEquals("ACTIVE", filters[3].third)
            assertEquals(42, filters[4].third)
        } finally {
            KtorAdminConfiguration().zoneId = originalZone
        }
    }
}
