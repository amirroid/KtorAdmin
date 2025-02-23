package models.filters

enum class FilterTypes {
    ENUMERATION,
    DATE,
    DATETIME,
    REFERENCE,
    BOOLEAN,
}

data class FiltersData(
    val paramName: String,
    val verboseName: String,
    val type: FilterTypes,
    val values: Any? = null
)