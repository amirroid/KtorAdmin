package ir.amirroid.ktoradmin.converters

import ir.amirroid.ktoradmin.column
import ir.amirroid.ktoradmin.models.field.FieldSet
import ir.amirroid.ktoradmin.models.types.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventsTest {
    @Test
    fun `should mark all column events changed when changed columns are not provided`() {
        val events = listOf(column("name"), column("age")).toEvents(listOf("Ada", 36))

        assertTrue(events.all { it.changed })
        assertEquals("Ada", events[0].value)
        assertEquals(36, events[1].value)
    }

    @Test
    fun `should mark only matching fields changed`() {
        val fields = listOf(FieldSet("title", type = FieldType.String), FieldSet("views", type = FieldType.Integer))

        val events = fields.toFieldEvents(listOf("Post", 10), changedFields = listOf("views"))

        assertFalse(events[0].changed)
        assertTrue(events[1].changed)
        assertEquals("views", events[1].fieldSet.fieldName)
    }
}
