package listener

import models.events.ColumnEvent

/**
 * This class is a generic listener for handling events related to data operations.
 * It includes methods for insert, update, and delete data operations.
 * This class can be extended to implement specific behaviors for each data operation.
 */
open class AdminEventListener {

    /**
     * Method to handle data insertion operation in a table.
     *
     * @param tableName the name of the table
     * @param objectPrimaryKey the primary key of the object
     * @param events a list of column events representing changes in the data
     */
    open suspend fun onInsertData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>
    ) = Unit

    /**
     * Method to handle data update operation in a table.
     *
     * @param tableName the name of the table
     * @param objectPrimaryKey the primary key of the object
     * @param events a list of column events representing changes in the data
     */
    open suspend fun onUpdateData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>
    ) = Unit

    /**
     * Method to handle data deletion operation from a table.
     *
     * @param tableName the name of the table
     * @param objectPrimaryKey the primary key of the object
     */
    open suspend fun onDeleteObject(
        tableName: String,
        objectPrimaryKey: String,
    ) = Unit
}