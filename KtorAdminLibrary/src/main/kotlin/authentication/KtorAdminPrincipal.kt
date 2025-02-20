package authentication

/**
 * Represents the principal for authenticated admin users in the KtorAdmin.
 * This class is used to store authentication and authorization details for an admin user.
 *
 * @property name The name or identifier of the authenticated admin user.
 * @property roles A list of access rules or permissions associated with the admin user.
 *                 Can be null if no specific rules are assigned.
 * @property dashboardAccess A flag indicating whether the admin user has access to the admin dashboard.
 *                           Defaults to `true`, meaning access is granted by default.
 */
data class KtorAdminPrincipal(
    val name: String,
    val roles: List<String>? = null,
    val dashboardAccess: Boolean = true
)