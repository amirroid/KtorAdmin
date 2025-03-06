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
import io.ktor.server.sessions.*
import models.forms.UserForm
import models.forms.toUserForm
import utils.invalidateRequest

/**
 * Key used to identify the authentication challenge for admin token authentication.
 * This is used internally to handle authentication failures for admin-related routes.
 */
private const val adminTokenAuthenticationChallengeKey = "AdminTokenAuthenticationChallenge"

/**
 * Custom authentication provider for admin token-based authentication in KtorAdmin.
 *
 * This provider handles authentication for admin routes, validates user tokens,
 * manages session-based authentication, and provides a configurable challenge function for failed authentication.
 *
 * @property formAuthenticateFunction Function responsible for validating form-based authentication credentials.
 * @property tokenAuthenticationFunction Function responsible for validating token-based authentication.
 * @property challengeFunction Function invoked when authentication fails to handle the challenge process.
 */
class KtorAdminTokenAuthProvider internal constructor(
    config: KtorAdminTokenAuthConfig
) : AuthenticationProvider(config) {

    // Function to validate form-based authentication credentials
    private val formAuthenticateFunction: AuthenticationFunction<UserForm> = config.formAuthenticationFunction

    // Function to validate token-based authentication
    private val tokenAuthenticationFunction: AuthenticationFunction<String> = config.tokenAuthenticationFunction

    // Function to handle authentication failure challenges
    private val challengeFunction: KtorAdminTokenAuthChallengeFunction = config.challengeFunction


    // Instance of CryptoManager for handling session encryption and decryption
    private val cryptoManager = CryptoManager()

    /**
     * Retrieves the authenticated token from session storage.
     *
     * @return The token stored in the session, or null if no token is present.
     */
    private fun ApplicationCall.getTokenFromSessions(): String? {
        return (sessions.get(USER_SESSIONS) as? String)?.let {
            runCatching { cryptoManager.decryptData(it) }.getOrNull()
        }
    }

    /**
     * Called during the authentication process to validate user tokens and set the principal.
     *
     * If a valid token is found in the session, it is used for authentication. Otherwise,
     * the function attempts to authenticate using request parameters.
     *
     * @param context The [AuthenticationContext] containing the current call and challenge state.
     */
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val token = call.getTokenFromSessions()

        if (token != null) {
            val principal = tokenAuthenticationFunction.invoke(call, token)
            if (principal != null) {
                context.principal(name, principal)
                return
            } else {
                call.sessions.clear(USER_SESSIONS)
                loginWithParameters(call, context)
            }
        } else {
            loginWithParameters(call, context)
        }
    }

    /**
     * Attempts to authenticate using form parameters.
     *
     * If authentication is successful, the token is stored in the session.
     * Otherwise, an authentication challenge is triggered.
     *
     * @param call The [ApplicationCall] containing the request data.
     * @param context The [AuthenticationContext] used to manage authentication state.
     */
    private suspend fun loginWithParameters(call: ApplicationCall, context: AuthenticationContext) {
        val inLoginUrl =
            call.request.uri.substringBefore("?") == "/${DynamicConfiguration.adminPath}/login" && call.request.httpMethod == HttpMethod.Post
        var parameters: UserForm? = null
        if (inLoginUrl) {
            parameters = call.receiveParameters().apply {
                val csrfToken = get(CSRF_TOKEN_FIELD_NAME)
                if (CsrfManager.validateToken(csrfToken).not()) {
                    return call.invalidateRequest()
                }
            }.toUserForm()
            val token = parameters.let { formAuthenticateFunction.invoke(call, it) }

            if (token is String) {
                val principal = tokenAuthenticationFunction.invoke(call, token)
                if (principal != null) {
                    call.sessions.set(USER_SESSIONS, cryptoManager.encryptData(token))
                    context.principal(name, principal)
                    return
                }
            }
        }

        val cause = AuthenticationFailedCause.InvalidCredentials
        context.challenge(adminTokenAuthenticationChallengeKey, cause) { challenge, challengeCall ->
            challengeFunction.invoke(KtorAdminAuthChallengeContext(challengeCall), parameters)
            if (!challenge.completed && challengeCall.response.status() != null) {
                challenge.complete()
            }
        }
    }

    /**
     * Configuration class for the custom admin token authentication provider.
     *
     * This class allows customization of the authentication and challenge logic for admin routes.
     *
     * @property formAuthenticationFunction Function to validate the credentials.
     * @property tokenAuthenticationFunction Function to validate token-based authentication.
     * @property challengeFunction Function that handles challenges when authentication fails.
     */
    class KtorAdminTokenAuthConfig internal constructor(name: String? = null) : Config(name) {
        internal var formAuthenticationFunction: AuthenticationFunction<UserForm> = { null }
        internal var tokenAuthenticationFunction: AuthenticationFunction<String> = { null }

        // Default challenge function to redirect to the admin login page
        internal var challengeFunction: KtorAdminTokenAuthChallengeFunction = {
            redirectToLogin(call, it?.get(REQUEST_ID_FORM))
        }

        fun validateToken(body: suspend ApplicationCall.(token: String) -> Any?) {
            tokenAuthenticationFunction = body
        }

        fun validateForm(body: suspend ApplicationCall.(credential: UserForm) -> Any?) {
            formAuthenticationFunction = body
        }

        fun challenge(function: KtorAdminTokenAuthChallengeFunction) {
            challengeFunction = function
        }

        internal fun build() = KtorAdminTokenAuthProvider(this)
    }
}

typealias KtorAdminTokenAuthChallengeFunction = suspend KtorAdminAuthChallengeContext.(UserForm?) -> Unit

fun AuthenticationConfig.ktorAdminTokenAuth(
    name: String,
    configure: KtorAdminTokenAuthProvider.KtorAdminTokenAuthConfig.() -> Unit
) {
    val provider = KtorAdminTokenAuthProvider.KtorAdminTokenAuthConfig(name).apply(configure).build()
    register(provider)
}