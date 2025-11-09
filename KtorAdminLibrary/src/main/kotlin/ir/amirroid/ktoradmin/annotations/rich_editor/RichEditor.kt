package ir.amirroid.ktoradmin.annotations.rich_editor

/**
 * Annotation to mark a property as a rich text editor field.
 *
 * This annotation should be used on fields or database columns that need
 * to be displayed as a rich text editor in the admin panel.
 *
 * Example usage:
 * ```
 * @RichEditor
 * var content: String
 * ```
 *
 * @Target(AnnotationTarget.PROPERTY) - Can be applied to properties.
 * @Retention(AnnotationRetention.SOURCE) - This annotation is only used at the source level and is not retained in the compiled class.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class RichEditor