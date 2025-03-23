package translator.locals.en

import translator.KtorAdminTranslator

internal object EnglishKtorAdminTranslator : KtorAdminTranslator() {
    override val languageCode: String
        get() = "en"

    override val translates: Map<String, String> = mapOf(
        Keys.DASHBOARD to "Dashboard", // done
        Keys.ITEMS to "{count} Items", // done
        Keys.CHANGE_THEME to "Change theme", // done
        Keys.DOWNLOAD_AS_CSV to "Download as CSV", // done
        Keys.DOWNLOAD_AS_PDF to "Download as PDF", // done
        Keys.PERFORM to "Perform", // done
        Keys.DELETE_SELECTED_ITEMS to "Delete selected items", // done
        Keys.ADD_NEW_ITEM to "Add a new {name}", // done
        Keys.UPDATE_ITEM to "Update {name}", // done
        Keys.RESET_ITEM to "Reset {name}", // done
        Keys.SUBMIT to "Submit", // done
        Keys.LINK to "Link", // done
        Keys.SELECTED_FILE to "Selected file: {name}", // done
        Keys.SELECT_A_FILE to "Select a file", // done
        Keys.CURRENT_FILE to "Current file", // done
        Keys.EMPTY to "Empty", // done
        Keys.SEARCH to "Search...", // done
        Keys.LOGIN to "Login", // done
        Keys.LOGIN to "Login", // done
        Keys.LOGOUT to "Logout", // done

        Keys.ERROR_NULL_FIELD to "The field cannot be null",
        Keys.ERROR_EMPTY_FIELD to "The field cannot be empty",
        Keys.ERROR_EMPTY_OR_NULL_REFERENCE to "The field cannot be empty or null when a reference is required.",
        Keys.ERROR_INVALID_VALUE to "The provided value is not valid.",
        Keys.ERROR_UNIQUE_FIELD to "The field must be unique",
        Keys.ERROR_MAX_LENGTH_EXCEEDED to "Value exceeds maximum length of {length}",
        Keys.ERROR_MIN_LENGTH_NOT_MET to "Value is shorter than minimum length of {length}",
        Keys.ERROR_REGEX_MISMATCH to "Value does not match the required pattern ({pattern})",
        Keys.ERROR_INVALID_INTEGER to "Value should be a valid integer",
        Keys.ERROR_INTEGER_MAX_EXCEEDED to "Value exceeds maximum count of {max}",
        Keys.ERROR_INTEGER_MIN_EXCEEDED to "Value is less than minimum count of {min}",
        Keys.ERROR_INVALID_UNSIGNED_INTEGER to "Value should be a valid unsigned integer",
        Keys.ERROR_UNSIGNED_INTEGER_MAX_EXCEEDED to "Value exceeds maximum count of {max}",
        Keys.ERROR_UNSIGNED_INTEGER_MIN_EXCEEDED to "Value is less than minimum count of {min}",
        Keys.ERROR_INVALID_SHORT to "Value should be a valid short",
        Keys.ERROR_SHORT_MAX_EXCEEDED to "Value exceeds maximum count of {max}",
        Keys.ERROR_SHORT_MIN_EXCEEDED to "Value is less than minimum count of {min}",
        Keys.ERROR_INVALID_UNSIGNED_SHORT to "Value should be a valid unsigned short",
        Keys.ERROR_UNSIGNED_SHORT_MAX_EXCEEDED to "Value exceeds maximum count of {max}",
        Keys.ERROR_UNSIGNED_SHORT_MIN_EXCEEDED to "Value is less than minimum count of {min}",
        Keys.ERROR_INVALID_LONG to "Value should be a valid long",
        Keys.ERROR_LONG_MAX_EXCEEDED to "Value exceeds maximum count of {max}",
        Keys.ERROR_LONG_MIN_EXCEEDED to "Value is less than minimum count of {min}",
        Keys.ERROR_INVALID_UNSIGNED_LONG to "Value should be a valid unsigned long",
        Keys.ERROR_UNSIGNED_LONG_MAX_EXCEEDED to "Value exceeds maximum count of {max}",
        Keys.ERROR_UNSIGNED_LONG_MIN_EXCEEDED to "Value is less than minimum count of {min}",
        Keys.ERROR_INVALID_DOUBLE to "Value should be a valid double",
        Keys.ERROR_DOUBLE_MAX_EXCEEDED to "Value exceeds maximum count of {max}",
        Keys.ERROR_DOUBLE_MIN_EXCEEDED to "Value is less than minimum count of {min}",
        Keys.ERROR_INVALID_FLOAT to "Value should be a valid float",
        Keys.ERROR_FLOAT_MAX_EXCEEDED to "Value exceeds maximum count of {max}",
        Keys.ERROR_FLOAT_MIN_EXCEEDED to "Value is less than minimum count of {min}",
        Keys.ERROR_INVALID_BIG_DECIMAL to "Value should be a valid big decimal",

        Keys.ERROR_INVALID_CHAR to "Value contains invalid characters",
        Keys.ERROR_INVALID_BOOLEAN to "Value should be either true or false",
        Keys.ERROR_INVALID_ENUMERATION to "Value should be one of {values}",
        Keys.ERROR_INVALID_DATE to "Value should be a valid date",
        Keys.ERROR_DATE_BEFORE_MIN to "Date should not be before {date}",
        Keys.ERROR_DATE_AFTER_MAX to "Date should not be after {date}",
        Keys.ERROR_INVALID_DURATION to "Value should be a valid duration format.",
        Keys.ERROR_INVALID_DATETIME to "Value should be a valid datetime. ({format})",
        Keys.ERROR_DATETIME_BEFORE_MIN to "Datetime should not be before {datetime}",
        Keys.ERROR_DATETIME_AFTER_MAX to "Datetime should not be after {datetime}",
        Keys.ERROR_FILE_SIZE_EXCEEDED to "File size exceeds {size}",
        Keys.ERROR_INVALID_MIME_TYPE to "Invalid MIME type for file {file}. Allowed types are {types}",
        Keys.ERROR_INVALID_DECIMAL128 to "Value should be a valid Decimal128 number",
        Keys.ERROR_DECIMAL128_MAX_EXCEEDED to "Value exceeds {max}",
        Keys.ERROR_DECIMAL128_MIN_EXCEEDED to "Value is less than {min}",
        Keys.ERROR_BIG_DECIMAL_MAX_EXCEEDED to "Value exceeds {max}",
        Keys.ERROR_BIG_DECIMAL_MIN_EXCEEDED to "Value is less than {min}"
    )
}