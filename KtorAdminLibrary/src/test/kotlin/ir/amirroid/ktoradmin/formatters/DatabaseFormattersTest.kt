package ir.amirroid.ktoradmin.formatters

import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.models.types.FieldType
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.Month
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseFormattersTest {
    @Test
    fun `formatToDisplayInTable should format timestamp with month abbreviation`() {
        val ldt = LocalDateTime.of(2026, Month.JANUARY, 15, 14, 30, 45)
        val timestamp = Timestamp.valueOf(ldt)

        val formattedDateTime = timestamp.formatToDisplayInTable(ColumnType.DATETIME)
        // Check day, month (Jan) and year are properly formatted
        assertEquals("15 Jan 2026 - 14:30:45", formattedDateTime)

        val formattedDate = timestamp.formatToDisplayInTable(ColumnType.DATE)
        assertEquals("15 Jan 2026", formattedDate)
    }

    @Test
    fun `formatToDisplayInCollection should format date with month abbreviation`() {
        val calendar =
            Calendar.getInstance(Locale.US).apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.FEBRUARY)
                set(Calendar.DAY_OF_MONTH, 20)
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 15)
                set(Calendar.SECOND, 30)
                set(Calendar.MILLISECOND, 0)
            }
        val date: Date = calendar.time

        val formattedDate = date.formatToDisplayInCollection(FieldType.Date)
        // Check day, month and year
        assert(formattedDate.contains("20") && formattedDate.contains("2026"))

        val formattedDateTime = date.formatToDisplayInCollection(FieldType.DateTime)
        assert(formattedDateTime.contains("20") && formattedDateTime.contains("2026") && formattedDateTime.contains("10:15:30"))
    }

    @Test
    fun `formatToDisplayInTable should handle null and binary values`() {
        val nullVal: Any? = null
        assertEquals("N/A", nullVal.formatToDisplayInTable(ColumnType.DATETIME))

        val byteArray = byteArrayOf(1, 2, 3)
        assertEquals("Byte array object", byteArray.formatToDisplayInTable(ColumnType.BINARY))
    }
}
