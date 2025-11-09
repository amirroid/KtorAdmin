package ir.amirroid.ktoradmin.hikra

import com.zaxxer.hikari.HikariDataSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Manages HikariCP connection pools with thread-safe operations.
 *
 * Provides methods to:
 * - Create and manage database connection pools
 * - Retrieve existing connection pools
 * - Close connection pools
 */
object KtorAdminHikariCP {
    /**
     * Thread-safe concurrent map to store HikariDataSource instances.
     * Key: Pool name
     * Value: HikariDataSource instance
     */
    private val connectionPools: ConcurrentMap<String, HikariDataSource> = ConcurrentHashMap()

    /**
     * Adds a custom HikariDataSource to the pool with a specified name.
     *
     * @param poolName Unique name for the connection pool
     * @param dataSource Pre-configured HikariDataSource
     */
    fun custom(poolName: String, dataSource: HikariDataSource) {
        // Close existing connection pool if it's closed
        connectionPools[poolName]?.takeIf { it.isClosed }?.close()
        connectionPools[poolName] = dataSource
    }

    /**
     * Adds a custom HikariDataSource to the default pool.
     *
     * @param dataSource Pre-configured HikariDataSource
     */
    fun defaultCustom(dataSource: HikariDataSource) = custom("default", dataSource)

    /**
     * Retrieves an existing HikariDataSource from the connection pools.
     *
     * @param poolName Name of the connection pool (defaults to "default")
     * @return Active HikariDataSource
     * @throws IllegalStateException If no active connection pool is found
     */
    fun dataSource(poolName: String = "default"): HikariDataSource {
        val dataSource = connectionPools[poolName]
        return dataSource?.takeUnless { it.isClosed }
            ?: throw IllegalStateException("DataSource ($poolName) is absent or closed.")
    }

    /**
     * Closes all active connection pools.
     */
    fun closeAllConnections() {
        connectionPools.values
            .filterNot { it.isClosed }
            .forEach { it.close() }
    }
}