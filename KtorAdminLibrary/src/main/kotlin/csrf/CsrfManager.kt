package csrf

import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object CsrfManager {
    var tokenExpirationTime = 10 * 60 * 1000L

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val AES_ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_SIZE = 128

    private val secureRandom = SecureRandom()
    private val hmacSecretKey = generateRandomKey()  // Secret key for HMAC
    private val aesSecretKey = generateRandomKey()   // Secret key for AES encryption

    /**
     * Generates a secure random key (256-bit).
     */
    private fun generateRandomKey(): ByteArray {
        val key = ByteArray(32) // 256-bit key
        secureRandom.nextBytes(key)
        return key
    }

    /**
     * Generates a secure CSRF token including: UUID + Encrypted Timestamp + HMAC.
     */
    fun generateToken(): String {
        val uuid = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis().toString()
        val encryptedTimestamp = encryptTimestamp(timestamp)
        val rawToken = "$uuid:$encryptedTimestamp"
        val hmac = generateHmac(rawToken)
        return "$rawToken:$hmac"
    }

    /**
     * Validates the received CSRF token.
     */
    fun validateToken(token: String?): Boolean {
        if (token.isNullOrEmpty()) return false

        val parts = token.split(":")
        if (parts.size != 3) return false

        val uuid = parts[0]
        val encryptedTimestamp = parts[1]
        val receivedHmac = parts[2]

        // Verify HMAC integrity
        val expectedHmac = generateHmac("$uuid:$encryptedTimestamp")
        if (expectedHmac != receivedHmac) return false

        // Decrypt and validate the timestamp
        val decryptedTimestamp = decryptTimestamp(encryptedTimestamp)?.toLongOrNull() ?: return false
        return System.currentTimeMillis() - decryptedTimestamp <= tokenExpirationTime
    }

    /**
     * Generates an HMAC-SHA256 hash for integrity verification.
     */
    private fun generateHmac(data: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(hmacSecretKey, HMAC_ALGORITHM))
        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray()))
    }

    /**
     * Encrypts the timestamp using AES-GCM.
     */
    private fun encryptTimestamp(timestamp: String): String {
        val iv = ByteArray(GCM_IV_SIZE).apply { secureRandom.nextBytes(this) }
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesSecretKey, "AES"), GCMParameterSpec(GCM_TAG_SIZE, iv))
        val encryptedData = cipher.doFinal(timestamp.toByteArray())
        return Base64.getEncoder().encodeToString(iv + encryptedData)
    }

    /**
     * Decrypts the AES-GCM encrypted timestamp.
     */
    private fun decryptTimestamp(encryptedTimestamp: String): String? {
        return try {
            val decodedData = Base64.getDecoder().decode(encryptedTimestamp)
            val iv = decodedData.copyOfRange(0, GCM_IV_SIZE)
            val encryptedBytes = decodedData.copyOfRange(GCM_IV_SIZE, decodedData.size)

            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesSecretKey, "AES"), GCMParameterSpec(GCM_TAG_SIZE, iv))
            String(cipher.doFinal(encryptedBytes))
        } catch (e: Exception) {
            null // Return null if decryption fails
        }
    }
}