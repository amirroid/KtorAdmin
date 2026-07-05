package ir.amirroid.ktoradmin.audit

import java.time.Instant
import java.util.UUID

enum class AuditAction {
    INSERT,
    UPDATE,
    DELETE,
}

enum class AuditResourceType {
    JDBC_TABLE,
    MONGO_COLLECTION,
}

data class AuditFieldChange(
    val name: String,
    val verboseName: String = name,
    val changed: Boolean,
    val value: Any?,
)

data class AuditEvent(
    val id: String = UUID.randomUUID().toString(),
    val occurredAt: Instant = Instant.now(),
    val action: AuditAction,
    val resourceType: AuditResourceType,
    val resourceName: String,
    val objectPrimaryKeys: List<String>,
    val changes: List<AuditFieldChange> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
) {
    val objectPrimaryKey: String?
        get() = objectPrimaryKeys.firstOrNull()
}

data class AuditDispatchFailure(
    val handlerKey: String,
    val cause: Throwable,
)

data class AuditDispatchResult(
    val event: AuditEvent,
    val failures: List<AuditDispatchFailure>,
) {
    val isSuccess: Boolean
        get() = failures.isEmpty()
}
