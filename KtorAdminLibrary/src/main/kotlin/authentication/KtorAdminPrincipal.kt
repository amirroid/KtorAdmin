package authentication

/**
 * Represents the principal for authenticated admin users in the Ktor application.
 *
 * @property name The name or identifier of the authenticated admin user.
 * @property roles A list of access rules or permissions associated with the admin user.
 *                 Can be null if no specific rules are assigned.
 */
data class KtorAdminPrincipal(
    val name: String,
    val roles: List<String>? = null
)
