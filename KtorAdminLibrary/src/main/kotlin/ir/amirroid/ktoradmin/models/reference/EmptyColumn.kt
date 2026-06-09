package ir.amirroid.ktoradmin.models.reference

/**
 * Placeholder column used when a property needs to participate in annotations
 * or metadata processing, but does not represent a real database column.
 *
 * This is useful for relationship definitions and other virtual fields that
 * should be discovered by reflection-based processors while being excluded
 * from table schema generation.
 *
 * Example:
 * ```
 * @ManyToManyReferences(
 *     referenceTable = "users",
 *     junctionTable = "tasks_users",
 *     sourceColumn = "task_id",
 *     targetColumn = "user_id"
 * )
 * val users = EmptyColumn()
 * ```
 */
class EmptyColumn
