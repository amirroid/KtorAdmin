package response

data class ErrorResponse(
    val field: String,
    val messages: List<String>
)


fun List<ErrorResponse>.toMap() = associateBy {
    it.field
}