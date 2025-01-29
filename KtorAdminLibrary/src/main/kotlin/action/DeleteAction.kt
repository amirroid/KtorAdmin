package action

import panels.AdminJdbcTable
import panels.AdminMongoCollection
import panels.AdminPanel
import repository.JdbcQueriesRepository
import repository.MongoClientRepository

internal class DeleteAction(private val panel: AdminPanel) : CustomAdminAction {
    override var key: String = "DELETE"

    override val displayText: String
        get() = "Delete selected items"

    override suspend fun performAction(name: String, selectedIds: List<String>) {
        when (panel) {
            is AdminJdbcTable -> {
                JdbcQueriesRepository.deleteRows(panel, selectedIds)
            }
            is AdminMongoCollection -> {
                MongoClientRepository.deleteRows(panel, selectedIds)
            }
        }
    }
}