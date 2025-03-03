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
import kotlinx.coroutines.flow.count
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
import java.util.logging.Filter

internal object MongoClientRepository {
    private var databases: MutableMap<String?, MongoDatabase> = mutableMapOf()

    fun registerNewClient(
        key: String?,
        databaseName: String,
        address: MongoServerAddress,
        credential: MongoCredential? = null
    ) {
        if (databases.containsKey(key)) {
            throw IllegalArgumentException("Client with key '$key' already exists")
        }
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
            .applyToClusterSettings { cluster -> cluster.hosts(listOf(ServerAddress(address.host, address.port))) }
            .build()
        databases[key] = MongoClient.create(settings).getDatabase(databaseName)
    }

    private fun AdminMongoCollection.getCollection(): MongoCollection<Document> {
        val databaseKey = getDatabaseKey()
        val database =
            databases[databaseKey] ?: throw IllegalArgumentException("Client for database name $databaseKey not found.")
        return database.getCollection(getCollectionName())
    }

    private fun AdminMongoCollection.getPrimaryKeyFilter(primaryKey: String) =
        if (getPrimaryKeyField().type == FieldType.ObjectId || getPrimaryKey() == "_id") {
            eq(getPrimaryKey(), ObjectId(primaryKey))
        } else eq(getPrimaryKey(), primaryKey)

    private fun BsonValue.toStringId() = asObjectId()?.value?.toHexString()

    private fun String.formatParameter(field: FieldSet) = when (field.type) {
        FieldType.Boolean -> if (this == "on") "true" else "false"
        else -> this
    }


    suspend fun insertData(
        values: Map<FieldSet, Any?>,
        panel: AdminMongoCollection
    ): String? {
        val changeDates = panel.getAllAutoNowDateInsertFields().map {
            it to it.getCurrentDate()
        }
        val document = Document().apply {
            values.toList().plus(changeDates).distinctBy { it.first }.forEach { (field, value) ->
                put(field.fieldName, value?.toString()?.formatParameter(field) ?: return@forEach)
            }
            panel.getAllAutoNowDateInsertFields()
        }
        return panel.getCollection().insertOne(document).insertedId?.toStringId()
    }

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
                if (order != null) {
                    when (order.direction.lowercase()) {
                        "asc" -> it.sort(Sorts.ascending(order.name))
                        "desc" -> it.sort(Sorts.descending(order.name))
                        else -> it
                    }
                } else it
            }
            .projection(
                fields(
                    fields(
                        *fields.plus(table.getPrimaryKeyField())
                            .map { include(it.fieldName) }
                            .distinct()
                            .toTypedArray()
                    )
                )
            ).filterNotNull().toList()
        return projection.map { values ->
            DataWithPrimaryKey(
                primaryKey = values[table.getPrimaryKey()]!!.toString(),
                data = fields.map { field -> values[field.fieldName]?.toString() }
            )
        }
    }

    suspend fun getCount(
        table: AdminMongoCollection,
        filters: Bson,
    ): Long {
        return table.getCollection().countDocuments(filters)
    }


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

    suspend fun updateChangedData(
        panel: AdminMongoCollection,
        parameters: Map<FieldSet, String?>,
        primaryKey: String,
        initialData: List<String?>?,
    ): Pair<String?, List<String>>? {
        if (initialData == null) {
            return insertData(parameters, panel)?.let { id ->
                id to panel.getAllFields().map { it.fieldName.toString() }
            }
        } else {
            val changeDates = panel.getAllAutoNowDateUpdateFields().map {
                it to it.getCurrentDate()
            }
            val updateFields = parameters.toList().filterIndexed { index, item ->
                val initialValue = initialData.getOrNull(index)
                initialValue != item.second?.formatParameter(item.first) && !(initialValue != null && item.second == null)
            }.plus(changeDates).distinctBy { it.first }
            val updatedFieldsBson = updateFields.mapNotNull {
                set(
                    it.first.fieldName.toString(),
                    it.second?.formatParameter(it.first) ?: return@mapNotNull null
                )
            }
            if (updateFields.isEmpty()) return null
            val id = panel.getCollection()
                .updateOne(panel.getPrimaryKeyFilter(primaryKey), combine(updatedFieldsBson)).upsertedId?.toStringId()
            return id to updateFields.map { it.first.fieldName.toString() }
        }
    }

    private fun AdminMongoCollection.getPrimaryKeyFilters(primaryKeys: List<String>) =
        if (getPrimaryKeyField().type == FieldType.ObjectId || getPrimaryKey() == "_id") {
            Filters.`in`(getPrimaryKey(), primaryKeys.map { ObjectId(it) })
        } else {
            Filters.`in`(getPrimaryKey(), primaryKeys)
        }


    suspend fun deleteRows(panel: AdminMongoCollection, selectedIds: List<String>) {
        panel.getCollection().deleteMany(
            panel.getPrimaryKeyFilters(selectedIds)
        )
    }
}