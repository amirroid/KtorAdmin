package ir.amirroid.ktoradmin.modules.autocomplete

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoCompleteModelsTest {

    @Test
    fun `AutoCompleteRequest defaults`() {
        val request = AutoCompleteRequest()
        assertEquals("", request.search)
        assertEquals(0, request.page)
    }

    @Test
    fun `AutoCompleteRequest custom values`() {
        val request = AutoCompleteRequest(
            search = "test",
            page = 2,
        )
        assertEquals("test", request.search)
        assertEquals(2, request.page)
    }

    @Test
    fun `AutoCompleteItem creation`() {
        val item = AutoCompleteItem(
            key = "1",
            label = "Test User",
        )
        assertEquals("1", item.key)
        assertEquals("Test User", item.label)
    }

    @Test
    fun `AutoCompleteResponse creation`() {
        val items = listOf(
            AutoCompleteItem("1", "User 1"),
            AutoCompleteItem("2", "User 2"),
        )
        val response = AutoCompleteResponse(
            items = items,
            totalCount = 2,
        )
        assertEquals(2, response.items.size)
        assertEquals(2, response.totalCount)
        assertEquals("1", response.items[0].key)
        assertEquals("User 2", response.items[1].label)
    }

    @Test
    fun `AutoCompleteResponse empty items`() {
        val response = AutoCompleteResponse(
            items = emptyList(),
            totalCount = 0,
        )
        assertTrue(response.items.isEmpty())
        assertEquals(0, response.totalCount)
    }
}
