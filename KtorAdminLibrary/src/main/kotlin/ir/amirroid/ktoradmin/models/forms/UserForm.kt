package ir.amirroid.ktoradmin.models.forms

import io.ktor.http.*
import io.ktor.util.*
import ir.amirroid.ktoradmin.csrf.CSRF_TOKEN_FIELD_NAME

typealias UserForm = Map<String, String?>

internal fun Parameters.toUserForm(): UserForm = toMap().filter { it.key != CSRF_TOKEN_FIELD_NAME }.mapValues { it.value.firstOrNull() }
