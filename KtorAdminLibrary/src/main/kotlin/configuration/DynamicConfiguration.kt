package configuration

import listener.AdminEventListener

internal object DynamicConfiguration {
    var maxItemsInPage: Int = 20
    var currentEventListener: AdminEventListener? = null

    fun registerEventListener(listener: AdminEventListener) {
        if (currentEventListener != null) {
            throw IllegalStateException("An event listener is already registered. Please unregister it before registering a new one.")
        }
        currentEventListener = listener
    }
}