package flash

import configuration.DynamicConfiguration
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.AttributeKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import response.ErrorResponse

internal const val REQUEST_ID = "Request-Id"
internal const val REQUEST_ID_FORM = "requestId"

internal suspend fun ApplicationCall.setFlashSessionsAndRedirect(
    requestId: String?,
    errors: List<ErrorResponse>,
    values: Map<String, String?>
) {
    val referer = request.headers["Referer"]
    println("REQUEST ID $requestId")
    if (referer != null && requestId != null) {
        response.cookies.append(
            "${requestId}-data",
            Json.encodeToString(values),
            maxAge = DynamicConfiguration.formsLifetime,
            httpOnly = true
        )
        response.cookies.append(
            "${requestId}-errors",
            Json.encodeToString(errors),
            maxAge = DynamicConfiguration.formsLifetime,
            httpOnly = true
        )
    }
    requestId?.let {
        response.cookies.append(
            name = REQUEST_ID, it,
            maxAge = DynamicConfiguration.formsLifetime,
            httpOnly = true
        )
    }
    respondRedirect(referer ?: "/admin")
}


internal fun ApplicationCall.getFlashDataAndClear(requestId: String = getRequestId()): Pair<Map<String, String?>?, List<ErrorResponse>?> {
    var values: Map<String, String?>? = null
    var errors: List<ErrorResponse>? = null
    request.cookies["${requestId}-data"]?.let {
        response.cookies.append("${requestId}-data", "", maxAge = 0)
        values = Json.decodeFromString<Map<String, String?>>(it)
    }
    request.cookies["${requestId}-errors"]?.let {
        response.cookies.append("${requestId}-errors", "", maxAge = 0)
        errors = Json.decodeFromString(it)
    }
    request.cookies[REQUEST_ID]?.let { response.cookies.append(REQUEST_ID, "", maxAge = 0) }
    return values to errors
}

internal fun ApplicationCall.getRequestId(): String {
    return request.cookies[REQUEST_ID] ?: KtorFlashHelper.generateId()
}