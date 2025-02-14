package configuration

import action.CustomAdminAction
import dashboard.KtorAdminDashboard
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import listener.AdminEventListener
import mapper.KtorAdminValueMapper
import models.forms.LoginFiled
import tiny.TinyMCEConfig
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Dynamic configuration object for managing admin panel settings and behaviors.
 * Provides centralized configuration for pagination, authentication, custom actions,
 * event handling, and various feature toggles.
 */
internal object DynamicConfiguration {
    /** Maximum number of items to display per page in listings */
    var maxItemsInPage: Int = 20

    /** List of fields required for login functionality */
    var loginFields: List<LoginFiled> = emptyList()

    /** Custom actions specific to certain admin panel sections */
    var customActions: List<CustomAdminAction> = emptyList()

    /** Custom actions that apply to all admin panel sections */
    var forAllCustomActions: List<CustomAdminAction> = emptyList()

    /** Current event listener for admin panel events */
    var currentEventListener: AdminEventListener? = null

    /** Timezone configuration for date/time handling */
    var timeZone: ZoneId = ZoneId.systemDefault()

    /** Lifetime duration for forms in milliseconds */
    var formsLifetime = 1_000 * 60L

    /** Dashboard configuration instance */
    var dashboard: KtorAdminDashboard? = null

    /** Maximum age for authentication sessions */
    var authenticationSessionMaxAge: Duration = 10.days

    /** TinyMCE editor configuration */
    var tinyMCEConfig = TinyMCEConfig.Basic

    /** Rate limit for requests per minute */
    var rateLimitPerMinutes = 30

    /** Flag to enable/disable CSV data download */
    var canDownloadDataAsCsv = false

    /** Flag to enable/disable PDF data download */
    var canDownloadDataAsPdf = false

    val valueMappers = mutableListOf<KtorAdminValueMapper>()

    fun registerValueMapper(valueMapper: KtorAdminValueMapper) {
        if (valueMappers.any { it.key == valueMapper.key }) {
            throw IllegalStateException("A ValueMapper with the key '${valueMapper.key}' is already registered.")
        }
        valueMappers += valueMapper
    }


    /**
     * Registers an event listener for admin panel events.
     * Only one listener can be active at a time.
     *
     * @param listener The event listener to register
     * @throws IllegalStateException if another listener is already registered
     */
    fun registerEventListener(listener: AdminEventListener) {
        if (currentEventListener != null) {
            throw IllegalStateException("An event listener is already registered. Please unregister it before registering a new one.")
        }
        currentEventListener = listener
    }

    /**
     * Registers a custom admin action for specific sections.
     * Each action must have a unique key.
     *
     * @param customAction The custom action to register
     * @throws IllegalArgumentException if an action with the same key is already registered
     */
    fun registerCustomAdminAction(customAction: CustomAdminAction) {
        if (customAction.key in customActions.map { it.key }) {
            throw IllegalArgumentException("A custom action with key '${customAction.key}' is already registered.")
        }
        customActions += customAction
    }

    /**
     * Registers a custom admin action that applies to all sections.
     * Each action must have a unique key.
     *
     * @param customAction The custom action to register
     * @throws IllegalArgumentException if an action with the same key is already registered
     */
    fun registerCustomAdminActionForAll(customAction: CustomAdminAction) {
        if (customAction.key in forAllCustomActions.map { it.key }) {
            throw IllegalArgumentException("A custom action with key '${customAction.key}' is already registered.")
        }
        forAllCustomActions += customAction
    }
}