package repository

import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Aggregates.group
import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Aggregates.sort
import com.mongodb.client.model.BsonField
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Projections.*
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import configuration.DynamicConfiguration
import dashboard.chart.ChartDashboardSection
import dashboard.simple.TextDashboardSection
import formatters.map
import getters.toTypedValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.toList
import models.DataWithPrimaryKey
import models.chart.ChartDashboardAggregationFunction
import models.chart.ChartData
import models.chart.ChartLabelsWithValues
import models.chart.TextDashboardAggregationFunction
import models.chart.TextData
import models.chart.getFieldFunctionBasedOnAggregationFunction
import models.chart.getFieldNameBasedOnAggregationFunction
import models.events.FileEvent
import models.field.FieldSet
import models.field.getCurrentDate
import models.order.Order
import models.types.FieldType
import mongo.MongoCredential
import mongo.MongoServerAddress
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import org.bson.BsonValue
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import panels.*
import utils.Constants
import utils.formatAsIntegerIfPossible
import java.text.SimpleDateFormat
import java.util.Date
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
                if (field.type is FieldType.File && value is FileEvent) {
                    put(field.fieldName, value.fileName)
                } else put(field.fieldName, value)
            }
        }

        return panel.getCollection().insertOne(document).insertedId?.toStringId()
    }

    suspend fun getCountOfCollections(panels: List<AdminMongoCollection>): Map<String, Long> = coroutineScope {
        val deferredCounts = panels.map { panel ->
            async {
                panel.getCollection().countDocuments()
            }
        }

        // Await all results and associate them with their collection names
        panels.map { it.getCollectionName() }.zip(deferredCounts.awaitAll()).toMap()
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
        parameters: Map<FieldSet, Pair<String, Any?>?>,
        primaryKey: String,
        initialData: List<String?>?,
    ): Pair<String?, List<String>>? {
        // If initialData is null, this is an insert operation
        if (initialData == null) {
            return insertData(parameters.mapValues { it.value?.second }, panel)?.let { id ->
                id to panel.getAllFields().map { it.fieldName.toString() }
            }
        } else {

            // Find fields that have changed values
            val updateFields = parameters.toList().filterIndexed { index, item ->
                val initialValue = initialData.getOrNull(index)
                val formattedValue = item.second?.first
                initialValue != formattedValue && !(initialValue != null && item.second == null)
            }

            // If no fields changed, return null
            if (updateFields.isEmpty()) return null

            val updatedFieldsBson = updateFields.mapNotNull {
                if (it.first.type is FieldType.File && it.second?.second is FileEvent) {
                    set(
                        it.first.fieldName.toString(),
                        (it.second?.second as FileEvent).fileName
                    )
                } else set(
                    it.first.fieldName.toString(),
                    it.second?.second ?: return@mapNotNull null
                )
            }.plus(
                panel.getAllAutoNowDateUpdateFields().map {
                    set(it.fieldName.orEmpty(), Date())
                }
            )

            val id = panel.getCollection()
                .updateOne(panel.getPrimaryKeyFilter(primaryKey), combine(updatedFieldsBson))
                .upsertedId?.toStringId()

            return id to updateFields.map { it.first.fieldName.toString() }
        }
    }

    suspend fun updateConfirmation(
        panel: AdminMongoCollection,
        primaryKey: String,
        field: FieldSet,
        value: String?
    ): String? {
        val updatedFieldBson = set(
            field.fieldName.toString(),
            value?.formatParameter(field) ?: return null
        )
        val id = panel.getCollection()
            .updateOne(panel.getPrimaryKeyFilter(primaryKey), updatedFieldBson)
            .upsertedId?.toStringId()
        return id
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
     * This function retrieves chart data from a MongoDB collection based on the specified aggregation function
     * and values fields from the provided chart dashboard section.
     *
     * @param panel The MongoDB collection to query.
     * @param section The chart dashboard section containing aggregation settings.
     * @return The chart data including labels and corresponding values.
     */
    suspend fun getChartData(panel: AdminMongoCollection, section: ChartDashboardSection): ChartData {
        // Destructure the section properties
        val aggregationFunction = section.aggregationFunction
        val labelsSet = mutableSetOf<String>()
        val pipeline = mutableListOf<Bson>()

        // Grouping stage based on labelField (not _id)
        val groupFields = mutableListOf<BsonField>()
        val groupId = "\$${section.labelField}"

        // Apply aggregation function to each value field
        section.valuesFields.forEach { field ->
            val fieldName = field.fieldName
            val aggField: BsonField = when (aggregationFunction) {
                ChartDashboardAggregationFunction.COUNT -> Accumulators.sum(fieldName, 1) // Count occurrences
                ChartDashboardAggregationFunction.SUM -> Accumulators.sum(fieldName, "\$$fieldName") // Sum values
                ChartDashboardAggregationFunction.AVERAGE -> Accumulators.avg(
                    fieldName,
                    "\$$fieldName"
                ) // Average values
                ChartDashboardAggregationFunction.ALL -> Accumulators.push(
                    fieldName,
                    "\$$fieldName"
                ) // Store all values without aggregation
            }
            groupFields.add(aggField)
        }

        // Add group stage to pipeline
        pipeline.add(group(groupId, groupFields))

        // Apply sorting if specified
        section.orderQuery?.let {
            val sortFields = it.trim().split(",").map { fieldSpec ->
                val parts = fieldSpec.trim().split(" ")
                val field = parts[0]
                val order = if (parts.getOrNull(1)?.equals("DESC", ignoreCase = true) == true) Sorts.descending(field)
                else Sorts.ascending(field)
                order
            }
            pipeline.add(sort(Sorts.orderBy(sortFields)))
        }

        // Apply limit if specified
        section.limitCount?.let {
            pipeline.add(limit(it))
        }

        // Execute the aggregation query and collect results
        val groupedData = mutableMapOf<String, MutableList<MutableList<Double>>>()
        val labels = mutableListOf<String>()

        // Collect the aggregation result from MongoDB
        val documents = panel.getCollection().aggregate(pipeline).toList()

        // Define a date format for labels that are Date type
        val labelFormatter = panel.getAllFields().find { it.fieldName == section.labelField }?.let { field ->
            when (field.type) {
                is FieldType.Date -> SimpleDateFormat("yyyy-MM-dd")
                is FieldType.DateTime, is FieldType.Instant -> SimpleDateFormat(Constants.LOCAL_DATETIME_FORMAT)
                else -> null
            }
        }

        // Iterate through the results to process each document
        documents.forEach { document ->
            val label = document.get("_id")
                ?.let {
                    when (it) {
                        is Date -> labelFormatter?.format(it) ?: it.toString() // If it's a Date, format it
                        else -> it.toString() // Otherwise, just convert it to string
                    }
                } ?: "Unknown" // Default if label is not present
            labelsSet.add(label)

            // Extract values for the specified fields based on the aggregation function
            val values = section.valuesFields.map { field ->
                val fieldName = field.fieldName
                if (aggregationFunction == ChartDashboardAggregationFunction.ALL) {
                    // For ALL aggregation, push all the values
                    document.get(fieldName)?.let { value ->
                        (value as? List<*>)?.mapNotNull { it?.toString()?.toDoubleOrNull() } ?: listOf(0.0)
                    } ?: listOf(0.0)
                } else {
                    // For other aggregation functions (COUNT, SUM, AVERAGE)
                    document.get(fieldName)?.toString()?.toDoubleOrNull() ?: 0.0
                }
            }

            // Store values based on aggregation type
            if (aggregationFunction == ChartDashboardAggregationFunction.ALL) {
                groupedData.computeIfAbsent(label) { MutableList(section.valuesFields.size) { mutableListOf<Double>() } }
                    .forEachIndexed { index, list ->
                        // Ensure all values are of type Double and add to the list
                        (values[index] as? List<*>)
                            ?.filterIsInstance<Double>()
                            ?.let { list.addAll(it) }
                    }
            } else {
                groupedData.computeIfAbsent(label) { MutableList(section.valuesFields.size) { mutableListOf(0.0) } }
                    .forEachIndexed { index, list ->
                        list[0] += values[index].toString().toDoubleOrNull()
                            ?: 0.0 // Update the list with the aggregated value
                    }
            }
        }

        // Prepare labels for the chart
        labels.addAll(labelsSet)

        // Construct chart values for each field
        val values = section.valuesFields.mapIndexed { index, field ->
            val currentValues = labels.map { label ->
                groupedData[label]?.get(index)?.firstOrNull() ?: 0.0
            }

            // Create ChartLabelsWithValues for each field
            ChartLabelsWithValues(
                displayName = field.displayName,
                values = currentValues,
                fillColors = labels.map { section.provideFillColor(it, field.displayName) },
                borderColors = labels.map { section.provideBorderColor(it, field.displayName) }
            )
        }

        // Return the final chart data
        return ChartData(
            labels = labels.toList(),
            values = values,
            section = section
        )
    }

    /**
     * Retrieves aggregated data for a specific field in a MongoDB collection based on the given TextDashboardSection.
     *
     * This function dynamically constructs an aggregation pipeline to handle various aggregation functions
     * such as LAST_ITEM, PROFIT_PERCENTAGE, COUNT, AVERAGE, and SUM. It also supports sorting based on the
     * 'orderQuery' if specified.
     *
     * The following aggregation operations are supported:
     * - LAST_ITEM: Fetches the most recent item based on sorting.
     * - PROFIT_PERCENTAGE: Calculates the percentage difference between the two most recent items.
     * - COUNT: Counts the total number of documents.
     * - AVERAGE/SUM: Performs averaging or summing on the specified field.
     *
     * The aggregation pipeline is executed using MongoDB's aggregation framework, and the resulting data
     * is processed based on the aggregation function applied.
     *
     * @param panel The AdminMongoCollection from which to fetch data.
     * @param section The TextDashboardSection containing configuration and aggregation details.
     * @return A TextData object containing the aggregated value and the section information.
     */
    suspend fun getTextData(panel: AdminMongoCollection, section: TextDashboardSection): TextData {
        val fieldName = section.fieldName
        val aggregationFunction = section.aggregationFunction
        val pipeline = mutableListOf<Bson>()

        // Add dynamic sorting if 'orderQuery' is provided
        section.orderQuery?.let {
            val sortFields = it.trim().split(",").map { fieldSpec ->
                val parts = fieldSpec.trim().split(" ")
                val field = parts[0]
                val order = if (parts.getOrNull(1)?.equals("DESC", ignoreCase = true) == true) Sorts.descending(field)
                else Sorts.ascending(field)
                order
            }
            pipeline.add(sort(Sorts.orderBy(sortFields)))
        }

        // Apply aggregation function based on the aggregation type
        when (aggregationFunction) {
            TextDashboardAggregationFunction.LAST_ITEM -> {
                // Get the last item (sorted by date if needed)
                pipeline.add(Aggregates.limit(1))  // Limit to 1 document to get the last item
                pipeline.add(Aggregates.project(include(fieldName)))  // Project only the needed field
            }

            TextDashboardAggregationFunction.PROFIT_PERCENTAGE -> {
                // Profit percentage: (nextItem - prevItem) / prevItem * 100
                pipeline.add(Aggregates.limit(2))  // Limit to last 2 items
                pipeline.add(Aggregates.project(include(fieldName)))  // Project the needed field
                // To calculate the percentage, you will need to process the results after fetching
            }

            TextDashboardAggregationFunction.COUNT -> {
                // Count the number of documents
                pipeline.add(Aggregates.count("aggregationFunctionValue"))
            }

            else -> {
                // For aggregation functions like AVERAGE, SUM, etc.
                val aggregationFunctionQuery = when (aggregationFunction) {
                    TextDashboardAggregationFunction.AVERAGE -> Accumulators.avg(fieldName, "\$$fieldName")
                    TextDashboardAggregationFunction.SUM -> Accumulators.sum(fieldName, "\$$fieldName") // Sum values
                    else -> null
                }
                aggregationFunctionQuery?.let {
                    pipeline.add(group(null, it))  // Grouping for aggregation function (e.g., AVG or SUM)
                }
            }
        }

        // Execute the aggregation pipeline
        val result = panel.getCollection().aggregate(pipeline).toList()

        // Process the result based on the aggregation function
        val value = when (aggregationFunction) {
            TextDashboardAggregationFunction.LAST_ITEM -> {
                if (result.isNotEmpty()) {
                    result.first().get(fieldName)?.toString() ?: ""
                } else ""
            }

            TextDashboardAggregationFunction.PROFIT_PERCENTAGE -> {
                if (result.size == 2) {
                    val nextItem = result[0].get(fieldName)?.toString()?.toDoubleOrNull() ?: 0.0
                    val prevItem = result[1].get(fieldName)?.toString()?.toDoubleOrNull() ?: 0.0
                    runCatching {
                        ((nextItem - prevItem) / prevItem * 100).formatAsIntegerIfPossible()
                    }.getOrElse { "" }
                } else ""
            }

            TextDashboardAggregationFunction.COUNT -> {
                result.firstOrNull()?.getInteger("aggregationFunctionValue")?.toString() ?: ""
            }

            else -> {
                result.firstOrNull()?.get(fieldName)?.toString() ?: ""
            }
        }

        return TextData(
            value = value,
            section = section
        )
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