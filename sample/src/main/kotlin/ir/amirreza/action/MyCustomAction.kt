package ir.amirreza.action

import ir.amirroid.ktoradmin.action.ActionOptions
import ir.amirroid.ktoradmin.action.CustomAdminAction

class MyCustomAction : CustomAdminAction {
    override var key: String = "delete"
    override val displayText: String
        get() = "Delete all"

    override val options: ActionOptions
        get() = ActionOptions(showInEditPage = false)

    override suspend fun performAction(name: String, selectedIds: List<String>) {
        // Do action
    }
}