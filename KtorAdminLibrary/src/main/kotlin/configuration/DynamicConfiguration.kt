package configuration

import action.CustomAdminAction
import listener.AdminEventListener
import models.forms.LoginFiled
import java.time.ZoneId

internal object DynamicConfiguration {
    var maxItemsInPage: Int = 20
    var loginFields: List<LoginFiled> = emptyList()
    var customActions: List<CustomAdminAction> = emptyList()
    var forAllCustomActions: List<CustomAdminAction> = emptyList()
    var currentEventListener: AdminEventListener? = null
    var cryptoPassword: String? = null
    var timeZone: ZoneId = ZoneId.systemDefault()
    var formsLifetime = 1_000 * 60L


    fun registerEventListener(listener: AdminEventListener) {
        if (currentEventListener != null) {
            throw IllegalStateException("An event listener is already registered. Please unregister it before registering a new one.")
        }
        currentEventListener = listener
    }

    fun registerCustomAdminAction(customAction: CustomAdminAction) {
        if (customAction.key in customActions.map { it.key }) {
            throw IllegalArgumentException("A custom action with key '${customAction.key}' is already registered.")
        }
        customActions += customAction
    }

    fun registerCustomAdminActionForAll(customAction: CustomAdminAction) {
        if (customAction.key in forAllCustomActions.map { it.key }) {
            throw IllegalArgumentException("A custom action with key '${customAction.key}' is already registered.")
        }
        forAllCustomActions += customAction
    }
}