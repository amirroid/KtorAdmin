package ir.amirroid.ktoradmin.logger

import org.slf4j.LoggerFactory

internal object KtorAdminLogger {
    private val logger = LoggerFactory.getLogger("KtorAdmin")

    fun debug(message: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(message())
        }
    }

    fun info(message: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(message())
        }
    }

    fun warn(
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (throwable != null) {
            logger.warn(message(), throwable)
        } else {
            logger.warn(message())
        }
    }

    fun error(
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (throwable != null) {
            logger.error(message(), throwable)
        } else {
            logger.error(message())
        }
    }
}
