package authentication

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import models.forms.UserForm
import models.forms.toUserForm
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str
import utils.baseUrl
import java.net.URI
import java.net.URL

/**
 * Key used to identify the admin authentication challenge.
 */
private const val adminAuthenticationChallengeKey = "AdminAuthenticationChallenge"

/**
 * Custom authentication provider for admin-specific authentication in Ktor.
 *
 * @property authenticateFunction A function responsible for validating the authentication credentials.
 * @property challengeFunction A function invoked when authentication fails to handle the challenge process.
 */
class KtorAdminAuthenticationProvider internal constructor(
    config: KtorAdminAuthenticationConfig
) : AuthenticationProvider(config) {

    private val authenticateFunction: AuthenticationFunction<UserForm> = config.authenticationFunction
    private val challengeFunction: KtorAdminAuthChallengeFunction = config.challengeFunction

    private fun ApplicationCall.getUserFromSessions() = sessions.get<UserForm>()

    /**
     * Called during the authentication process to validate credentials and set the principal.
     *
     * @param context The [AuthenticationContext] containing the current call and challenge state.
     */
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val sessionUser = call.getUserFromSessions()
        val inLoginUrl =
            call.request.uri.substringBefore("?") == "/admin/login" && call.request.httpMethod == HttpMethod.Post
        val formParameters = call.takeIf { inLoginUrl }?.receiveParameters()?.toUserForm()
        val parameters = sessionUser ?: formParameters

        // Invoke the authentication function if parameters are present
        val principal = parameters?.let { authenticateFunction.invoke(call, it) }

        if (principal != null) {
            // If authentication is successful, set the principal and return
            context.principal(name, principal)
            if (inLoginUrl && formParameters != sessionUser) {
                formParameters?.let { call.sessions.set(it) }
            }
            return
        }

        // Determine the cause of authentication failure
        val cause = when {
            parameters.isNullOrEmpty() -> AuthenticationFailedCause.NoCredentials
            else -> AuthenticationFailedCause.InvalidCredentials
        }
        // Trigger the challenge function to handle failed authentication
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
     * @property authenticationFunction A validation function to authenticate the credentials.
     * @property challengeFunction A function that handles challenges when authentication fails.
     */
    class KtorAdminAuthenticationConfig internal constructor(name: String? = null) : Config(name) {

        internal var authenticationFunction: AuthenticationFunction<UserForm> = { null }
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
         */
        private fun challenge(function: KtorAdminAuthChallengeFunction) {
            challengeFunction = function
        }

        /**
         * Specifies a redirect URL in a case of failed authentication.
         */
        private fun challenge(redirectUrl: String) {
            challenge {
                call.respondRedirect(redirectUrl)
            }
        }

        /**
         * Sets the validation function responsible for checking authentication credentials.
         *
         * @param body The validation logic that takes a map of parameters and returns a principal on success.
         */
        fun validate(body: suspend ApplicationCall.(UserForm) -> Any?) {
            authenticationFunction = body
        }

        /**
         * Builds the admin authentication provider using the current configuration.
         */
        internal fun build() = KtorAdminAuthenticationProvider(this)
    }
}

/**
 * Context for handling authentication challenges specific to the admin authentication provider.
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