package ir.amirroid.ktoradmin.models.forms

/**
 * Data class representing a login form field.
 *
 * @param name The display name of the field (e.g., "Username" or "Email").
 * @param key The unique key for submitting the field to the server (e.g., "username" or "email").
 * @param type The input type (default is "text", can be "password", "email", "number", etc.).
 * @param autoComplete The autocomplete attribute for the browser (e.g., "username", "current-password", or "email").
 */
data class LoginFiled(
    val name: String,
    val key: String,
    val type: String = "text",
    val autoComplete: String? = null
)
