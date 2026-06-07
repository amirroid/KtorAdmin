package ir.amirroid.ktoradmin.validators

import ir.amirroid.ktoradmin.TestJdbcTable
import ir.amirroid.ktoradmin.models.ColumnSet
import ir.amirroid.ktoradmin.models.Limit
import ir.amirroid.ktoradmin.models.field.FieldSet
import ir.amirroid.ktoradmin.models.types.ColumnType
import ir.amirroid.ktoradmin.models.types.FieldType
import ir.amirroid.ktoradmin.translator.locals.en.EnglishKtorAdminTranslator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidatorsTest {
    private val translator = EnglishKtorAdminTranslator
    private val table = TestJdbcTable()

    @Test
    fun `should reject null value for non nullable column`() {
        val column = columnSet("name", ColumnType.STRING, nullable = false)

        val error = Validators.validateColumnParameter(table, column, null, null, translator)

        assertEquals("The field cannot be null", error)
    }

    @Test
    fun `should allow null value for nullable non string column when empty string is submitted`() {
        val column = columnSet("age", ColumnType.INTEGER, nullable = true)

        val error = Validators.validateColumnParameter(table, column, "", null, translator)

        assertNull(error)
    }

    @Test
    fun `should reject blank column when blank values are disabled`() {
        val column = columnSet("name", ColumnType.STRING, blank = false)

        val error = Validators.validateColumnParameter(table, column, "   ", null, translator)

        assertEquals("The field cannot be empty", error)
    }

    @Test
    fun `should validate string length and regex boundaries`() {
        val column = columnSet("code", ColumnType.STRING, limits = Limit(minLength = 2, maxLength = 4, regexPattern = "[A-Z]+"))

        assertNull(Validators.validateColumnParameter(table, column, "AB", null, translator))
        assertEquals("Value exceeds maximum length of 4", Validators.validateColumnParameter(table, column, "ABCDE", null, translator))
        assertEquals("Value is shorter than minimum length of 2", Validators.validateColumnParameter(table, column, "A", null, translator))
        assertEquals(
            "Value does not match the required pattern ([A-Z]+)",
            Validators.validateColumnParameter(table, column, "ab", null, translator),
        )
    }

    @Test
    fun `should validate signed and unsigned integer ranges`() {
        val signed = columnSet("count", ColumnType.INTEGER, limits = Limit(minCount = -2.0, maxCount = 2.0))
        val unsigned = columnSet("count", ColumnType.UINTEGER, limits = Limit(minCount = 0.0, maxCount = 2.0))

        assertNull(Validators.validateColumnParameter(table, signed, "2", null, translator))
        assertEquals("Value exceeds maximum count of 2", Validators.validateColumnParameter(table, signed, "3", null, translator))
        assertEquals("The provided value is not valid.", Validators.validateColumnParameter(table, unsigned, "-1", null, translator))
        assertEquals("Value exceeds maximum count of 2", Validators.validateColumnParameter(table, unsigned, "3", null, translator))
    }

    @Test
    fun `should validate decimal char boolean and enumeration values`() {
        assertNull(Validators.validateColumnParameter(table, columnSet("price", ColumnType.BIG_DECIMAL), "12.50", null, translator))
        assertEquals(
            "The provided value is not valid.",
            Validators.validateColumnParameter(table, columnSet("price", ColumnType.BIG_DECIMAL), "x", null, translator),
        )
        assertNull(Validators.validateColumnParameter(table, columnSet("initial", ColumnType.CHAR), "A", null, translator))
        assertEquals(
            "The provided value is not valid.",
            Validators.validateColumnParameter(table, columnSet("initial", ColumnType.CHAR), "AB", null, translator),
        )
        assertNull(Validators.validateColumnParameter(table, columnSet("enabled", ColumnType.BOOLEAN), "on", null, translator))
        assertEquals(
            "The provided value is not valid.",
            Validators.validateColumnParameter(table, columnSet("enabled", ColumnType.BOOLEAN), "yes", null, translator),
        )
        assertNull(
            Validators.validateColumnParameter(
                table,
                columnSet("status", ColumnType.ENUMERATION, enumerationValues = listOf("ACTIVE")),
                "ACTIVE",
                null,
                translator,
            ),
        )
        assertEquals(
            "Value should be one of ACTIVE",
            Validators.validateColumnParameter(
                table,
                columnSet("status", ColumnType.ENUMERATION, enumerationValues = listOf("ACTIVE")),
                "DELETED",
                null,
                translator,
            ),
        )
    }

    @Test
    fun `should validate date datetime and duration formats`() {
        assertNull(Validators.validateColumnParameter(table, columnSet("birthday", ColumnType.DATE), "2025-01-31", null, translator))
        assertEquals(
            "The provided value is not valid.",
            Validators.validateColumnParameter(table, columnSet("birthday", ColumnType.DATE), "31-01-2025", null, translator),
        )
        assertNull(
            Validators.validateColumnParameter(table, columnSet("created", ColumnType.DATETIME), "2025-01-31T10:15", null, translator),
        )
        assertEquals(
            "The provided value is not valid.",
            Validators.validateColumnParameter(table, columnSet("created", ColumnType.DATETIME), "2025-01-31 10:15", null, translator),
        )
        assertNull(Validators.validateColumnParameter(table, columnSet("duration", ColumnType.DURATION), "1h", null, translator))
        assertEquals(
            "Value should be a valid duration format.",
            Validators.validateColumnParameter(table, columnSet("duration", ColumnType.DURATION), "one hour", null, translator),
        )
    }

    @Test
    fun `should validate field parameters with nullable and decimal rules`() {
        val nullableInteger = FieldSet("age", type = FieldType.Integer, nullable = true)
        val decimal = FieldSet("amount", type = FieldType.Decimal128, limits = Limit(minCount = 1.0, maxCount = 5.0))

        assertNull(Validators.validateFieldParameter(nullableInteger, "", translator))
        assertNull(Validators.validateFieldParameter(decimal, "5", translator))
        assertEquals("Value exceeds 5.0", Validators.validateFieldParameter(decimal, "6", translator))
        assertEquals("Value should be a valid Decimal128 number", Validators.validateFieldParameter(decimal, "bad", translator))
    }

    @Test
    fun `should validate file size and mime type`() {
        val limits = Limit(maxBytes = 5, allowedMimeTypes = listOf("image/png"))

        assertNull(Validators.validateBytesSize(5, limits, translator))
        assertEquals("File size exceeds 5", Validators.validateBytesSize(6, limits, translator))
        assertNull(Validators.validateMimeType("avatar.png", limits, translator))
        assertEquals(
            "Invalid MIME type for file avatar.txt. Allowed types are image/png",
            Validators.validateMimeType("avatar.txt", limits, translator),
        )
    }

    private fun columnSet(
        name: String,
        type: ColumnType,
        nullable: Boolean = false,
        blank: Boolean = true,
        limits: Limit? = null,
        enumerationValues: List<String>? = null,
    ) = ColumnSet(
        columnName = name,
        verboseName = name,
        type = type,
        nullable = nullable,
        blank = blank,
        limits = limits,
        enumerationValues = enumerationValues,
    )
}
