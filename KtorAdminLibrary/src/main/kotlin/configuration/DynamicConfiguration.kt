package configuration

import listener.AdminEventListener
import models.forms.LoginFiled

internal object DynamicConfiguration {
    var maxItemsInPage: Int = 20
    var loginFields: List<LoginFiled> = emptyList()
    var currentEventListener: AdminEventListener? = null

    fun registerEventListener(listener: AdminEventListener) {
        if (currentEventListener != null) {
            throw IllegalStateException("An event listener is already registered. Please unregister it before registering a new one.")
        }
        currentEventListener = listener
    }
}