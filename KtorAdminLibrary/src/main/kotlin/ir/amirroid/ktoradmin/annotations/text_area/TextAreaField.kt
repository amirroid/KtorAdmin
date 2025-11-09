package ir.amirroid.ktoradmin.annotations.text_area

/**
 * Marks a property as a text area field, typically used for multi-line text input.
 *
 * This annotation is supported for both MongoDB and SQL databases.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class TextAreaField
