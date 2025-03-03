package repository

import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Projections.*
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import configuration.DynamicConfiguration
import formatters.map
import getters.toTypedValue
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import models.DataWithPrimaryKey
import models.field.FieldSet
import models.field.getCurrentDate
import models.order.Order
import models.types.FieldType
import mongo.MongoCredential
import mongo.MongoServerAddress
import org.bson.BsonValue
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import panels.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.jvm.Synchronized

/**
 * Thread-safe repository for managing MongoDB client connections and operations.
 * Uses ConcurrentHashMap to safely handle multiple threads accessing the database connections.
 * Maintains references to existing client connections to prevent duplicates.
 */
internal object MongoClientRepository {
    // Thread-safe map for database connections
    private val databases: ConcurrentHashMap<String, MongoDatabase> = ConcurrentHashMap()

    // Keep track of all created clients for proper cleanup
    private val mongoClients: ConcurrentHashMap<String, MongoClient> = ConcurrentHashMap()

    // Store connection configurations to enable client reuse
    private data class ConnectionConfig(
        val host: String,
        val port: Int,
        val databaseName: String,
        val username: String?,
        val passwordHash: Int?
    )

    // Map to store connection configurations for client reuse
    private val connectionConfigs: ConcurrentHashMap<String, ConnectionConfig> = ConcurrentHashMap()

    // Map for reverse lookup (config to key)
    private val configToKeyMap: ConcurrentHashMap<ConnectionConfig, String> = ConcurrentHashMap()

    // Lock for synchronizing client creation operations
    private val clientOperationsLock = ReentrantReadWriteLock()

    fun getActualKey(key: String?) = key ?: "default"

    /**
     * Registers a new MongoDB client connection with the specified parameters.
     * If a connection with the same configuration already exists, it will be reused.
     *
     * @param key Unique identifier for the database connection
     * @param databaseName Name of the database to connect to
     * @param address Server address information
     * @param credential Optional authentication credentials
     * @throws IllegalArgumentException if a client with the given key already exists
     */
    fun registerNewClient(
        key: String?,
        databaseName: String,
        address: MongoServerAddress,
        credential: MongoCredential? = null
    ) {
        val actualKey = getActualKey(key)

        // If a database already exists with this key, don't create a new one
        clientOperationsLock.read {
            if (databases.containsKey(actualKey)) {
                return
            }
        }

        // Create a configuration object to check if we already have a similar connection
        val config = ConnectionConfig(
            host = address.host,
            port = address.port,
            databaseName = databaseName,
            username = credential?.username,
            passwordHash = credential?.password?.hashCode()
        )

        clientOperationsLock.write {
            // Double-check if a database was created while waiting for the lock
            if (databases.containsKey(actualKey)) {
                return
            }

            // Try to find an existing client with the same configuration using the reverse map
            val existingKey = configToKeyMap[config]

            if (existingKey != null && mongoClients.containsKey(existingKey)) {
                // Reuse the existing client but connect to the requested database
                val existingClient = mongoClients[existingKey]
                if (existingClient != null) {
                    val database = existingClient.getDatabase(databaseName)
                    databases[actualKey] = database
                    connectionConfigs[actualKey] = config
                    return
                }
            }

            // Create a new client if no matching configuration was found
            val settings = MongoClientSettings.builder()
                .let {
                    if (credential != null) {
                        it.credential(
                            com.mongodb.MongoCredential.createCredential(
                                credential.username, credential.database, credential.password.toCharArray()
                            )
                        )
                    } else it
                }
                .applyToClusterSettings { cluster ->
                    cluster.hosts(listOf(ServerAddress(address.host, address.port)))
                }
                .build()

            val newClient = MongoClient.create(settings)
            val database = newClient.getDatabase(databaseName)

            // Store client and database with proper key
            val clientKey = key?.toString() ?: "default-${System.currentTimeMillis()}"
            mongoClients[clientKey] = newClient
            databases[actualKey] = database
            connectionConfigs[actualKey] = config
            configToKeyMap[config] = clientKey
        }
    }

