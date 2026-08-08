package ir.amirroid.ktoradmin.authentication

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.authentication
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import ir.amirroid.ktoradmin.TestJdbcTable
import ir.amirroid.ktoradmin.column
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.csrf.CsrfManager
import ir.amirroid.ktoradmin.models.actions.Action
import ir.amirroid.ktoradmin.modules.autocomplete.configureAutoCompleteRouting
import ir.amirroid.ktoradmin.modules.configureGetRouting
import ir.amirroid.ktoradmin.modules.configureSavesRouting
import ir.amirroid.ktoradmin.modules.configureTemplating
import ir.amirroid.ktoradmin.modules.download.configureDownloadFilesRouting
import ir.amirroid.ktoradmin.modules.file.handleGenerateFileUrl
import ir.amirroid.ktoradmin.pages.CustomPage
import ir.amirroid.ktoradmin.ratelimit.configureRateLimit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PanelAuthorizationTest {
    private val protectedPanel =
        TestJdbcTable(
            columns = listOf(column("id"), column("name")),
            tableName = "test_table",
            pluralName = "Tests",
            accessRoles = listOf("admin"),
            defaultActions = listOf(Action.ADD, Action.EDIT, Action.DELETE),
        )

    private val openPanel =
        TestJdbcTable(
            columns = listOf(column("id"), column("name")),
            tableName = "open_table",
            pluralName = "Opens",
            accessRoles = null,
            defaultActions = listOf(Action.ADD, Action.EDIT, Action.DELETE),
        )

    @BeforeTest
    fun setup() {
        DynamicConfiguration.adminPath = "admin"
        DynamicConfiguration.canDownloadDataAsCsv = true
        DynamicConfiguration.canDownloadDataAsPdf = true
    }

    private fun Application.setupTestAuthAndRouting() {
        configureRateLimit()
        configureTemplating()
        authentication {
            register(TestAuthProvider(TestAuthConfig("test-auth")))
        }
        routing {
            configureGetRouting(listOf(protectedPanel, openPanel), "test-auth")
            configureSavesRouting(listOf(protectedPanel, openPanel), "test-auth")
            configureDownloadFilesRouting("test-auth", listOf(protectedPanel, openPanel))
            handleGenerateFileUrl(listOf(protectedPanel, openPanel), "test-auth")
            configureAutoCompleteRouting(listOf(protectedPanel, openPanel), "test-auth")
        }
    }

    @Test
    fun `should forbid unauthenticated user from accessing protected panel list`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.get("/admin/resources/Tests")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid authenticated user without required role from accessing protected panel list`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.get("/admin/resources/Tests") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should allow unauthenticated user to pass authorization for open panel list`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.get("/admin/resources/Opens")
            assertNotEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should allow authenticated user with required role to pass authorization for protected panel list`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.get("/admin/resources/Tests") {
                    header("Authorization", "Bearer admin-token")
                }
            assertNotEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from accessing Add view for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.get("/admin/resources/Tests/add")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from accessing Add view for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.get("/admin/resources/Tests/add") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should allow user with required role to pass authorization for Add view`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.get("/admin/resources/Tests/add") {
                    header("Authorization", "Bearer admin-token")
                }
            assertNotEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from submitting Add request for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.post("/admin/resources/Tests/add")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from submitting Add request for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.post("/admin/resources/Tests/add") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from accessing Edit view for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.get("/admin/resources/Tests/1")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from accessing Edit view for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.get("/admin/resources/Tests/1") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from submitting Update request for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.post("/admin/resources/Tests/1")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from submitting Update request for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.post("/admin/resources/Tests/1") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from executing actions on protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.post("/admin/actions/Tests/delete")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from executing actions on protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.post("/admin/actions/Tests/delete") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from accessing confirmation page for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.get("/admin/resources/Tests/1/delete")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from accessing confirmation page for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.get("/admin/resources/Tests/1/delete") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from posting confirmation action for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.post("/admin/resources/Tests/1/delete")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from downloading CSV for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val token = CsrfManager.generateToken()
            val response = client.get("/admin/downloads/Tests/csv?_csrf=$token")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from downloading CSV for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val token = CsrfManager.generateToken()
            val response =
                client.get("/admin/downloads/Tests/csv?_csrf=$token") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from downloading PDF for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val token = CsrfManager.generateToken()
            val response = client.get("/admin/downloads/Tests/1/pdf?_csrf=$token")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from downloading PDF for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val token = CsrfManager.generateToken()
            val response =
                client.get("/admin/downloads/Tests/1/pdf?_csrf=$token") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from searching autocomplete on protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response = client.post("/admin/autocomplete/Tests/name")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from searching autocomplete on protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.post("/admin/autocomplete/Tests/name") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated user from generating file URL for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.post("/admin/file_handler/generate/") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody("fileName=test.png&field=Tests.name")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid user without required role from generating file URL for protected panel`() =
        testApplication {
            application { setupTestAuthAndRouting() }
            val response =
                client.post("/admin/file_handler/generate/") {
                    header("Authorization", "Bearer user-token")
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody("fileName=test.png&field=Tests.name")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid unauthenticated access when page permissions are defined`() =
        testApplication {
            val page =
                CustomPage(
                    path = "secret-dashboard",
                    title = "Secret Dashboard",
                    permissions = listOf("admin"),
                    renderContent = { "Secret Content" },
                )
            DynamicConfiguration.registerCustomPage(page)

            application { setupTestAuthAndRouting() }

            val response = client.get("/admin/resources/secret-dashboard")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should allow authenticated user with required role to access protected custom page`() =
        testApplication {
            val page =
                CustomPage(
                    path = "admin-only",
                    title = "Admin Only",
                    permissions = listOf("admin"),
                    renderContent = { "Admin Content" },
                )
            DynamicConfiguration.registerCustomPage(page)

            application { setupTestAuthAndRouting() }

            val response =
                client.get("/admin/resources/admin-only") {
                    header("Authorization", "Bearer admin-token")
                }
            assertNotEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `should forbid authenticated user without required role from accessing custom page`() =
        testApplication {
            val page =
                CustomPage(
                    path = "admin-only-2",
                    title = "Admin Only 2",
                    permissions = listOf("admin"),
                    renderContent = { "Admin Content" },
                )
            DynamicConfiguration.registerCustomPage(page)

            application { setupTestAuthAndRouting() }

            val response =
                client.get("/admin/resources/admin-only-2") {
                    header("Authorization", "Bearer user-token")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    private class TestAuthConfig(
        name: String? = null,
    ) : AuthenticationProvider.Config(name)

    private class TestAuthProvider(
        config: TestAuthConfig,
    ) : AuthenticationProvider(config) {
        override suspend fun onAuthenticate(context: AuthenticationContext) {
            when (context.call.request.headers["Authorization"]) {
                "Bearer admin-token" ->
                    context.principal(KtorAdminPrincipal("admin_user", listOf("admin")))
                "Bearer user-token" ->
                    context.principal(KtorAdminPrincipal("normal_user", listOf("user")))
            }
        }
    }
}
