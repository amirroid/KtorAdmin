package ir.amirroid.ktoradmin.formatters

import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayFormatsTest {
    @Test
    fun `should extract placeholders in order including duplicates`() {
        val keys = "Hello {first} {last}, again {first}".extractTextInCurlyBraces()

        assertEquals(listOf("first", "last", "first"), keys)
    }

    @Test
    fun `should populate known placeholders and leave missing placeholders unchanged`() {
        val result = populateTemplate(
            "{first} {last} - {missing}",
            mapOf("first" to "Ada", "last" to "Lovelace", "missing" to null),
        )

        assertEquals("Ada Lovelace - {missing}", result)
    }

    @Test
    fun `should support unicode replacement values`() {
        assertEquals("Hello سلام", populateTemplate("Hello {name}", mapOf("name" to "سلام")))
    }
}