    /**
     * Gets the appropriate MongoDB collection for the given admin collection.
     *
     * @return The MongoDB collection
     * @throws IllegalArgumentException if the database connection is not found
     */
    private fun AdminMongoCollection.getCollection(): MongoCollection<Document> {
        val databaseKey = getActualKey(getDatabaseKey())
        return databases[databaseKey]?.getCollection(collectionName = getCollectionName())
            ?: throw IllegalArgumentException("Client for database key $databaseKey not found.")
    }

    /**
     * Creates a filter for querying by primary key, handling ObjectId conversion if needed.
     *
     * @param primaryKey The primary key value
     * @return A MongoDB filter for the primary key
     */
    private fun AdminMongoCollection.getPrimaryKeyFilter(primaryKey: String) =
        if (getPrimaryKeyField().type == FieldType.ObjectId || getPrimaryKey() == "_id") {
            eq(getPrimaryKey(), ObjectId(primaryKey))
        } else eq(getPrimaryKey(), primaryKey)

    /**
     * Converts a BsonValue to a string ID if it's an ObjectId.
     */
    private fun BsonValue.toStringId() = asObjectId()?.value?.toHexString()

    /**
     * Formats a string parameter according to the field type.
     */
    private fun String.formatParameter(field: FieldSet) = toTypedValue(field.type)

    /**
     * Inserts a new document into the specified collection.
     *
     * @param values Map of field sets and their values
     * @param panel The admin collection to insert into
     * @return The ID of the inserted document, or null if insertion failed
     */
    suspend fun insertData(
        values: Map<FieldSet, Any?>,
        panel: AdminMongoCollection
    ): String? {
        val changeDates = panel.getAllAutoNowDateInsertFields().map {
            it to it.getCurrentDate()
        }

        val document = Document().apply {
            values.toList().plus(changeDates).distinctBy { it.first }.forEach { (field, value) ->
                put(field.fieldName, value?.toString()?.formatParameter(field))
            }
        }

        return panel.getCollection().insertOne(document).insertedId?.toStringId()
    }

    /**
     * Retrieves paginated data from the specified collection with filtering and optional sorting.
     *
     * @param table The admin collection to query
     * @param page Page number for pagination
     * @param filters MongoDB filters to apply
     * @param order Optional sorting order
     * @return List of data objects with their primary keys
     */
    suspend fun getAllData(
        table: AdminMongoCollection,
        page: Int,
        filters: Bson,
        order: Order? = null
    ): List<DataWithPrimaryKey> {
        val fields = table.getAllAllowToShowFields()
        val projection = table.getCollection()
            .find(filters)
            .skip(DynamicConfiguration.maxItemsInPage * page)
            .limit(DynamicConfiguration.maxItemsInPage)
            .let {
                when {
                    order == null -> it
                    order.direction.equals("asc", ignoreCase = true) -> it.sort(Sorts.ascending(order.name))
                    order.direction.equals("desc", ignoreCase = true) -> it.sort(Sorts.descending(order.name))
                    else -> it
                }
            }
            .projection(
                fields(
                    *fields.plus(table.getPrimaryKeyField())
                        .map { include(it.fieldName) }
                        .distinct()
                        .toTypedArray()
                )
            ).filterNotNull().toList()

        return projection.map { values ->
            DataWithPrimaryKey(
                primaryKey = values[table.getPrimaryKey()]!!.toString(),
                data = fields.map { field -> values[field.fieldName]?.toString() }
            )
        }
    }


    suspend fun getAllDataAsCsvFile(panel: AdminMongoCollection): String {
        val fields = panel.getAllAllowToShowFields()
        return panel.getCollection().find().projection(
            fields(
                *fields.map { include(it.fieldName) }.toTypedArray()
            )
        ).filterNotNull().toList().joinToString("\n") { values ->
            fields.joinToString(", ") { values[it.fieldName]?.toString() ?: "N/A" }
        }
    }

    /**
     * Counts the number of documents matching the specified filters.
     *
     * @param table The admin collection
     * @param filters MongoDB filters to apply
     * @return The count of matching documents
     */
    suspend fun getCount(
        table: AdminMongoCollection,
        filters: Bson,
    ): Long {
        return table.getCollection().countDocuments(filters)
    }

