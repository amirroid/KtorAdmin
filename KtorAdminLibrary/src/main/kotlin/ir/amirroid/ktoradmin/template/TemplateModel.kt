package ir.amirroid.ktoradmin.template

/**
 * A flexible data model that carries all view-specific data to the template.
 *
 * The template implementation reads whichever keys it needs from [data].
 * This keeps the interface decoupled from any specific view's shape.
 */
data class TemplateModel(
    val data: Map<String, Any?> = emptyMap(),
) {
    operator fun get(key: String): Any? = data[key]

    companion object {
        fun of(vararg pairs: Pair<String, Any?>) = TemplateModel(mapOf(*pairs))
    }
}
