package ir.amirroid.ktoradmin.audit

object AuditFormatters {
    val event: AuditFormatter<AuditEvent> = AuditFormatter { it }

    val map: AuditFormatter<Map<String, Any?>> =
        AuditFormatter { event ->
            mapOf(
                "id" to event.id,
                "occurredAt" to event.occurredAt.toString(),
                "action" to event.action.name,
                "resourceType" to event.resourceType.name,
                "resourceName" to event.resourceName,
                "objectPrimaryKeys" to event.objectPrimaryKeys,
                "changes" to
                    event.changes.map { change ->
                        mapOf(
                            "name" to change.name,
                            "verboseName" to change.verboseName,
                            "changed" to change.changed,
                            "value" to change.value,
                        )
                    },
                "attributes" to event.attributes,
            )
        }
}
