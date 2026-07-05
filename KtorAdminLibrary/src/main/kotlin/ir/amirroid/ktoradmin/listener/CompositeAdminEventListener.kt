package ir.amirroid.ktoradmin.listener

import ir.amirroid.ktoradmin.models.events.ColumnEvent
import ir.amirroid.ktoradmin.models.events.FieldEvent
import java.util.concurrent.CopyOnWriteArrayList

internal class CompositeAdminEventListener(
    listeners: Collection<AdminEventListener> = emptyList(),
) : AdminEventListener() {
    private val listeners = CopyOnWriteArrayList(listeners)

    fun add(listener: AdminEventListener) {
        listeners.add(listener)
    }

    override suspend fun onInsertJdbcData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>,
    ) {
        listeners.forEach { it.onInsertJdbcData(tableName, objectPrimaryKey, events) }
    }

    override suspend fun onInsertMongoData(
        collectionName: String,
        objectPrimaryKey: String,
        events: List<FieldEvent>,
    ) {
        listeners.forEach { it.onInsertMongoData(collectionName, objectPrimaryKey, events) }
    }

    override suspend fun onUpdateJdbcData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>,
    ) {
        listeners.forEach { it.onUpdateJdbcData(tableName, objectPrimaryKey, events) }
    }

    override suspend fun onUpdateMongoData(
        collectionName: String,
        objectPrimaryKey: String,
        events: List<FieldEvent>,
    ) {
        listeners.forEach { it.onUpdateMongoData(collectionName, objectPrimaryKey, events) }
    }

    override suspend fun onDeleteJdbcObjects(
        tableName: String,
        objectPrimaryKeys: List<String>,
    ) {
        listeners.forEach { it.onDeleteJdbcObjects(tableName, objectPrimaryKeys) }
    }

    override suspend fun onDeleteMongoObjects(
        collectionName: String,
        objectPrimaryKeys: List<String>,
    ) {
        listeners.forEach { it.onDeleteMongoObjects(collectionName, objectPrimaryKeys) }
    }
}
