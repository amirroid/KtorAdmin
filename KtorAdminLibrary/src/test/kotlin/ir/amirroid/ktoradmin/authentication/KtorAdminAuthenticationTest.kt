package ir.amirroid.ktoradmin.authentication

import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.authentication
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.testing.testApplication
import ir.amirroid.ktoradmin.configuration.KtorAdminConfiguration
import ir.amirroid.ktoradmin.csrf.CSRF_TOKEN_FIELD_NAME
import ir.amirroid.ktoradmin.csrf.CsrfManager
import ir.amirroid.ktoradmin.models.forms.UserForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.days

class KtorAdminAuthenticationTest {
    @Test
    fun `should authenticate valid form credentials and reuse encrypted session`() = testApplication {
        application { configureFormAuthTestApp() }
        val client = createClient { install(HttpCookies) }

        val loginResponse = client.submitForm(
            url = "/admin/login",
            formParameters = Parameters.build {
                append(CSRF_TOKEN_FIELD_NAME, CsrfManager.generateToken())
                append("username", "admin")
                append("password", "secret")
            },
        )
        val protectedResponse = client.get("/protected")

        assertEquals(HttpStatusCode.OK, loginResponse.status)
        assertEquals("form:admin", loginResponse.bodyAsText())
        assertEquals(HttpStatusCode.OK, protectedResponse.status)
        assertEquals("protected", protectedResponse.bodyAsText())
    }

