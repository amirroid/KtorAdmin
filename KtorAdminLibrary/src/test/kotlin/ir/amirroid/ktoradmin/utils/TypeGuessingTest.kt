package ir.amirroid.ktoradmin.utils

import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.models.types.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeGuessingTest {
    @Test
    fun `should guess supported jdbc column property types`() {
        assertEquals(ColumnType.INTEGER, guessPropertyType("kotlin.Int"))
        assertEquals(ColumnType.UINTEGER, guessPropertyType("kotlin.UInt"))
        assertEquals(ColumnType.STRING, guessPropertyType("kotlin.String"))
        assertEquals(ColumnType.BINARY, guessPropertyType("kotlin.ByteArray"))
        assertEquals(ColumnType.BIG_DECIMAL, guessPropertyType("java.math.BigDecimal"))
        assertEquals(ColumnType.DATETIME, guessPropertyType("kotlinx.datetime.Instant"))
        assertEquals(ColumnType.TIMESTAMP_WITH_TIMEZONE, guessPropertyType("java.time.OffsetDateTime"))
        assertEquals(ColumnType.NOT_AVAILABLE, guessPropertyType("com.example.Unknown"))
    }

    @Test
    fun `should guess supported mongo field property types`() {
        assertEquals(FieldType.String, guessFieldPropertyType("kotlin.String"))
        assertEquals(FieldType.Integer, guessFieldPropertyType("kotlin.Int"))
        assertEquals(FieldType.Decimal128, guessFieldPropertyType("java.math.BigDecimal"))
        assertEquals(FieldType.ObjectId, guessFieldPropertyType("org.bson.types.ObjectId"))
        assertEquals(FieldType.Instant, guessFieldPropertyType("kotlinx.datetime.Instant"))
        assertEquals(FieldType.DateTime, guessFieldPropertyType("java.time.LocalDateTime"))
        assertEquals(FieldType.Date, guessFieldPropertyType("java.time.LocalDate"))
        assertEquals(FieldType.List(emptyList()), guessFieldPropertyType("kotlin.collections.List"))
        assertEquals(FieldType.Map(emptyList()), guessFieldPropertyType("kotlin.collections.Map"))
        assertEquals(FieldType.NotAvailable, guessFieldPropertyType("com.example.Unknown"))
    }
}
