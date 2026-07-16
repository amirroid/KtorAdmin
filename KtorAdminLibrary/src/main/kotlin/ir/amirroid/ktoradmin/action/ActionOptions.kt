package ir.amirroid.ktoradmin.action

/**
 * Configurable options for how an action appears in the admin panel's edit/upsert pages.
 *
 * @property showInEditPage Whether to display this action button on the edit page. Defaults to true.
 * @property icon Optional inline SVG string to display as the action button icon. If null, no icon is shown.
 * @property style Optional CSS style string to apply to the action button (e.g. custom color, background).
 */
data class ActionOptions(
    val showInEditPage: Boolean = true,
    val icon: String? = null,
    val style: String? = null,
)
