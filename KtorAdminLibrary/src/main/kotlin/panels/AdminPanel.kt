package panels

import action.CustomAdminAction
import action.DeleteAction
import configuration.DynamicConfiguration
import models.actions.Action
import models.order.Order

interface AdminPanel {
    fun getSingularName(): String
    fun getPluralName(): String
    fun getGroupName(): String?
    fun getDatabaseKey(): String?
    fun getPrimaryKey(): String
    fun getDisplayFormat(): String?
    fun getSearches(): List<String>
    fun getFilters(): List<String>
    fun getAccessRoles(): List<String>?
    fun getDefaultOrder(): Order?
    fun getDefaultActions(): List<Action>
    fun getCustomActions(): List<String>
}

fun List<AdminPanel>.findWithPluralName(name: String?) = find { it.getPluralName() == name }

fun AdminPanel.getAllCustomActions(): List<CustomAdminAction> {
    if (getCustomActions().any { it !in DynamicConfiguration.customActions.map { action -> action.key } }) {
        throw IllegalStateException("(${getPluralName()}) One or more custom actions are not registered in DynamicConfiguration.")
    }
    return buildList {
        addAll(DynamicConfiguration.customActions.filter {
            it.key in getCustomActions()
        })
        addAll(DynamicConfiguration.forAllCustomActions)
        if (getDefaultActions().contains(Action.DELETE)) add(DeleteAction(this@getAllCustomActions))
    }
}