    @Test
    fun `should reject invalid form credentials with unauthorized challenge`() = testApplication {
        application { configureFormAuthTestApp() }

        val response = client.submitForm(
            url = "/admin/login",
            formParameters = Parameters.build {
                append(CSRF_TOKEN_FIELD_NAME, CsrfManager.generateToken())
                append("username", "admin")
                append("password", "wrong")
            },
        )

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("/admin", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `should reject form login when csrf token is missing malformed or tampered`() = testApplication {
        application { configureFormAuthTestApp() }

        val nonRedirectingClient = createClient { followRedirects = false }
        val missing = nonRedirectingClient.submitForm("/admin/login", Parameters.build { append("username", "admin") })
        val malformed = nonRedirectingClient.submitForm("/admin/login", Parameters.build { append(CSRF_TOKEN_FIELD_NAME, "bad-token") })
        val valid = CsrfManager.generateToken()
        val tampered = valid.split(":").let { listOf(it[0], it[1], it[2].reversed()).joinToString(":") }
        val tamperedResponse = nonRedirectingClient.submitForm("/admin/login", Parameters.build { append(CSRF_TOKEN_FIELD_NAME, tampered) })

        assertEquals(HttpStatusCode.BadRequest, missing.status)
        assertEquals(HttpStatusCode.BadRequest, malformed.status)
        assertEquals(HttpStatusCode.BadRequest, tamperedResponse.status)
    }

    @Test
    fun `should reject protected form route when session token is expired`() = testApplication {
        val configuration = KtorAdminConfiguration()
        val original = configuration.authenticationSessionMaxAge
        try {
            configuration.authenticationSessionMaxAge = 1.days
            application { configureFormAuthTestApp() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            client.submitForm(
                url = "/admin/login",
                formParameters = Parameters.build {
                    append(CSRF_TOKEN_FIELD_NAME, CsrfManager.generateToken())
                    append("username", "admin")
                    append("password", "secret")
                },
            )

            configuration.authenticationSessionMaxAge = (-1).milliseconds
            val response = client.get("/protected")

            assertEquals(HttpStatusCode.Found, response.status)
            assertTrue(response.headers[HttpHeaders.Location]!!.endsWith("/admin/login?origin=http://localhost/protected"))
        } finally {
            configuration.authenticationSessionMaxAge = original
        }
    }

    @Test
    fun `should authenticate token flow and reuse encrypted token session`() = testApplication {
        application { configureTokenAuthTestApp() }
        val client = createClient { install(HttpCookies) }

        val loginResponse = client.submitForm(
            url = "/admin/login",
            formParameters = Parameters.build {
                append(CSRF_TOKEN_FIELD_NAME, CsrfManager.generateToken())
                append("apiKey", "valid-key")
            },
        )
        val protectedResponse = client.get("/token-protected")

        assertEquals(HttpStatusCode.OK, loginResponse.status)
        assertEquals("token-login", loginResponse.bodyAsText())
        assertEquals(HttpStatusCode.OK, protectedResponse.status)
        assertEquals("token-protected", protectedResponse.bodyAsText())
    }

    @Test
    fun `should reject token flow for invalid credentials and missing session`() = testApplication {
        application { configureTokenAuthTestApp() }

        val missingSession = client.get("/token-protected")
        val invalidLogin = client.submitForm(
            url = "/admin/login",
            formParameters = Parameters.build {
                append(CSRF_TOKEN_FIELD_NAME, CsrfManager.generateToken())
                append("apiKey", "bad-key")
            },
        )

        assertEquals(HttpStatusCode.Unauthorized, missingSession.status)
        assertEquals("token-challenge:null", missingSession.bodyAsText())
        assertEquals(HttpStatusCode.Unauthorized, invalidLogin.status)
        assertEquals("token-challenge:bad-key", invalidLogin.bodyAsText())
    }

    @Test
    fun `should enforce role and dashboard permission checks in route authorization`() = testApplication {
        application {
            install(Sessions) { configureAdminCookies() }
            authentication {
                ktorAdminFormAuth("role-auth") {
                    validate { form ->
                        when (form["username"]) {
                            "editor" -> KtorAdminPrincipal("editor", roles = listOf("editor"), dashboardAccess = true)
                            "viewer" -> KtorAdminPrincipal("viewer", roles = listOf("viewer"), dashboardAccess = false)
                            else -> null
                        }
                    }
                }
            }
            routing {
                authenticate("role-auth") {
                    post("/admin/login") { call.respondText("logged-in") }
                    get("/editor-only") {
                        val principal = call.principal<KtorAdminPrincipal>()!!
                        if (principal.roles?.contains("editor") == true) call.respondText("allowed")
                        else call.respondText("forbidden", status = HttpStatusCode.Forbidden)
                    }
                    get("/dashboard") {
                        val principal = call.principal<KtorAdminPrincipal>()!!
                        if (principal.dashboardAccess) call.respondText("dashboard")
                        else call.respondText("no-dashboard", status = HttpStatusCode.Forbidden)
                    }
                }
            }
        }
        val editor = createClient { install(HttpCookies) }
        editor.submitForm("/admin/login", Parameters.build {
            append(CSRF_TOKEN_FIELD_NAME, CsrfManager.generateToken())
            append("username", "editor")
        })
        val viewer = createClient { install(HttpCookies) }
        viewer.submitForm("/admin/login", Parameters.build {
            append(CSRF_TOKEN_FIELD_NAME, CsrfManager.generateToken())
            append("username", "viewer")
        })

        assertEquals(HttpStatusCode.OK, editor.get("/editor-only").status)
        assertEquals(HttpStatusCode.OK, editor.get("/dashboard").status)
        assertEquals(HttpStatusCode.Forbidden, viewer.get("/editor-only").status)
        assertEquals(HttpStatusCode.Forbidden, viewer.get("/dashboard").status)
    }

    private fun io.ktor.server.application.Application.configureFormAuthTestApp() {
        install(Sessions) { configureAdminCookies() }
        authentication {
            ktorAdminFormAuth("form-auth") {
                validate { form ->
                    if (form["username"] == "admin" && form["password"] == "secret") {
                        KtorAdminPrincipal("admin", roles = listOf("admin"))
                    } else null
                }
            }
        }
        routing {
            authenticate("form-auth") {
                post("/admin/login") { call.respondText("form:${call.principal<KtorAdminPrincipal>()!!.name}") }
                get("/protected") { call.respondText("protected") }
            }
        }
    }

    private fun io.ktor.server.application.Application.configureTokenAuthTestApp() {
        install(Sessions) { configureAdminCookies() }
        authentication {
            ktorAdminTokenAuth("token-auth") {
                validateForm { form -> if (form["apiKey"] == "valid-key") "valid-token" else null }
                validateToken { token -> if (token == "valid-token") KtorAdminPrincipal("token-user") else null }
                challenge { form ->
                    call.respondText("token-challenge:${form?.get("apiKey")}", status = HttpStatusCode.Unauthorized)
                }
            }
        }
        routing {
            authenticate("token-auth") {
                post("/admin/login") { call.respondText("token-login") }
                get("/token-protected") { call.respondText("token-protected") }
            }
        }
    }
}
