package ir.amirroid.ktoradmin.audit

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuditLoggerTest {
    @Test
    fun `should format event and write to multiple destinations concurrently`() =
        runBlocking {
            val writes = CopyOnWriteArrayList<Map<String, Any?>>()
            val logger = AuditLogger()
            logger.registerDestination(
                key = "primary",
                formatter = AuditFormatters.map,
                destination =
                    AuditDestination { payload, _ ->
                        delay(100)
                        writes.add(payload)
                    },
            )
            logger.registerDestination(
                key = "replica",
                formatter = AuditFormatters.map,
                destination =
                    AuditDestination { payload, _ ->
                        delay(100)
                        writes.add(payload)
                    },
            )

            val elapsed =
                measureTimeMillis {
                    val result = logger.log(testEvent())
                    assertTrue(result.isSuccess)
                }

            assertEquals(2, writes.size)
            assertTrue(elapsed < 180, "Handlers should run concurrently, elapsed=$elapsed ms")
            assertEquals(setOf("primary", "replica"), logger.registeredHandlerKeys)
        }

    @Test
    fun `should collect handler failures without blocking successful handlers`() =
        runBlocking {
            var successfulHandlerCalled = false
            val failures = mutableListOf<AuditDispatchResult>()
            val logger =
                AuditLogger(
                    errorHandler = AuditErrorHandler { failures.add(it) },
                )
            logger.registerHandler(
                object : AuditExecutionHandler {
                    override val key: String = "failing"

                    override suspend fun handle(event: AuditEvent) {
                        error("Destination unavailable")
                    }
                },
            )
            logger.registerHandler(
                object : AuditExecutionHandler {
                    override val key: String = "successful"

                    override suspend fun handle(event: AuditEvent) {
                        successfulHandlerCalled = true
                    }
                },
            )

            val result = logger.log(testEvent())

            assertFalse(result.isSuccess)
            assertTrue(successfulHandlerCalled)
            assertEquals(listOf("failing"), result.failures.map { it.handlerKey })
            assertEquals(1, failures.size)
        }

    @Test
    fun `should throw when fail on handler error is enabled`() =
        runBlocking {
            val logger =
                AuditLogger(failOnHandlerError = true)
                    .registerHandler(
                        object : AuditExecutionHandler {
                            override val key: String = "failing"

                            override suspend fun handle(event: AuditEvent) {
                                error("Destination unavailable")
                            }
                        },
                    )

            val exception = assertFailsWith<AuditLogException> { logger.log(testEvent()) }
            assertEquals(listOf("failing"), exception.result.failures.map { it.handlerKey })
        }

    @Test
    fun `should support runtime handler removal`() =
        runBlocking {
            var calls = 0
            val logger =
                AuditLogger()
                    .registerHandler(
                        object : AuditExecutionHandler {
                            override val key: String = "runtime"

                            override suspend fun handle(event: AuditEvent) {
                                calls++
                            }
                        },
                    )

            assertTrue(logger.unregisterHandler("runtime"))
            assertFalse(logger.unregisterHandler("runtime"))
            logger.log(testEvent())

            assertEquals(0, calls)
        }

    private fun testEvent(): AuditEvent =
        AuditEvent(
            action = AuditAction.UPDATE,
            resourceType = AuditResourceType.JDBC_TABLE,
            resourceName = "users",
            objectPrimaryKeys = listOf("1"),
            changes = listOf(AuditFieldChange("name", changed = true, value = "Ada")),
        )
}
