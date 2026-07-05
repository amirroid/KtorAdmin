---
description: Extensible audit logging in KtorAdmin
---

# Audit Log

KtorAdmin includes a built-in audit log architecture that converts admin data events into normalized `AuditEvent` objects and dispatches them to any number of destinations. The logger separates log generation from log consumption:

* `AuditEventListener` listens to KtorAdmin insert, update, and delete callbacks.
* `AuditLogger` dispatches each normalized event to registered handlers concurrently.
* `AuditFormatter<T>` converts an `AuditEvent` into your target schema.
* `AuditDestination<T>` writes the formatted payload to a database, queue, file, external API, or any other sink.
* `AuditExecutionHandler` lets you register fully custom runtime handlers when a formatter/destination pair is not enough.

## Registering an audit logger

Use `audit {}` inside the `KtorAdmin` installation block:

```kotlin
install(KtorAdmin) {
    audit {
        registerDestination(
            key = "stdout",
            formatter = AuditFormatters.map,
            destination = AuditDestination { payload, _ ->
                println(payload)
            },
        )
    }
}
```

The audit listener composes with regular `AdminEventListener` registrations, so existing event listeners can keep handling thumbnails, custom workflows, or integrations while the audit logger records the same events.

## Event model

Every audit entry is represented as an `AuditEvent`:

```kotlin
data class AuditEvent(
    val id: String,
    val occurredAt: Instant,
    val action: AuditAction,
    val resourceType: AuditResourceType,
    val resourceName: String,
    val objectPrimaryKeys: List<String>,
    val changes: List<AuditFieldChange>,
    val attributes: Map<String, Any?>
)
```

`action` is one of `INSERT`, `UPDATE`, or `DELETE`.

`resourceType` is one of `JDBC_TABLE` or `MONGO_COLLECTION`.

`changes` contains field or column values for inserts and updates. Delete events contain the deleted primary keys and an empty `changes` list.

## Custom schemas

Create a formatter when your destination expects a specific schema:

```kotlin
data class AuditRow(
    val eventId: String,
    val tableName: String,
    val action: String,
    val primaryKey: String?,
    val changedFields: List<String>,
)

val rowFormatter =
    AuditFormatter { event ->
        AuditRow(
            eventId = event.id,
            tableName = event.resourceName,
            action = event.action.name,
            primaryKey = event.objectPrimaryKey,
            changedFields = event.changes.filter { it.changed }.map { it.name },
        )
    }

install(KtorAdmin) {
    audit {
        registerDestination(
            key = "audit-table",
            formatter = rowFormatter,
            destination = AuditDestination { row, _ ->
                auditRepository.insert(row)
            },
        )
    }
}
```

## Multiple destinations

Handlers are executed concurrently for each event. A slow destination does not block other destinations from receiving the same event:

```kotlin
install(KtorAdmin) {
    audit {
        registerDestination("database", rowFormatter) { row, _ ->
            auditRepository.insert(row)
        }

        registerDestination("queue", AuditFormatters.map) { payload, _ ->
            auditQueue.publish(payload)
        }
    }
}
```

## Runtime handlers

Use `AuditLogger` directly when you need to add or remove handlers after application startup:

```kotlin
val auditLogger = AuditLogger()

install(KtorAdmin) {
    registerAuditLogger(auditLogger)
}

auditLogger.registerHandler(
    object : AuditExecutionHandler {
        override val key: String = "security-monitor"

        override suspend fun handle(event: AuditEvent) {
            securityMonitor.capture(event)
        }
    },
)

auditLogger.unregisterHandler("security-monitor")
```

## Failure handling

By default, handler failures are collected in `AuditDispatchResult` and do not stop the admin operation or other handlers. Register an `AuditErrorHandler` to observe failures:

```kotlin
install(KtorAdmin) {
    audit {
        errorHandler =
            AuditErrorHandler { result ->
                result.failures.forEach { failure ->
                    application.log.error("Audit handler failed: ${failure.handlerKey}", failure.cause)
                }
            }
    }
}
```

Set `failOnHandlerError = true` if audit delivery must fail the current admin event when any destination fails:

```kotlin
install(KtorAdmin) {
    audit {
        failOnHandlerError = true
    }
}
```

## Event enrichment

For request-specific metadata such as actor ID, IP address, or tenant ID, create an `AuditEventListener` manually and enrich events before they are dispatched:

```kotlin
val logger = AuditLogger()

install(KtorAdmin) {
    registerEventListener(
        AuditEventListener(logger) { event ->
            event.copy(
                attributes =
                    event.attributes +
                        mapOf(
                            "tenant" to "default",
                            "source" to "admin-panel",
                        ),
            )
        },
    )
}
```

Use `attributes` for metadata that is part of your audit schema but is not produced by the database mutation itself.
