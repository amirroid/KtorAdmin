package error

import configuration.DynamicConfiguration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.uri
import io.ktor.util.*

/**
 * Configuration class for the KtorAdminErrorHandler plugin.
 * This allows registering custom handlers for different HTTP status codes.
 */
internal class KtorAdminErrorHandlerConfig {
    /**
     * A map storing custom handlers for specific HTTP status codes.
     */
    val statusHandlers = mutableMapOf<HttpStatusCode, suspend (ApplicationCall) -> Unit>()

    /**
     * Registers a handler for a given HTTP status code.
     * @param status The HTTP status code to handle.
     * @param handler The function to execute when the specified status occurs.
     */
    fun handle(status: HttpStatusCode, handler: suspend (ApplicationCall) -> Unit) {
        statusHandlers[status] = handler
    }
}

/**
 * A Ktor plugin that allows handling specific HTTP status codes dynamically.
 * Developers can register custom responses for different status codes.
 */
internal val KtorAdminErrorHandler =
    createApplicationPlugin("KtorAdminErrorHandler", ::KtorAdminErrorHandlerConfig) {
        /**
         * An attribute key to prevent multiple executions for the same request.
         */
        val handledAttributeKey = AttributeKey<Boolean>("KtorAdminHandledStatus")

        /**
         * Intercepts the response before it is sent to apply custom status handlers if defined.
         */
        on(ResponseBodyReadyForSend) { call, content ->
            if (call.attributes.getOrNull(handledAttributeKey) == true || !call.request.uri.startsWith(
                    "/${DynamicConfiguration.adminPath}"
                )
            ) return@on
            val status = content.status ?: call.response.status() ?: return@on
            pluginConfig.statusHandlers[status]?.let { handler ->
                call.attributes.put(handledAttributeKey, true)
                handler(call)
            }
        }
    }