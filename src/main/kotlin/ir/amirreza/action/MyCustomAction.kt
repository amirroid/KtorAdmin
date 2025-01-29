package ir.amirreza.action

import action.CustomAdminAction

class MyCustomAction : CustomAdminAction {
    override var key: String = "delete"
    override val displayText: String
        get() = "Delete all"

    override suspend fun performAction(name: String, selectedIds: List<String>) {
    }
}