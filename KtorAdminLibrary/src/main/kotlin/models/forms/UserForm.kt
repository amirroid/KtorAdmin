package models.forms

import io.ktor.http.*
import io.ktor.util.*


typealias UserForm = Map<String, String?>

internal fun Parameters.toUserForm(): UserForm {
    return toMap().mapValues { it.value.firstOrNull()?.toString() }
}