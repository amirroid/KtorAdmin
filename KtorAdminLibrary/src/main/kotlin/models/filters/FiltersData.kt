package models.filters

enum class FilterTypes {
    ENUMERATION,
    DATE,
    DATETIME,
    REFERENCE,
}

data class FiltersData(
    val paramName: String,
    val type: FilterTypes,
    val values: Any? = null
)
