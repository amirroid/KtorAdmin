package authentication

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import models.forms.UserForm
import models.forms.toUserForm
import utils.baseUrl

/**
 * Key used to identify the admin authentication challenge.
 * This is used internally to handle authentication failures for admin routes.
 */
private const val adminAuthenticationChallengeKey = "AdminAuthenticationChallenge"

/**
 * Custom authentication provider for admin-specific authentication in Ktor.
 *
 * This class is designed to handle authentication for admin routes. It validates user credentials,
 * handles session-based authentication, and provides a customizable challenge function for failed authentication.
 *
 * @property authenticateFunction A function responsible for validating the authentication credentials.
 * @property challengeFunction A function invoked when authentication fails to handle the challenge process.
 */
class KtorAdminAuthenticationProvider internal constructor(
    config: KtorAdminAuthenticationConfig
) : AuthenticationProvider(config) {

    // Function to validate authentication credentials
    private val authenticateFunction: AuthenticationFunction<UserForm> = config.authenticationFunction

    // Function to handle challenges for failed authentication
    private val challengeFunction: KtorAdminAuthChallengeFunction = config.challengeFunction

    /**
     * Retrieves the authenticated user from session storage.
     *
     * @return The user object stored in the session, or null if no user is present.
     */
    private fun ApplicationCall.getUserFromSessions() = sessions.get<UserForm>()

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

        // Check if the request is for "/admin/login" with the POST method
        val inLoginUrl =
            call.request.uri.substringBefore("?") == "/admin/login" && call.request.httpMethod == HttpMethod.Post
        // Extract user parameters from the session or the request body
        val formParameters = call.takeIf { inLoginUrl }?.receiveParameters()?.toUserForm()
        val userSession = call.getUserFromSessions()
        val parameters = userSession ?: formParameters

        // Validate the user credentials
        val principal = parameters?.let { authenticateFunction.invoke(call, it) }

        if (principal != null) {
            // If authentication is successful, set the principal
            context.principal(name, principal)

            // Update the session with new credentials if logging in
            if (inLoginUrl && formParameters != userSession) {
                formParameters?.let { call.sessions.set(it) }
            }
            return
        }

        if (userSession != null) {
            call.sessions.clear<UserForm>()
        }

        // Determine the cause of authentication failure
        val cause = when {
            parameters.isNullOrEmpty() -> AuthenticationFailedCause.NoCredentials
            else -> AuthenticationFailedCause.InvalidCredentials
        }

        // Handle authentication failure with the configured challenge function
        context.challenge(adminAuthenticationChallengeKey, cause) { challenge, challengeCall ->
            challengeFunction.invoke(KtorAdminAuthChallengeContext(challengeCall), parameters)
            if (!challenge.completed && challengeCall.response.status() != null) {
                challenge.complete()
            }
        }
    }

    /**
     * Configuration class for the custom admin authentication provider.
     *
     * This class allows customization of the authentication and challenge logic for the admin
     * authentication provider.
     *
     * @property authenticationFunction A validation function to authenticate the credentials.
     * @property challengeFunction A function that handles challenges when authentication fails.
     */
    class KtorAdminAuthenticationConfig internal constructor(name: String? = null) : Config(name) {

        // Function to validate user credentials
        internal var authenticationFunction: AuthenticationFunction<UserForm> = { null }

        // Default challenge function to redirect to the admin login page
        internal var challengeFunction: KtorAdminAuthChallengeFunction = {
            val originUrl = if (call.request.uri.startsWith("/admin/login")) {
                URLBuilder(call.request.uri).parameters["origin"].orEmpty()
            } else {
                URLBuilder(call.baseUrl + call.request.uri).apply {
                    if (parameters.contains("origin")) {
                        parameters.remove("origin")
                    }
                }.buildString()
            }
            call.respondRedirect("${call.baseUrl}/admin/login?origin=$originUrl")
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
         * Builds the admin authentication provider using the current configuration.
         *
         * @return A configured instance of [KtorAdminAuthenticationProvider].
         */
        internal fun build() = KtorAdminAuthenticationProvider(this)
    }
}

/**
 * Context for handling authentication challenges specific to the admin authentication provider.
 *
 * This provides access to the [ApplicationCall] during challenge execution.
 *
 * @property call The current [ApplicationCall].
 */
class KtorAdminAuthChallengeContext(val call: ApplicationCall)

/**
 * A type alias for the admin authentication challenge function.
 *
 * The function takes a [KtorAdminAuthChallengeContext] and optional credentials as parameters.
 */
typealias KtorAdminAuthChallengeFunction = suspend KtorAdminAuthChallengeContext.(UserForm?) -> Unit

/**
 * Registers the custom admin authentication provider in the Ktor [AuthenticationConfig].
 *
 * This extension function allows you to easily add and configure the `ktorAdmin` authentication provider.
 *
 * @param name The name of the authentication provider.
 * @param configure A configuration block for customizing the admin authentication behavior.
 */
fun AuthenticationConfig.ktorAdmin(
    name: String,
    configure: KtorAdminAuthenticationProvider.KtorAdminAuthenticationConfig.() -> Unit
) {
    val provider = KtorAdminAuthenticationProvider.KtorAdminAuthenticationConfig(name).apply(configure).build()
    register(provider)
}