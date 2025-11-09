package getters

import configuration.DynamicConfiguration
import io.ktor.server.util.toZonedDateTime
import utils.Constants
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


/**
 * Extension function to convert a String to LocalDate.
 */
internal fun String.toLocalDate(): LocalDate? {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        LocalDate.parse(this, formatter)
    } catch (e: DateTimeParseException) {
        null
    }
}

/**
 * Extension function to convert a String to LocalDateTime.
 */
internal fun String.toLocalDateTime(): LocalDateTime? {
    val formatter = DateTimeFormatter.ofPattern(Constants.LOCAL_DATETIME_FORMAT)
    return try {
        LocalDateTime.parse(this, formatter)
    } catch (e: DateTimeParseException) {
        null
    }
}

/**
 * Safely converts a String to [OffsetDateTime] using the defined date-time format.
 * Returns `null` if parsing fails.
 */
internal fun String.toOffsetDateTime(): OffsetDateTime? {
    return runCatching {
        val formatter = DateTimeFormatter.ofPattern(Constants.LOCAL_DATETIME_FORMAT)
        LocalDateTime.parse(this, formatter)
            .atZone(DynamicConfiguration.timeZone)
            .toOffsetDateTime()
    }.getOrNull()
}


internal fun String.toInstant(): Instant? {
    return try {
        Instant.parse(this)
    } catch (e: DateTimeParseException) {
        null
    }
}