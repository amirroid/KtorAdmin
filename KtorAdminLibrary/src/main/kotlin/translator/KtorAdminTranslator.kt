package translator

abstract class KtorAdminTranslator {
    abstract val languageCode: String
    abstract val languageName: String
    abstract val translates: Map<String, String>
    open val layoutDirection: String = "ltr"

    object Keys {
        const val DASHBOARD = "dashboard"
        const val ITEMS = "items"
        const val CHANGE_THEME = "change_theme"
        const val DOWNLOAD_AS_CSV = "download_as_csv"
        const val DOWNLOAD_AS_PDF = "download_as_pdf"
        const val PERFORM = "perform"
        const val DELETE_SELECTED_ITEMS = "delete_selected_items"
        const val ADD_NEW_ITEM = "add_new_item"
        const val UPDATE_ITEM = "update_item"
        const val RESET_ITEM = "reset_item"
        const val SUBMIT = "submit"
        const val LINK = "link"
        const val SELECTED_FILE = "selected_file"
        const val SELECT_A_FILE = "select_file"
        const val CURRENT_FILE = "current_file"
        const val EMPTY = "empty"
        const val SEARCH = "search"
        const val LOGIN = "login"
        const val LOGOUT = "logout"
        const val LANGUAGES = "languages"
        const val FILTER = "filter"

        const val SELECT_RANGE_OF = "select_range_of"
        const val SELECT_AN_ITEM_FOR = "select_item_for"
        const val SELECT_AN_ITEM = "select_item"
        const val SELECT_AN_ACTION = "select_action"
        const val SELECT_A_BOOLEAN = "select_boolean"

        const val ERROR_NULL_FIELD = "error_null_field"
        const val ERROR_EMPTY_FIELD = "error_empty_field"
        const val ERROR_EMPTY_OR_NULL_REFERENCE = "error_empty_or_null_reference"
        const val ERROR_INVALID_VALUE = "error_invalid_value"
        const val ERROR_UNIQUE_FIELD = "error_unique_field"
        const val ERROR_MAX_LENGTH_EXCEEDED = "error_max_length_exceeded"
        const val ERROR_MIN_LENGTH_NOT_MET = "error_min_length_not_met"
        const val ERROR_REGEX_MISMATCH = "error_regex_mismatch"
        const val ERROR_INVALID_INTEGER = "error_invalid_integer"
        const val ERROR_INTEGER_MAX_EXCEEDED = "error_integer_max_exceeded"
        const val ERROR_INTEGER_MIN_EXCEEDED = "error_integer_min_exceeded"
        const val ERROR_INVALID_UNSIGNED_INTEGER = "error_invalid_unsigned_integer"
        const val ERROR_UNSIGNED_INTEGER_MAX_EXCEEDED = "error_unsigned_integer_max_exceeded"
        const val ERROR_UNSIGNED_INTEGER_MIN_EXCEEDED = "error_unsigned_integer_min_exceeded"
        const val ERROR_INVALID_SHORT = "error_invalid_short"
        const val ERROR_SHORT_MAX_EXCEEDED = "error_short_max_exceeded"
        const val ERROR_SHORT_MIN_EXCEEDED = "error_short_min_exceeded"
        const val ERROR_INVALID_UNSIGNED_SHORT = "error_invalid_unsigned_short"
        const val ERROR_UNSIGNED_SHORT_MAX_EXCEEDED = "error_unsigned_short_max_exceeded"
        const val ERROR_UNSIGNED_SHORT_MIN_EXCEEDED = "error_unsigned_short_min_exceeded"
        const val ERROR_INVALID_LONG = "error_invalid_long"
        const val ERROR_LONG_MAX_EXCEEDED = "error_long_max_exceeded"
        const val ERROR_LONG_MIN_EXCEEDED = "error_long_min_exceeded"
        const val ERROR_INVALID_UNSIGNED_LONG = "error_invalid_unsigned_long"
        const val ERROR_UNSIGNED_LONG_MAX_EXCEEDED = "error_unsigned_long_max_exceeded"
        const val ERROR_UNSIGNED_LONG_MIN_EXCEEDED = "error_unsigned_long_min_exceeded"
        const val ERROR_INVALID_DOUBLE = "error_invalid_double"
        const val ERROR_DOUBLE_MAX_EXCEEDED = "error_double_max_exceeded"
        const val ERROR_DOUBLE_MIN_EXCEEDED = "error_double_min_exceeded"
        const val ERROR_INVALID_FLOAT = "error_invalid_float"
        const val ERROR_FLOAT_MAX_EXCEEDED = "error_float_max_exceeded"
        const val ERROR_FLOAT_MIN_EXCEEDED = "error_float_min_exceeded"
        const val ERROR_INVALID_BIG_DECIMAL = "error_invalid_big_decimal"


        const val ERROR_INVALID_CHAR = "error_invalid_char"
        const val ERROR_INVALID_BOOLEAN = "error_invalid_boolean"
        const val ERROR_INVALID_ENUMERATION = "error_invalid_enumeration"
        const val ERROR_INVALID_DATE = "error_invalid_date"
        const val ERROR_DATE_BEFORE_MIN = "error_date_before_min"
        const val ERROR_DATE_AFTER_MAX = "error_date_after_max"
        const val ERROR_INVALID_DURATION = "error_invalid_duration"
        const val ERROR_INVALID_DATETIME = "error_invalid_datetime"
        const val ERROR_DATETIME_BEFORE_MIN = "error_datetime_before_min"
        const val ERROR_DATETIME_AFTER_MAX = "error_datetime_after_max"
        const val ERROR_FILE_SIZE_EXCEEDED = "error_file_size_exceeded"
        const val ERROR_INVALID_MIME_TYPE = "error_invalid_mime_type"
        const val ERROR_INVALID_DECIMAL128 = "error_invalid_decimal128"
        const val ERROR_DECIMAL128_MAX_EXCEEDED = "error_decimal128_max_exceeded"
        const val ERROR_DECIMAL128_MIN_EXCEEDED = "error_decimal128_min_exceeded"
        const val ERROR_BIG_DECIMAL_MAX_EXCEEDED = "error_big_decimal_max_exceeded"
        const val ERROR_BIG_DECIMAL_MIN_EXCEEDED = "error_big_decimal_min_exceeded"
    }

    internal fun translate(key: String, values: Map<String, String> = emptyMap()) = translates[key]?.let {
        var newText = it
        values.forEach { (itemKey, value) ->
            newText = newText.replace("{$itemKey}", value)
        }
        newText
    } ?: "undefined translated key"
}