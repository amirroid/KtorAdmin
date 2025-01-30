package csrf

import com.vladsch.kotlin.jdbc.param
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import utils.forbidden
import utils.serverError

const val CSRF_TOKEN_HEADER_NAME = "X-CSRF-Token"


internal class CsrfPlugin {
    internal class Configuration {
        internal var headerName = CSRF_TOKEN_HEADER_NAME
    }

    companion object : Plugin<ApplicationCallPipeline, Configuration, CsrfPlugin> {
        override val key: AttributeKey<CsrfPlugin>
            get() = AttributeKey("KtorAdminCsrfProtected")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CsrfPlugin {
            val config = Configuration().apply(configure)
            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                kotlin.runCatching {
                    if (call.request.httpMethod in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete)) {
                        val csrfToken = call.request.header(config.headerName) ?: CsrfManager.generateToken()
                        if (!CsrfManager.validateToken(csrfToken)) {
                            call.forbidden("Invalid CSRF Token")
                            finish()
                        }
                    }
                }.onFailure { call.serverError(it.message.orEmpty(), it) }
            }
            return CsrfPlugin()
        }
    }
}