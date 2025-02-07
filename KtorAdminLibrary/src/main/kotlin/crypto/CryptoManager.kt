package crypto

import configuration.DynamicConfiguration
import java.nio.ByteBuffer
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
        private const val AES_KEY_SIZE = 256 // AES key size in bits
        private const val GCM_IV_SIZE = 12   // 12-byte IV for AES-GCM encryption
        private const val GCM_TAG_SIZE = 128 // 128-bit authentication tag
        private const val SALT_SIZE = 16  // 16-byte salt for key derivation
        private const val HMAC_SIZE = 32  // 32-byte HMAC for data integrity
        private const val TIMESTAMP_SIZE = Long.SIZE_BYTES // 8-byte timestamp
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val PBKDF2_ITERATIONS = 100_000 // Secure key derivation iterations

        private val EXPIRATION_TIME_MS: Long
            get() = DynamicConfiguration.authenticationSessionMaxAge.inWholeMilliseconds

        private val secureRandom = SecureRandom()
        private val salt: ByteArray by lazy { generateSalt() }
        private val secretKey: SecretKey by lazy { deriveAESKey() }
        private val hmacKey: SecretKey by lazy { deriveHMACKey() }

        /**
         * Generates a random salt.
         */
        private fun generateSalt(): ByteArray {
            return ByteArray(SALT_SIZE).apply { secureRandom.nextBytes(this) }
        }

        /**
         * Derives AES key using PBKDF2 with a fixed passphrase.
         */
        private fun deriveAESKey(): SecretKey {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec("FixedPassphrase".toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
            return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        }

        /**
         * Derives HMAC key separately for integrity verification.
         */
        private fun deriveHMACKey(): SecretKey {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec("HMACPassphrase".toCharArray(), salt, PBKDF2_ITERATIONS, HMAC_SIZE * 8)
            return SecretKeySpec(factory.generateSecret(spec).encoded, HMAC_ALGORITHM)
        }
    }

    /**
     * Retrieves the secure current time in milliseconds.
     */
    private fun getSecureTime(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Generates an HMAC for data integrity verification.
     */
    private fun generateHMAC(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM).apply { init(SecretKeySpec(key, HMAC_ALGORITHM)) }
        return mac.doFinal(data)
    }

    /**
     * Encrypts the given data using AES-GCM and generates an HMAC.
     * The output format: Base64(IV + salt + timestamp + encryptedData + HMAC).
     */
    fun encryptData(data: String): String {
        val iv = ByteArray(GCM_IV_SIZE).apply { secureRandom.nextBytes(this) }
        val timestamp = getSecureTime()

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_SIZE, iv))
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Convert timestamp to byte array
        val timestampBytes = ByteBuffer.allocate(TIMESTAMP_SIZE).putLong(timestamp).array()

        // Concatenate IV + salt + timestamp + encrypted data
        val combined = iv + salt + timestampBytes + encryptedData

        // Generate HMAC for integrity
        val hmac = generateHMAC(combined, hmacKey.encoded)

        // Encode final data
        return Base64.getEncoder().encodeToString(combined + hmac)
    }

    /**
     * Decrypts the given AES-GCM encrypted data and verifies the HMAC.
     * Returns null if data is expired, modified, or integrity check fails.
     */
    fun decryptData(encryptedData: String): String? {
        val decodedData = Base64.getDecoder().decode(encryptedData)

        if (decodedData.size < GCM_IV_SIZE + SALT_SIZE + TIMESTAMP_SIZE + HMAC_SIZE) {
            return null // Invalid data
        }

        // Extract IV, salt, timestamp, encrypted bytes, and HMAC
        val iv = decodedData.copyOfRange(0, GCM_IV_SIZE)
        val extractedSalt = decodedData.copyOfRange(GCM_IV_SIZE, GCM_IV_SIZE + SALT_SIZE)
        val timestampBytes = decodedData.copyOfRange(GCM_IV_SIZE + SALT_SIZE, GCM_IV_SIZE + SALT_SIZE + TIMESTAMP_SIZE)
        val encryptedBytes = decodedData.copyOfRange(GCM_IV_SIZE + SALT_SIZE + TIMESTAMP_SIZE, decodedData.size - HMAC_SIZE)
        val storedHMAC = decodedData.copyOfRange(decodedData.size - HMAC_SIZE, decodedData.size)

        // Convert timestamp bytes back to long
        val timestamp = ByteBuffer.wrap(timestampBytes).long

        // Check if the data has expired
        if (getSecureTime() - timestamp > EXPIRATION_TIME_MS) {
            return null // Data expired
        }

        // Verify HMAC integrity
        val calculatedHMAC = generateHMAC(decodedData.copyOfRange(0, decodedData.size - HMAC_SIZE), hmacKey.encoded)
        if (!calculatedHMAC.contentEquals(storedHMAC)) {
            return null // Integrity check failed
        }

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_SIZE, iv))
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return decryptedBytes.toString(Charsets.UTF_8)
    }
}