package ir.amirroid.ktoradmin.audit

fun interface AuditFormatter<out T> {
    suspend fun format(event: AuditEvent): T
}

fun interface AuditDestination<in T> {
    suspend fun write(
        payload: T,
        event: AuditEvent,
    )
}

interface AuditExecutionHandler {
    val key: String

    suspend fun handle(event: AuditEvent)
}

class FormattingAuditExecutionHandler<T>(
    override val key: String,
    private val formatter: AuditFormatter<T>,
    private val destination: AuditDestination<T>,
) : AuditExecutionHandler {
    override suspend fun handle(event: AuditEvent) {
        destination.write(formatter.format(event), event)
    }
}

fun interface AuditErrorHandler {
    fun onFailure(result: AuditDispatchResult)
}
