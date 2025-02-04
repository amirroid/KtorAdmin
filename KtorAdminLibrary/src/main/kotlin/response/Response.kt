package response


internal sealed class Response<out D> {
    data class Success<out D>(val data: D) : Response<D>()
    data class Error(val errors: List<ErrorResponse>, val values: Map<String, String?>, val requestId: String? = null) :
        Response<Nothing>()

    data object InvalidRequest : Response<Nothing>()
}

internal inline fun <D> Response<D>.onSuccess(action: (D) -> Unit): Response<D> {
    return when (this) {
        is Response.Success -> {
            action(data)
            this
        }

        else -> this
    }
}

internal inline fun <D> Response<D>.onError(action: (List<ErrorResponse>, values: Map<String, String?>) -> Unit): Response<D> {
    return when (this) {
        is Response.Error -> {
            action(errors, values)
            this
        }

        else -> this
    }
}

internal inline fun <D> Response<D>.onError(action: (requestId: String?, List<ErrorResponse>, values: Map<String, String?>) -> Unit): Response<D> {
    return when (this) {
        is Response.Error -> {
            action(requestId, errors, values)
            this
        }

        else -> this
    }
}

internal inline fun <D> Response<D>.onInvalidateRequest(action: () -> Unit): Response<D> {
    return when (this) {
        is Response.InvalidRequest -> {
            action()
            this
        }

        else -> this
    }
}