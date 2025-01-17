package models

data class Limit(
    val maxLength: Int? = null,
    val minLength: Int? = null,
    val regexPattern: String? = null
)


fun Limit.toFormattedString(): String {
    return """
        |Limit(
        |       maxLength = $maxLength,
        |       minLength = $minLength,
        |       regexPattern = ${regexPattern?.let { "\"$it\"" }}
        |   )
    """.trimMargin("|")
}
