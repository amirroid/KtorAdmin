package ir.amirroid.ktoradmin.crypto

import ir.amirroid.ktoradmin.configuration.KtorAdminConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.days

class CryptoManagerTest {
    private val crypto = CryptoManager()

    @Test
    fun `should decrypt encrypted data back to original plaintext`() {
        val encrypted = crypto.encryptData("user:admin:unicode:سلام")

        val decrypted = crypto.decryptData(encrypted)

        assertEquals("user:admin:unicode:سلام", decrypted)
    }

    @Test
    fun `should produce different ciphertexts for same plaintext`() {
        val first = crypto.encryptData("same-value")
        val second = crypto.encryptData("same-value")

        assertNotEquals(first, second)
        assertEquals("same-value", crypto.decryptData(first))
        assertEquals("same-value", crypto.decryptData(second))
    }

    @Test
    fun `should reject tampered ciphertext`() {
        val encrypted = crypto.encryptData("sensitive")
        val tampered = encrypted.dropLast(2) + "AA"

        val decrypted = runCatching { crypto.decryptData(tampered) }.getOrNull()

        assertNull(decrypted)
    }

    @Test
    fun `should reject expired ciphertext`() {
        val configuration = KtorAdminConfiguration()
        val original = configuration.authenticationSessionMaxAge
        try {
            configuration.authenticationSessionMaxAge = 1.days
            val encrypted = crypto.encryptData("short-lived")
            configuration.authenticationSessionMaxAge = (-1).milliseconds

            assertNull(crypto.decryptData(encrypted))
        } finally {
            configuration.authenticationSessionMaxAge = original
        }
    }

    @Test
    fun `should throw when ciphertext is not valid base64`() {
        assertFailsWith<IllegalArgumentException> {
            crypto.decryptData("not base64 !!!")
        }
    }
}
