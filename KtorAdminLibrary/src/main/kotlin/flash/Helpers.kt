package flash

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.AttributeKey
import response.ErrorResponse

suspend fun ApplicationCall.setFlashSessionsAndRedirect(
    requestId: String,
    errors: List<ErrorResponse>,
    values: Map<String, String?>
) {
    val referer = request.headers["Referer"]
    if (referer != null) {
        attributes.put(AttributeKey("${requestId}-data"), values)
        attributes.put(AttributeKey("${requestId}-errors"), errors)
    }
    response.headers.append("requestId", requestId)
    respondRedirect(referer ?: "/admin")
}


fun ApplicationCall.getFlashDataAndClear(): Pair<Map<String, String?>?, List<ErrorResponse>?> {
    val requestId = request.headers["requestId"]
    var values: Map<String, String?>? = null
    var errors: List<ErrorResponse>? = null
    if (attributes.contains(AttributeKey<Map<String, String?>>("${requestId}-data"))) {
        values = attributes[AttributeKey<Map<String, String?>>("${requestId}-data")]
    }
    if (attributes.contains(AttributeKey<List<ErrorResponse>>("${requestId}-errors"))) {
        errors = attributes[AttributeKey<List<ErrorResponse>>("${requestId}-errors")]
    }
    return values to errors
}