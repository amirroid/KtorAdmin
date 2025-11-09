package ir.amirroid.ktoradmin.formatters


fun String.extractTextInCurlyBraces(): List<String> {
    val regex = "\\{(.*?)}".toRegex()
    return regex.findAll(this).map { it.groupValues[1] }.toList()
}

fun populateTemplate(template: String, values: Map<String, String?>): String {
    val regexPattern = "\\{(.*?)}".toRegex()
    return regexPattern.replace(template) { matchResult ->
        val key = matchResult.groupValues[1]
        values[key] ?: matchResult.value
    }
}