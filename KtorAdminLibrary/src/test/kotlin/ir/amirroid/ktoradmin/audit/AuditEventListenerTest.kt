package ir.amirroid.ktoradmin.audit

import ir.amirroid.ktoradmin.column
import ir.amirroid.ktoradmin.models.events.ColumnEvent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditEventListenerTest {
    @Test
    fun `should convert jdbc insert callback to normalized audit event`() =
        runBlocking {
            val events = mutableListOf<AuditEvent>()
            val logger =
                AuditLogger()
                    .registerDestination(
                        key = "memory",
                        formatter = AuditFormatters.event,
                        destination = AuditDestination { payload, _ -> events.add(payload) },
                    )
            val listener =
                AuditEventListener(logger) {
                    it.copy(attributes = it.attributes + ("actorId" to "admin-1"))
                }

            listener.onInsertJdbcData(
                tableName = "users",
                objectPrimaryKey = "7",
                events =
                    listOf(
                        ColumnEvent(
                            changed = true,
                            columnSet = column("email"),
                            value = "ada@example.com",
                        ),
                    ),
            )

            assertEquals(1, events.size)
            val event = events.single()
            assertEquals(AuditAction.INSERT, event.action)
            assertEquals(AuditResourceType.JDBC_TABLE, event.resourceType)
            assertEquals("users", event.resourceName)
            assertEquals(listOf("7"), event.objectPrimaryKeys)
            assertEquals("email", event.changes.single().name)
            assertEquals("ada@example.com", event.changes.single().value)
            assertEquals("admin-1", event.attributes["actorId"])
        }

    @Test
    fun `should convert delete callbacks to audit event without changes`() =
        runBlocking {
            val events = mutableListOf<AuditEvent>()
            val logger =
                AuditLogger()
                    .registerDestination(
                        key = "memory",
                        formatter = AuditFormatters.event,
                        destination = AuditDestination { payload, _ -> events.add(payload) },
                    )
            val listener = AuditEventListener(logger)

            listener.onDeleteMongoObjects("posts", listOf("a", "b"))

            val event = events.single()
            assertEquals(AuditAction.DELETE, event.action)
            assertEquals(AuditResourceType.MONGO_COLLECTION, event.resourceType)
            assertEquals("posts", event.resourceName)
            assertEquals(listOf("a", "b"), event.objectPrimaryKeys)
            assertEquals(emptyList(), event.changes)
        }
}
