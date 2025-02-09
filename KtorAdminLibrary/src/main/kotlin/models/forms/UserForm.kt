package models.forms

import csrf.CSRF_TOKEN_FIELD_NAME
import io.ktor.http.*
import io.ktor.util.*


typealias UserForm = Map<String, String?>

internal fun Parameters.toUserForm(): UserForm {
    return toMap().filter { it.key != CSRF_TOKEN_FIELD_NAME }.mapValues { it.value.firstOrNull()?.toString() }
}