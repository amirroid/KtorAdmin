package ir.amirroid.ktoradmin.audit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

class AuditLogger(
    private val failOnHandlerError: Boolean = false,
    private val errorHandler: AuditErrorHandler? = null,
) {
    private val handlers = ConcurrentHashMap<String, AuditExecutionHandler>()

    val registeredHandlerKeys: Set<String>
        get() = handlers.keys.toSet()

    fun registerHandler(handler: AuditExecutionHandler): AuditLogger {
        require(handler.key.isNotBlank()) { "Audit handler key cannot be blank." }
        val previous = handlers.putIfAbsent(handler.key, handler)
        require(previous == null) { "An audit handler with key '${handler.key}' is already registered." }
        return this
    }

    fun <T> registerDestination(
        key: String,
        formatter: AuditFormatter<T>,
        destination: AuditDestination<T>,
    ): AuditLogger =
        registerHandler(
            FormattingAuditExecutionHandler(
                key = key,
                formatter = formatter,
                destination = destination,
            ),
        )

    fun unregisterHandler(key: String): Boolean = handlers.remove(key) != null

    suspend fun log(event: AuditEvent): AuditDispatchResult {
        val activeHandlers = handlers.values.toList()
        val failures =
            coroutineScope {
                activeHandlers
                    .map { handler ->
                        async {
                            val exception = runCatching { handler.handle(event) }.exceptionOrNull()
                            if (exception is CancellationException) throw exception
                            exception?.let { AuditDispatchFailure(handler.key, it) }
                        }
                    }.awaitAll()
                    .filterNotNull()
            }

        val result = AuditDispatchResult(event, failures)
        if (!result.isSuccess) {
            errorHandler?.onFailure(result)
            if (failOnHandlerError) {
                throw AuditLogException(result)
            }
        }
        return result
    }
}

class AuditLogException(
    val result: AuditDispatchResult,
) : RuntimeException(
        "Audit log dispatch failed for handlers: ${result.failures.joinToString { it.handlerKey }}",
        result.failures.firstOrNull()?.cause,
    )
