package configuration

import action.CustomAdminAction
import dashboard.KtorAdminDashboard
import listener.AdminEventListener
import mapper.KtorAdminValueMapper
import models.forms.LoginFiled
import models.menu.Menu
import preview.KtorAdminPreview
import tiny.TinyMCEConfig
import java.time.ZoneId
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
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

    /** List of fields required for login functionality (Thread-safe) */
    private val _loginFields = ConcurrentLinkedQueue<LoginFiled>()
    var loginFields: List<LoginFiled>
        get() = _loginFields.toList()
        set(value) {
            _loginFields.clear()
            _loginFields.addAll(value)
        }

    /** Custom actions specific to certain admin panel sections (Thread-safe) */
    private val _customActions = ConcurrentLinkedQueue<CustomAdminAction>()
    val customActions: List<CustomAdminAction>
        get() = _customActions.toList()

    /** Custom actions that apply to all admin panel sections (Thread-safe) */
    private val _forAllCustomActions = ConcurrentLinkedQueue<CustomAdminAction>()
    val forAllCustomActions: List<CustomAdminAction>
        get() = _forAllCustomActions.toList()

    /** Current event listener for admin panel events (Thread-safe) */
    private val _currentEventListener = AtomicReference<AdminEventListener?>()
    var currentEventListener: AdminEventListener?
        get() = _currentEventListener.get()
        set(value) {
            if (!_currentEventListener.compareAndSet(null, value)) {
                throw IllegalStateException("An event listener is already registered. Please unregister it before registering a new one.")
            }
        }

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

    var adminPath = "admin"

    var loginPageMessage : String? = null

    /**
     * If debugMode is enabled, error messages will be displayed.
     * Otherwise, no message will be shown for better security.
     */
    var debugMode = true

    /**
     * Authentication name (if required).
     */
    var authenticateName: String? = null

    /** Value mappers (Thread-safe) */
    private val _valueMappers = ConcurrentLinkedQueue<KtorAdminValueMapper>()
    val valueMappers: List<KtorAdminValueMapper>
        get() = _valueMappers.toList()

    /** Previews (Thread-safe) */
    private val _previews = ConcurrentLinkedQueue<KtorAdminPreview>()
    val previews: List<KtorAdminPreview>
        get() = _previews.toList()


    var menuProvider: ((String?) -> List<Menu>)? = null

    /**
     * Registers a new value mapper.
     * @param valueMapper The value mapper to register.
     * @throws IllegalStateException if a value mapper with the same key is already registered.
     */
    fun registerValueMapper(valueMapper: KtorAdminValueMapper) {
        if (_valueMappers.any { it.key == valueMapper.key }) {
            throw IllegalStateException("A ValueMapper with the key '${valueMapper.key}' is already registered.")
        }
        _valueMappers.add(valueMapper)
    }

    /**
     * Registers a new preview.
     * @param preview The preview to register.
     * @throws IllegalStateException if a preview with the same key is already registered.
     */
    fun registerPreview(preview: KtorAdminPreview) {
        if (_previews.any { it.key == preview.key }) {
            throw IllegalStateException("Preview with key '${preview.key}' is already registered.")
        }
        _previews.add(preview)
    }

    /**
     * Retrieves a preview by its key.
     * @param key The key of the preview.
     * @return The corresponding preview.
     * @throws NoSuchElementException if no preview is found with the given key.
     */
    fun getPreview(key: String): KtorAdminPreview {
        return previews.firstOrNull { it.key == key }
            ?: throw NoSuchElementException("No preview found with key '$key'.")
    }

    /**
     * Registers a custom admin action for specific sections.
     * @param customAction The custom action to register.
     * @throws IllegalArgumentException if an action with the same key is already registered.
     */
    fun registerCustomAdminAction(customAction: CustomAdminAction) {
        if (_customActions.any { it.key == customAction.key }) {
            throw IllegalArgumentException("A custom action with key '${customAction.key}' is already registered.")
        }
        _customActions.add(customAction)
    }

    /**
     * Registers a custom admin action that applies to all sections.
     * @param customAction The custom action to register.
     * @throws IllegalArgumentException if an action with the same key is already registered.
     */
    fun registerCustomAdminActionForAll(customAction: CustomAdminAction) {
        if (_forAllCustomActions.any { it.key == customAction.key }) {
            throw IllegalArgumentException("A custom action with key '${customAction.key}' is already registered.")
        }
        _forAllCustomActions.add(customAction)
    }
}