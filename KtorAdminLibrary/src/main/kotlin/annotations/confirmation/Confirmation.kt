package annotations.confirmation

/**
 * Marks a field as requiring confirmation, meaning the user must enter the same value twice for verification.
 * This is typically used for fields such as passwords, email addresses, or phone numbers.
 * The actual validation is handled by the library and does not require a separate confirmation field to be stored.
 *
 * **Usage Example:**
 * ```kotlin
 * @Confirmation
 * val password = varchar("password", 200)
 * ```
 *
 * **Note:**
 * This annotation does not store a second confirmation field.
 * The validation is handled internally to ensure the entered value is confirmed correctly.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Confirmation