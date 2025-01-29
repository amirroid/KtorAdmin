package action


/**
 * Interface for defining custom administrative actions that can be performed on a database table or collection.
 *
 * @property key A unique identifier for the action.
 */
interface CustomAdminAction {
    var key: String

    /**
     * Gets the human-readable display text for this action.
     */
    val displayText: String

    /**
     * Executes the action on the specified table or collection.
     *
     * @param name The name of the table or collection in the database.
     * @param selectedIds A list of IDs representing the selected entries.
     */
    suspend fun performAction(
        name: String,
        selectedIds: List<String>,
    )
}
