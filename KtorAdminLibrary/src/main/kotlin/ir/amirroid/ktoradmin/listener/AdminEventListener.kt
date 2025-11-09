package ir.amirroid.ktoradmin.listener

import ir.amirroid.ktoradmin.models.events.ColumnEvent
import ir.amirroid.ktoradmin.models.events.FieldEvent

/**
 * A generic listener for handling database-related events.
 * This class provides methods to handle insert, update, and delete operations.
 * It can be extended to define specific behaviors for different data operations.
 */
open class AdminEventListener {

    /**
     * Handles the insertion of new data into a relational database table.
     *
     * @param tableName The name of the table where data is inserted.
     * @param objectPrimaryKey The primary key of the inserted object.
     * @param events A list of column events representing changes in the data.
     */
    open suspend fun onInsertJdbcData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>
    ) = Unit

    /**
     * Handles the insertion of new data into a MongoDB collection.
     *
     * @param collectionName The name of the collection where data is inserted.
     * @param objectPrimaryKey The primary key of the inserted object.
     * @param events A list of field events representing changes in the document.
     */
    open suspend fun onInsertMongoData(
        collectionName: String,
        objectPrimaryKey: String,
        events: List<FieldEvent>
    ) = Unit

    /**
     * Handles the update operation in a relational database table.
     *
     * @param tableName The name of the table where data is updated.
     * @param objectPrimaryKey The primary key of the updated object.
     * @param events A list of column events representing changes in the data.
     */
    open suspend fun onUpdateJdbcData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>
    ) = Unit

    /**
     * Handles the update operation in a MongoDB collection.
     *
     * @param collectionName The name of the collection where data is updated.
     * @param objectPrimaryKey The primary key of the updated object.
     * @param events A list of field events representing changes in the document.
     */
    open suspend fun onUpdateMongoData(
        collectionName: String,
        objectPrimaryKey: String,
        events: List<FieldEvent>
    ) = Unit

    /**
     * Handles the deletion of an object from a relational database table.
     *
     * @param tableName The name of the table where data is deleted.
     * @param objectPrimaryKeys The primary keys of the deleted object.
     */
    open suspend fun onDeleteJdbcObjects(
        tableName: String,
        objectPrimaryKeys: List<String>,
    ) = Unit

    /**
     * Handles the deletion of an object from a MongoDB collection.
     *
     * @param collectionName The name of the collection where data is deleted.
     * @param objectPrimaryKeys The primary keys of the deleted object.
     */
    open suspend fun onDeleteMongoObjects(
        collectionName: String,
        objectPrimaryKeys: List<String>,
    ) = Unit
}