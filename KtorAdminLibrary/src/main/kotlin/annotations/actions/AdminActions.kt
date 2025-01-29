package annotations.actions

import models.actions.Action

/** Defines admin privileges with a set of default and custom actions.
 *  @property actions The predefined admin actions (ADD, DELETE, EDIT by default).
 *  @property customActions Keys representing additional custom actions.
 *  */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AdminActions(
    val actions: Array<Action> = [Action.ADD, Action.DELETE, Action.EDIT],
    val customActions: Array<String> = []
)