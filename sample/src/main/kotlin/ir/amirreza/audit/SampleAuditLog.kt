package ir.amirreza.audit

import ir.amirroid.ktoradmin.audit.AuditDestination
import ir.amirroid.ktoradmin.audit.AuditErrorHandler
import ir.amirroid.ktoradmin.audit.AuditEvent
import ir.amirroid.ktoradmin.audit.AuditExecutionHandler
import ir.amirroid.ktoradmin.audit.AuditFormatter
import ir.amirroid.ktoradmin.audit.AuditFormatters
import ir.amirroid.ktoradmin.audit.AuditLogger
import ir.amirroid.ktoradmin.configuration.KtorAdminConfiguration

data class SampleAuditRecord(
    val eventId: String,
    val occurredAt: String,
    val action: String,
    val resourceType: String,
    val resourceName: String,
    val primaryKeys: List<String>,
    val changedValues: Map<String, Any?>,
    val metadata: Map<String, Any?>,
)

object SampleAuditStore {
    private val records = mutableListOf<SampleAuditRecord>()

    val all: List<SampleAuditRecord>
        get() = records.toList()

    fun append(record: SampleAuditRecord) {
        records.add(record)
    }
}

fun KtorAdminConfiguration.registerSampleAuditLog(): AuditLogger =
    audit {
        errorHandler =
            AuditErrorHandler { result ->
                result.failures.forEach { failure ->
                    println("Audit handler '${failure.handlerKey}' failed: ${failure.cause.message}")
                }
            }


        registerDestination(
            key = "sample-memory-store",
            formatter = sampleAuditRecordFormatter,
            destination = AuditDestination { record, _ ->
                SampleAuditStore.append(record)
                println("Stored audit record: $record")
            },
        )

        registerDestination(
            key = "sample-structured-output",
            formatter = AuditFormatters.map,
            destination = AuditDestination { payload, _ ->
                println("Published audit payload: $payload")
            },
        )

        registerHandler(SampleSecurityAuditHandler)
    }

private val sampleAuditRecordFormatter =
    AuditFormatter { event ->
        SampleAuditRecord(
            eventId = event.id,
            occurredAt = event.occurredAt.toString(),
            action = event.action.name,
            resourceType = event.resourceType.name,
            resourceName = event.resourceName,
            primaryKeys = event.objectPrimaryKeys,
            changedValues =
                event.changes
                    .filter { it.changed }
                    .associate { it.name to it.value },
            metadata =
                mapOf(
                    "source" to "sample-admin-panel",
                    "singlePrimaryKey" to event.objectPrimaryKey,
                ),
        )
    }

private object SampleSecurityAuditHandler : AuditExecutionHandler {
    override val key: String = "sample-security-handler"

    override suspend fun handle(event: AuditEvent) {
        if (event.action.name == "DELETE") {
            println(
                "Security audit: ${event.objectPrimaryKeys.size} object(s) deleted " +
                    "from ${event.resourceName}",
            )
        }
    }
}
