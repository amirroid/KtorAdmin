package annotations.display


/**
 * An annotation to specify the format for displaying an object as a string.
 *
 * @param format The string format to use for display. Use placeholders like {columnName} to include columns.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DisplayFormat(val format: String)