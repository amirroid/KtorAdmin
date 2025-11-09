package ir.amirroid.ktoradmin.panels

import ir.amirroid.ktoradmin.action.CustomAdminAction
import ir.amirroid.ktoradmin.action.DeleteAction
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.models.actions.Action
import ir.amirroid.ktoradmin.models.order.Order

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
    fun getIconFile(): String?
    fun isShowInAdminPanel(): Boolean
}

fun List<AdminPanel>.findWithPluralName(name: String?) = find { it.getPluralName() == name }

fun AdminPanel.getAllCustomActions(deleteActionDisplayText: String = "Delete selected items"): List<CustomAdminAction> {
    if (getCustomActions().any { it !in DynamicConfiguration.customActions.map { action -> action.key } }) {
        throw IllegalStateException("(${getPluralName()}) One or more custom actions are not registered in DynamicConfiguration.")
    }
    return buildList {
        addAll(DynamicConfiguration.customActions.filter {
            it.key in getCustomActions()
        })
        addAll(DynamicConfiguration.forAllCustomActions)
        if (getDefaultActions().contains(Action.DELETE)) add(
            DeleteAction(
                this@getAllCustomActions,
                displayText = deleteActionDisplayText
            )
        )
    }
}

val AdminPanel.hasAddAction: Boolean
    get() = getDefaultActions().contains(Action.ADD)

val AdminPanel.hasEditAction: Boolean
    get() = getDefaultActions().contains(Action.EDIT)