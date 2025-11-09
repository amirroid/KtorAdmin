package ir.amirroid.ktoradmin.annotations.status

/**
 * An annotation to define the status styling for database columns or fields
 * that use an enumeration. The styling is applied based on the index of the enum values.
 *
 * @param color The color styles corresponding to the enum indices.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class StatusStyle(
    vararg val color: String
)