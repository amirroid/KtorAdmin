package action

import configuration.DynamicConfiguration
import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import repository.JdbcQueriesRepository
import repository.MongoClientRepository

/**
 * Represents a delete action that removes selected items from an admin panel.
 * Supports deletion from both JDBC and MongoDB-based panels.
 */
internal class DeleteAction(private val panel: AdminPanel, override val displayText: String) : CustomAdminAction {

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
                JdbcQueriesRepository.deleteRows(panel, selectedIds)
            }

            is AdminMongoCollection -> {
                // Deletes documents from a MongoDB collection
                MongoClientRepository.deleteRows(panel, selectedIds)
            }
        }

        // Notify event listeners about the deletion
        sendEvent(name, selectedIds)
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