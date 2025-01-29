package crypto

import configuration.DynamicConfiguration
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal class CryptoManager {

    companion object {
        private const val AES_KEY_SIZE = 256 // 256-bit AES key
        private const val GCM_IV_SIZE = 12   // 12-byte IV for AES-GCM
        private const val GCM_TAG_SIZE = 128 // 128-bit authentication tag
        private const val PBKDF2_ITERATIONS = 100_000 // High iteration count for PBKDF2
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val SALT_SIZE = 16  // 16-byte salt for PBKDF2
    }

    private val secureRandom = SecureRandom()

    /**
     * Generates an AES key from the given password using PBKDF2.
     * A salt is used to prevent rainbow table attacks.
     */
    private fun generateAESKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts the given data using AES-GCM and PBKDF2.
     * The output consists of salt + IV + encrypted data.
     */
    fun encryptData(data: String, password: String? = DynamicConfiguration.cryptoPassword): String {
        if (password == null) {
            return data
        }
        val salt = ByteArray(SALT_SIZE).apply { secureRandom.nextBytes(this) }
        val key = generateAESKey(password, salt)
        val iv = ByteArray(GCM_IV_SIZE).apply { secureRandom.nextBytes(this) }

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE, iv))
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Concatenate salt + IV + encrypted data and encode in Base64
        val combined = salt + iv + encryptedData
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts the given AES-GCM encrypted data.
     * Extracts salt and IV before decryption.
     */
    fun decryptData(encryptedData: String, password: String? = DynamicConfiguration.cryptoPassword): String {
        if (password == null) {
            return encryptedData
        }
        val decodedData = Base64.getDecoder().decode(encryptedData)

        // Extract salt, IV, and encrypted data
        val salt = decodedData.copyOfRange(0, SALT_SIZE)
        val iv = decodedData.copyOfRange(SALT_SIZE, SALT_SIZE + GCM_IV_SIZE)
        val encryptedBytes = decodedData.copyOfRange(SALT_SIZE + GCM_IV_SIZE, decodedData.size)

        val key = generateAESKey(password, salt)

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE, iv))
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return decryptedBytes.toString(Charsets.UTF_8)
    }

    /**
     * Generates an HMAC for the given data using the specified password.
     */
    private fun generateHMAC(data: String, password: String): String {
        val key = SecretKeySpec(password.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM)
        val mac = Mac.getInstance(HMAC_ALGORITHM).apply { init(key) }
        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }

    /**
     * Verifies if the provided HMAC matches the expected value.
     */
    fun verifyHMAC(data: String, expectedHMAC: String, password: String): Boolean {
        return generateHMAC(data, password) == expectedHMAC
    }
}