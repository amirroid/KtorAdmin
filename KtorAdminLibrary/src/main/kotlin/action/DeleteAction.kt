package action

import configuration.DynamicConfiguration
import models.FileDeleteStrategy
import models.UploadTarget
import models.types.ColumnType
import models.types.FieldType
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import repository.FileRepository
import repository.JdbcQueriesRepository
import repository.MongoClientRepository

/**
 * Represents a delete action that removes selected items from an admin panel.
 * Supports deletion from both JDBC and MongoDB-based panels.
 */
internal class DeleteAction(private val panel: AdminPanel, override val displayText: String) :
    CustomAdminAction {

    // Unique key representing the delete action
    override var key: String = "DELETE"

    /**
     * Performs the delete action based on the type of admin panel.
     *
     * @param name The name of the entity being deleted.
     * @param selectedIds The list of IDs of the items to be deleted.
     */
    override suspend fun performAction(name: String, selectedIds: List<String>) {
        when (panel) {
            is AdminJdbcTable -> {
                // Deletes rows from a JDBC-based table
                deleteJdbcFiles(panel, selectedIds)
                JdbcQueriesRepository.deleteRows(panel, selectedIds)
            }

            is AdminMongoCollection -> {
                // Deletes documents from a MongoDB collection
                deleteMongoFiles(panel, selectedIds)
                MongoClientRepository.deleteRows(panel, selectedIds)
            }
        }

        // Notify event listeners about the deletion
        sendEvent(name, selectedIds)
    }

    /**
     * Deletes files associated with the specified rows in a JDBC table.
     *
     * Filters file columns based on their delete strategy and then deletes the files
     * from their respective storage targets.
     *
     * @param table The JDBC table containing file columns
     * @param selectedIds List of row IDs whose files should be deleted
     */
    private suspend fun deleteJdbcFiles(
        table: AdminJdbcTable,
        selectedIds: List<String>
    ) {
        val fileColumns = table.getAllColumns()
            .filter { it.type == ColumnType.FILE && it.uploadTarget != null }
            .filter {
                val uploadTarget = it.uploadTarget!!
                val deleteStrategy = when (uploadTarget) {
                    is UploadTarget.LocalFile -> uploadTarget.deleteStrategy
                    is UploadTarget.AwsS3 -> uploadTarget.deleteStrategy
                    is UploadTarget.Custom -> return@filter true
                }

                when (deleteStrategy) {
                    FileDeleteStrategy.DELETE -> true
                    FileDeleteStrategy.KEEP -> false
                    FileDeleteStrategy.INHERIT -> DynamicConfiguration.fileDeleteStrategy == FileDeleteStrategy.DELETE
                }
            }

        JdbcQueriesRepository.getSelectedColumnsForIds(table, selectedIds, fileColumns)
            .forEach { rows ->
                rows.forEachIndexed { index, value ->
                    if (value == null) return@forEachIndexed

                    val column = fileColumns[index]
                    val uploadTarget = column.uploadTarget!!
                    FileRepository.deleteFile(uploadTarget, value.toString())
                }
            }
    }


    /**
     * Deletes files associated with the specified documents in a Mongo collection.
     *
     * Filters file fields based on their delete strategy and then deletes the files
     * from their respective storage targets.
     *
     * @param panel The Mongo collection containing file fields
     * @param selectedIds List of document IDs whose files should be deleted
     */
    private suspend fun deleteMongoFiles(
        collection: AdminMongoCollection,
        selectedIds: List<String>
    ) {
        val fileFields = collection.getAllFields()
            .filter { it.type is FieldType.File && it.uploadTarget != null }
            .filter {
                val uploadTarget = it.uploadTarget!!
                val deleteStrategy = when (uploadTarget) {
                    is UploadTarget.LocalFile -> uploadTarget.deleteStrategy
                    is UploadTarget.AwsS3 -> uploadTarget.deleteStrategy
                    is UploadTarget.Custom -> return@filter true
                }

                when (deleteStrategy) {
                    FileDeleteStrategy.DELETE -> true
                    FileDeleteStrategy.KEEP -> false
                    FileDeleteStrategy.INHERIT -> DynamicConfiguration.fileDeleteStrategy == FileDeleteStrategy.DELETE
                }
            }

        MongoClientRepository.getSelectedFieldsForIds(collection, selectedIds, fileFields)
            .forEach { rows ->
                rows.forEachIndexed { index, value ->
                    if (value == null) return@forEachIndexed

                    val field = fileFields[index]
                    val uploadTarget = field.uploadTarget!!
                    FileRepository.deleteFile(uploadTarget, value.toString())
                }
            }
    }

    /**
     * Sends a delete event to the appropriate event listener if available.
     *
     * @param name The name of the entity being deleted.
     * @param selectedIds The list of deleted item IDs.
     */
    private suspend fun sendEvent(name: String, selectedIds: List<String>) {
        DynamicConfiguration.currentEventListener?.let { eventListener ->
            when (panel) {
                is AdminJdbcTable -> eventListener.onDeleteJdbcObjects(name, selectedIds)
                is AdminMongoCollection -> eventListener.onDeleteMongoObjects(name, selectedIds)
            }
        }
    }
}