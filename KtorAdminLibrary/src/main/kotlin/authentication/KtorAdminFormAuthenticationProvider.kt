package authentication

import configuration.DynamicConfiguration
import crypto.CryptoManager
import csrf.CSRF_TOKEN_FIELD_NAME
import csrf.CsrfManager
import flash.REQUEST_ID_FORM
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.forms.UserForm
import models.forms.toUserForm
import utils.invalidateRequest

/**
 * Key used to identify the authentication challenge for admin form authentication.
 * This is used internally to handle authentication failures for admin-related routes.
 */
private const val adminFormAuthenticationChallengeKey = "AdminFormAuthenticationChallenge"

/**
 * Custom authentication provider for admin form-based authentication in KtorAdmin.
 *
 * This provider handles authentication for admin routes, validates user credentials,
 * manages session-based authentication, and provides a configurable challenge function for failed authentication.
 *
 * @property authenticateFunction Function responsible for validating authentication credentials.
 * @property challengeFunction Function invoked when authentication fails to handle the challenge process.
 */
class KtorAdminFormAuthProvider internal constructor(
    config: KtorAdminFormAuthConfig
) : AuthenticationProvider(config) {

    // Function to validate authentication credentials
    private val authenticateFunction: AuthenticationFunction<UserForm> = config.authenticationFunction

    // Function to handle authentication failure challenges
    private val challengeFunction: KtorAdminAuthChallengeFunction = config.challengeFunction

    // Instance of CryptoManager for handling session encryption and decryption
    private val cryptoManager = CryptoManager()

    /**
     * Retrieves the authenticated user from session storage.
     *
     * @return The user object stored in the session, or null if no user is present.
     */
    private fun ApplicationCall.getUserFromSessions(): UserForm? {
        return (sessions.get(USER_SESSIONS) as? String)?.let {
            runCatching { cryptoManager.decryptData(it) }.getOrNull()?.let {
                runCatching { Json.decodeFromString<UserForm>(it) }.getOrNull()
            }
        }
    }

    /**
     * Called during the authentication process to validate user credentials and set the principal.
     *
     * If the request is for `/admin/login` with a POST method, the function attempts to parse the
     * user credentials from the request body. Otherwise, it checks the session for existing credentials.
     *
     * @param context The [AuthenticationContext] containing the current call and challenge state.
     */
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call

        // Check if the request is for "/${DynamicConfiguration.adminPath}/login" with the POST method
        val inLoginUrl =
            call.request.uri.substringBefore("?") == "/${DynamicConfiguration.adminPath}/login" && call.request.httpMethod == HttpMethod.Post

        // Extract user credentials from the session or the request body
        val formParameters = call.takeIf { inLoginUrl }?.receiveParameters()?.apply {
            val csrfToken = get(CSRF_TOKEN_FIELD_NAME)
            if (CsrfManager.validateToken(csrfToken).not()) {
                return call.invalidateRequest()
            }
        }?.toUserForm()

        val userSession = call.getUserFromSessions()
        val parameters = userSession ?: formParameters

        // Validate the user credentials
        val principal = parameters?.let { authenticateFunction.invoke(call, it) }

        if (principal != null) {
            // If authentication is successful, set the principal
            context.principal(name, principal)

            // Update the session with new credentials if logging in
            if (inLoginUrl && formParameters != userSession) {
                formParameters?.let {
                    val encryptedData = cryptoManager.encryptData(Json.encodeToString(it))
                    call.sessions.set(USER_SESSIONS, encryptedData)
                }
            }
            return
        }

        // Clear session if authentication fails
        if (userSession != null) {
            call.sessions.clear<UserForm>()
        }

        // Determine the cause of authentication failure
        val cause = when {
            parameters.isNullOrEmpty() -> AuthenticationFailedCause.NoCredentials
            else -> AuthenticationFailedCause.InvalidCredentials
        }

        // Handle authentication failure with the configured challenge function
        context.challenge(adminFormAuthenticationChallengeKey, cause) { challenge, challengeCall ->
            challengeFunction.invoke(KtorAdminAuthChallengeContext(challengeCall), parameters)
            if (!challenge.completed && challengeCall.response.status() != null) {
                challenge.complete()
            }
        }
    }

    /**
     * Configuration class for the custom admin form authentication provider.
     *
     * This class allows customization of the authentication and challenge logic for admin routes.
     *
     * @property authenticationFunction Function to validate the credentials.
     * @property challengeFunction Function that handles challenges when authentication fails.
     */
    class KtorAdminFormAuthConfig internal constructor(name: String? = null) : Config(name) {

        // Function to validate user credentials
        internal var authenticationFunction: AuthenticationFunction<UserForm> = { null }

        // Default challenge function to redirect to the admin login page
        internal var challengeFunction: KtorAdminAuthChallengeFunction = {
            redirectToLogin(call, it?.get(REQUEST_ID_FORM))
        }

        /**
         * Defines a custom challenge function for handling failed authentication.
         *
         * @param function A function that specifies the behavior for failed authentication.
         */
        private fun challenge(function: KtorAdminAuthChallengeFunction) {
            challengeFunction = function
        }

        /**
         * Specifies a redirect URL to be used in case of failed authentication.
         *
         * @param redirectUrl The URL to redirect the user to upon failed authentication.
         */
        private fun challenge(redirectUrl: String) {
            challenge {
                call.respondRedirect(redirectUrl)
            }
        }

        /**
         * Sets the validation function responsible for checking authentication credentials.
         *
         * @param body The validation logic that takes a [UserForm] and returns a principal on success.
         */
        fun validate(body: suspend ApplicationCall.(UserForm) -> Any?) {
            authenticationFunction = body
        }

        /**
         * Builds the admin form authentication provider using the current configuration.
         *
         * @return A configured instance of [KtorAdminFormAuthProvider].
         */
        internal fun build() = KtorAdminFormAuthProvider(this)
    }
}

/**
 * Context for handling authentication challenges specific to the admin form authentication provider.
 *
 * This provides access to the [ApplicationCall] during challenge execution.
 *
 * @property call The current [ApplicationCall].
 */
class KtorAdminAuthChallengeContext(val call: ApplicationCall)

/**
 * A type alias for the admin form authentication challenge function.
 *
 * The function takes a [KtorAdminAuthChallengeContext] and optional credentials as parameters.
 */
typealias KtorAdminAuthChallengeFunction = suspend KtorAdminAuthChallengeContext.(UserForm?) -> Unit

/**
 * Registers the custom admin form authentication provider in the Ktor [AuthenticationConfig].
 *
 * This extension function allows you to easily add and configure the `ktorAdminFormAuth` authentication provider.
 *
 * @param name The name of the authentication provider.
 * @param configure A configuration block for customizing the admin authentication behavior.
 */
fun AuthenticationConfig.ktorAdminFormAuth(
    name: String,
    configure: KtorAdminFormAuthProvider.KtorAdminFormAuthConfig.() -> Unit
) {
    val provider = KtorAdminFormAuthProvider.KtorAdminFormAuthConfig(name).apply(configure).build()
    register(provider)
}