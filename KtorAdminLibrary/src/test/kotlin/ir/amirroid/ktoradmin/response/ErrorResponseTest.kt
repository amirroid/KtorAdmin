package ir.amirroid.ktoradmin.response

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorResponseTest {
    @Test
    fun `should convert error responses to map by field with later duplicates winning`() {
        val first = ErrorResponse("name", listOf("required"))
        val second = ErrorResponse("name", listOf("too short"))
        val email = ErrorResponse("email", listOf("invalid"))

        val map = listOf(first, email, second).toMap()

        assertEquals(setOf("name", "email"), map.keys)
        assertEquals(second, map["name"])
        assertEquals(email, map["email"])
    }
}
