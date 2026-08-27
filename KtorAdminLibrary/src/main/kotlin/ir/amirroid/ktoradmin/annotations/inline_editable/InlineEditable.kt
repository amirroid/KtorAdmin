package ir.amirroid.ktoradmin.annotations.inline_editable

/**
 * Enables inline editing for a column or field directly in the list table view.
 *
 * When applied to a simple property (such as String, Char, numeric types, or Boolean),
 * the field can be edited directly without navigating to the detail edit page:
 * - **Text and Numbers**: Click the cell to edit inline, press Enter or blur to save, Escape to cancel.
 * - **Boolean**: Rendered as an interactive checkbox that updates immediately on click.
 *
 * Note: Complex or sensitive fields (such as read-only, foreign keys, uploads, rich editors,
 * confirmation fields, and auto dates) are excluded from inline editing even if annotated.
 *
 * **Usage Example:**
 * ```kotlin
 * @InlineEditable
 * val name = varchar("name", 150)
 *
 * @InlineEditable
 * val checked = bool("checked").default(true)
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class InlineEditable
