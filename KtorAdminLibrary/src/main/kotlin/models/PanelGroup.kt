package models

import panels.AdminPanel

internal class PanelGroup(
    val group: String,
    val panels: List<AdminPanel>
)

internal fun List<AdminPanel>.toTableGroups() =
    filter { it.isShowInAdminPanel() }.groupBy { it.getGroupName() }.map { (groupName, panels) ->
        PanelGroup(
            group = groupName?.replaceFirstChar { it.uppercaseChar() } ?: "Default",
            panels = panels
        )
    }.sortedByDescending { it.group == "Default" }