package ir.amirroid.ktoradmin.configuration

import ir.amirroid.ktoradmin.action.CustomAdminAction
import ir.amirroid.ktoradmin.dashboard.DashboardEntry
import ir.amirroid.ktoradmin.dashboard.KtorAdminDashboard
import ir.amirroid.ktoradmin.listener.AdminEventListener
import ir.amirroid.ktoradmin.listener.CompositeAdminEventListener
import ir.amirroid.ktoradmin.mapper.KtorAdminValueMapper
import ir.amirroid.ktoradmin.models.FileDeleteStrategy
import ir.amirroid.ktoradmin.models.forms.LoginFiled
import ir.amirroid.ktoradmin.models.menu.Menu
import ir.amirroid.ktoradmin.pages.CustomAdminPage
import ir.amirroid.ktoradmin.pages.CustomPage
import ir.amirroid.ktoradmin.pages.CustomPageEntry
import ir.amirroid.ktoradmin.preview.KtorAdminPreview
import ir.amirroid.ktoradmin.template.AdminTemplate
import ir.amirroid.ktoradmin.template.DefaultAdminTemplate
import ir.amirroid.ktoradmin.tiny.TinyMCEConfig
import ir.amirroid.ktoradmin.translator.KtorAdminTranslator
import ir.amirroid.ktoradmin.translator.locals.en.EnglishKtorAdminTranslator
import java.time.ZoneId
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
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
            value ?: run {
                _currentEventListener.set(null)
                return
            }

            while (true) {
                val current = _currentEventListener.get()
                when (current) {
                    null -> if (_currentEventListener.compareAndSet(null, value)) return
                    is CompositeAdminEventListener -> {
                        current.add(value)
                        return
                    }

                    else -> {
                        val composite = CompositeAdminEventListener(listOf(current, value))
                        if (_currentEventListener.compareAndSet(current, composite)) return
                    }
                }
            }
        }

    /** Timezone configuration for date/time handling */
    var timeZone: ZoneId = ZoneId.systemDefault()

    var defaultLanguage: String = "en"

    /** Lifetime duration for forms in milliseconds */
    var formsLifetime = 1_000 * 60L

    /** Registered dashboards (Thread-safe) */
    private val _dashboards = ConcurrentSkipListMap<String, DashboardEntry>()
    val dashboards: List<DashboardEntry>
        get() = _dashboards.values.toList()

    /**
     * Backward-compatible accessor for the primary dashboard.
     * @deprecated Use [registerDashboard] and [getPrimaryDashboard] instead.
     */
    @Deprecated(
        message = "Use registerDashboard() and getPrimaryDashboard() instead.",
        replaceWith = ReplaceWith("getPrimaryDashboard()"),
    )
    var dashboard: KtorAdminDashboard?
        get() = getPrimaryDashboard()?.dashboard
        set(value) {
            value?.let { registerDashboard(it) }
        }

    /** Maximum age for authentication sessions */
    var authenticationSessionMaxAge: Duration = 10.days

    /** TinyMCE editor configuration */
    var tinyMCEConfig = TinyMCEConfig.Companion.Basic

    /** Rate limit for requests per minute */
    var rateLimitPerMinutes = 30

    /** Flag to enable/disable CSV data download */
    var canDownloadDataAsCsv = false

    /** Flag to enable/disable PDF data download */
    var canDownloadDataAsPdf = false

    var adminPath = "admin"

    var loginPageMessage: String? = null

    /**
     * Strategy to use when deleting rows.
     */
    var fileDeleteStrategy: FileDeleteStrategy = FileDeleteStrategy.KEEP
        set(value) {
            require(value != FileDeleteStrategy.INHERIT) {
                "FileDeleteStrategy.INHERIT cannot be set directly. Use DELETE or KEEP."
            }
            field = value
        }

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

    /** Translators mappers (Thread-safe) */
    private val _translators =
        ConcurrentLinkedQueue<KtorAdminTranslator>(listOf(EnglishKtorAdminTranslator))
    val translators: List<KtorAdminTranslator>
        get() = _translators.toList()

    /** Previews (Thread-safe) */
    private val _previews = ConcurrentLinkedQueue<KtorAdminPreview>()
    val previews: List<KtorAdminPreview>
        get() = _previews.toList()

    /** Active template for rendering admin views */
    var template: AdminTemplate = DefaultAdminTemplate()

    var menuProvider: ((String?) -> List<Menu>)? = null

    /** Custom pages registered in the admin interface (Thread-safe) */
    private val _customPageEntries = ConcurrentSkipListMap<String, CustomPageEntry>()
    val customPageEntries: List<CustomPageEntry>
        get() = _customPageEntries.values.toList()

    /** Page size for autocomplete search results */
    var autocompletePageSize: Int = 20

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
    fun getPreview(key: String): KtorAdminPreview =
        previews.firstOrNull { it.key == key }
            ?: throw NoSuchElementException("No preview found with key '$key'.")

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

    fun registerTranslator(translator: KtorAdminTranslator) {
        if (_translators.any { it.languageCode == translator.languageCode }) {
            throw IllegalArgumentException("A translator with languageCode '${translator.languageCode}' is already registered.")
        }
        _translators.add(translator)
    }

    /**
     * Registers a DSL-defined custom page in the admin interface.
     * @param page The custom page to register.
     * @throws IllegalStateException if a page with the same path is already registered.
     */
    fun registerCustomPage(page: CustomPage) {
        registerCustomPageEntry(CustomPageEntry.from(page))
    }

    /**
     * Registers a class-based custom page in the admin interface.
     * @param page The custom admin page instance to register.
     * @throws IllegalStateException if a page with the same path is already registered.
     */
    fun registerCustomPage(page: CustomAdminPage) {
        registerCustomPageEntry(CustomPageEntry.from(page))
    }

    private fun registerCustomPageEntry(entry: CustomPageEntry) {
        if (_customPageEntries.containsKey(entry.path)) {
            throw IllegalStateException("A custom page with path '${entry.path}' is already registered.")
        }
        _customPageEntries[entry.path] = entry
    }

    /**
     * Retrieves a custom page entry by its path.
     * @param path The URL path segment of the custom page.
     * @return The custom page entry, or null if not found.
     */
    fun getCustomPage(path: String): CustomPageEntry? = _customPageEntries[path]

    /**
     * Registers a dashboard in the admin interface.
     *
     * @param dashboard The dashboard instance to register.
     * @throws IllegalStateException if a dashboard with the same path is already registered.
     * @throws IllegalStateException if more than one dashboard is marked as primary.
     */
    fun registerDashboard(dashboard: KtorAdminDashboard) {
        val entry = DashboardEntry.from(dashboard)

        if (_dashboards.containsKey(entry.path)) {
            throw IllegalStateException("A dashboard with path '${entry.path}' is already registered.")
        }

        if (entry.isPrimary && _dashboards.values.any { it.isPrimary }) {
            throw IllegalStateException("Only one dashboard can be marked as primary.")
        }

        _dashboards[entry.path] = entry
    }

    /**
     * Retrieves a dashboard entry by its path.
     * @param path The URL path segment of the dashboard.
     * @return The dashboard entry, or null if not found.
     */
    fun getDashboard(path: String): DashboardEntry? = _dashboards[path]

    /**
     * Returns the primary dashboard, or null if none registered.
     */
    fun getPrimaryDashboard(): DashboardEntry? = _dashboards.values.firstOrNull { it.isPrimary }

    /**
     * Returns all registered dashboards.
     */
    fun getAllDashboards(): List<DashboardEntry> = _dashboards.values.toList()

    fun getTranslator(languageCode: String?): KtorAdminTranslator =
        if (languageCode == null) {
            _translators.first { it.languageCode == defaultLanguage }
        } else {
            _translators.first { it.languageCode == languageCode }
        }
}
