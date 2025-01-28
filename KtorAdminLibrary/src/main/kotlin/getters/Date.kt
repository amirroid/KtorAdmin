package getters

import utils.Constants
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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


internal fun String.toInstant(): Instant? {
    return try {
        Instant.parse(this)
    } catch (e: DateTimeParseException) {
        null
    }
}