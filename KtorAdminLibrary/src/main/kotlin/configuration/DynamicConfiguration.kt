package configuration

import listener.AdminEventListener
import models.forms.LoginFiled
import java.time.ZoneId

internal object DynamicConfiguration {
    var maxItemsInPage: Int = 20
    var loginFields: List<LoginFiled> = emptyList()
    var currentEventListener: AdminEventListener? = null
    var cryptoPassword: String? = null

    var timeZone: ZoneId = ZoneId.systemDefault()

    fun registerEventListener(listener: AdminEventListener) {
        if (currentEventListener != null) {
            throw IllegalStateException("An event listener is already registered. Please unregister it before registering a new one.")
        }
        currentEventListener = listener
    }
}