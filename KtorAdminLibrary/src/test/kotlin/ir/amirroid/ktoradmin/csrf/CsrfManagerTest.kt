package ir.amirroid.ktoradmin.csrf

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CsrfManagerTest {
    @Test
    fun `should generate token that validates successfully`() {
        val token = CsrfManager.generateToken()

        assertTrue(CsrfManager.validateToken(token))
    }

    @Test
    fun `should reject missing malformed and tampered tokens`() {
        val token = CsrfManager.generateToken()
        val parts = token.split(":")
        val tampered = listOf(parts[0], parts[1], parts[2].reversed()).joinToString(":")

        assertFalse(CsrfManager.validateToken(null))
        assertFalse(CsrfManager.validateToken(""))
        assertFalse(CsrfManager.validateToken("one:two"))
        assertFalse(CsrfManager.validateToken(tampered))
    }

    @Test
    fun `should reject token after expiration window`() {
        val original = CsrfManager.tokenExpirationTime
        try {
            CsrfManager.tokenExpirationTime = -1
            val token = CsrfManager.generateToken()

            assertFalse(CsrfManager.validateToken(token))
        } finally {
            CsrfManager.tokenExpirationTime = original
        }
    }
}