    /**
     * Retrieves a single document by its primary key.
     *
     * @param panel The admin collection
     * @param primaryKey Primary key value
     * @return List of field values, or null if not found
     */
    suspend fun getData(panel: AdminMongoCollection, primaryKey: String): List<String?>? {
        val projection = panel.getCollection().find(
            panel.getPrimaryKeyFilter(primaryKey)
        ).projection(
            fields(
                fields(
                    *panel.getAllAllowToShowFieldsInUpsert()
                        .map { include(it.fieldName) }
                        .toTypedArray()
                )
            )
        ).firstOrNull()

        return projection?.let { values ->
            panel.getAllAllowToShowFieldsInUpsert().map { field -> values[field.fieldName]?.toString() }
        }.also {
            println("Data: $it")
        }
    }

    /**
     * Updates or inserts data based on changes between initial and new values.
     *
     * @param panel The admin collection
     * @param parameters Map of field sets and their new values
     * @param primaryKey Primary key value
     * @param initialData Initial field values
     * @return Pair of inserted/updated ID and list of changed field names, or null if no changes
     */
    suspend fun updateChangedData(
        panel: AdminMongoCollection,
        parameters: Map<FieldSet, String?>,
        primaryKey: String,
        initialData: List<String?>?,
    ): Pair<String?, List<String>>? {
        // If initialData is null, this is an insert operation
        if (initialData == null) {
            return insertData(parameters, panel)?.let { id ->
                id to panel.getAllFields().map { it.fieldName.toString() }
            }
        } else {
            // This is an update operation
            val changeDates = panel.getAllAutoNowDateUpdateFields().map {
                it to it.getCurrentDate()
            }

            // Find fields that have changed values
            val updateFields = parameters.toList().filterIndexed { index, item ->
                val initialValue = initialData.getOrNull(index)
                val formattedValue = item.second?.formatParameter(item.first)
                initialValue != formattedValue && !(initialValue != null && item.second == null)
            }.plus(changeDates).distinctBy { it.first }

            // If no fields changed, return null
            if (updateFields.isEmpty()) return null

            val updatedFieldsBson = updateFields.mapNotNull {
                set(
                    it.first.fieldName.toString(),
                    it.second?.formatParameter(it.first) ?: return@mapNotNull null
                )
            }

            val id = panel.getCollection()
                .updateOne(panel.getPrimaryKeyFilter(primaryKey), combine(updatedFieldsBson))
                .upsertedId?.toStringId()

            return id to updateFields.map { it.first.fieldName.toString() }
        }
    }

    /**
     * Creates filters for querying by multiple primary keys, handling ObjectId conversion if needed.
     *
     * @param primaryKeys List of primary key values
     * @return A MongoDB filter for the primary keys
     */
    private fun AdminMongoCollection.getPrimaryKeyFilters(primaryKeys: List<String>) =
        if (getPrimaryKeyField().type == FieldType.ObjectId || getPrimaryKey() == "_id") {
            Filters.`in`(getPrimaryKey(), primaryKeys.map { ObjectId(it) })
        } else {
            Filters.`in`(getPrimaryKey(), primaryKeys)
        }

    /**
     * Deletes multiple documents by their primary keys.
     *
     * @param panel The admin collection
     * @param selectedIds List of primary key values to delete
     */
    suspend fun deleteRows(panel: AdminMongoCollection, selectedIds: List<String>) {
        panel.getCollection().deleteMany(
            panel.getPrimaryKeyFilters(selectedIds)
        )
    }

    /**
     * Safely gets a database by key, used for testing and other direct access
     *
     * @param key The database key
     * @return The MongoDB database or null if not found
     */
    fun getDatabaseByKey(key: String?): MongoDatabase? {
        return databases[getActualKey(key)]
    }

    /**
     * Closes all database connections and clears the connection map.
     * Should be called when shutting down the application.
     */
    @Synchronized
    fun closeAllConnections() {
        clientOperationsLock.write {
            // Close all MongoDB clients
            mongoClients.forEach { (_, client) ->
                try {
                    client.close()
                } catch (e: Exception) {
                    println("ERROR IN CLOSING MONGODB: ${e.message}")
                }
            }

            // Clear all maps
            mongoClients.clear()
            databases.clear()
            connectionConfigs.clear()
            configToKeyMap.clear()
        }
    }
}