package ir.amirroid.ktoradmin.models

data class Limit(
    val maxLength: Int = Int.MAX_VALUE,
    val minLength: Int = 0,
    val regexPattern: String? = null,
    val maxCount: Double = Double.MAX_VALUE,
    val minCount: Double = Double.MIN_VALUE,
    val maxBytes: Long = Long.MAX_VALUE,
    val minDateRelativeToNow: Long = Long.MAX_VALUE,
    val maxDateRelativeToNow: Long = Long.MAX_VALUE,
    val allowedMimeTypes: List<String>? = null,
)


fun Limit.toFormattedString(): String {
    return """
        |Limit(
        |      maxLength = $maxLength,
        |      minLength = $minLength,
        |      regexPattern = ${regexPattern?.let { "\"$it\"" } ?: "null"},
        |      maxCount = $maxCount,
        |      minCount = $minCount,
        |      maxBytes = $maxBytes,
        |      minDateRelativeToNow = $minDateRelativeToNow,
        |      maxDateRelativeToNow = $maxDateRelativeToNow,
        |      allowedMimeTypes = ${
        allowedMimeTypes?.joinToString(
            prefix = "listOf(",
            postfix = ")",
            separator = ", "
        ) { "\"${it}\"" } ?: "null"
    },
        |   )
    """.trimMargin("|")
}