package configuration

import action.CustomAdminAction
import dashboard.KtorAdminDashboard
import listener.AdminEventListener
import models.forms.LoginFiled
import tiny.TinyMCEConfig
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

internal object DynamicConfiguration {
    var maxItemsInPage: Int = 20
    var loginFields: List<LoginFiled> = emptyList()
    var customActions: List<CustomAdminAction> = emptyList()
    var forAllCustomActions: List<CustomAdminAction> = emptyList()
    var currentEventListener: AdminEventListener? = null
    var timeZone: ZoneId = ZoneId.systemDefault()
    var formsLifetime = 1_000 * 60L
    var dashboard: KtorAdminDashboard? = null
    var authenticationSessionMaxAge: Duration = 10.days
    var tinyMCEConfig = TinyMCEConfig.Basic
    var rateLimitPerMinutes = 20
    var canDownloadDataAsCsv = false


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