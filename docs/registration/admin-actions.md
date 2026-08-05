---
description: Admin Actions in KtorAdmin
---

# Admin Actions

KtorAdmin provides a flexible action system for managing operations on tables and collections.

By default, the following actions are available:

* `Action.ADD` → Allows creating new entries.
* `Action.EDIT` → Allows editing existing entries.
* `Action.DELETE` → Allows deleting entries.

You can customize enabled actions using the `@AdminActions` annotation.

---

## Restricting Default Actions

By default, all built-in actions are enabled. You can limit available actions by specifying only the required ones:

```kotlin
@AdminActions(
    actions = [Action.ADD, Action.DELETE]
)
object Tasks : Table("tasks")
```

In this example, the Edit action is disabled, leaving only Add and Delete actions available.

---

# Custom Actions

Custom actions allow you to add additional operations to KtorAdmin beyond the built-in actions.

Custom actions must implement the `KtorAdminAction` interface.

Each action requires a unique `key` and defines its execution logic through `performAction`.

```kotlin
class MyCustomAction : KtorAdminAction {
    override var key: String = "archive"

    override val displayText: String
        get() = "Archive"

    override suspend fun performAction(
        name: String,
        selectedIds: List<String>,
    ) {
        // Implementation of the action
    }
}
```

## KtorAdminAction Properties

### `key`

A unique identifier for the action.

This key is used when registering and assigning custom actions to tables or collections.

### `displayText`

The label displayed in the admin panel for the action.

### `performAction`

Defines the action logic that runs on selected database entries.

The `selectedIds` parameter contains the IDs of the selected rows.

### `options`

Controls how the action button is displayed in the admin panel.

You can customize the action visibility, icon, and styling by overriding the `options` property.

```kotlin
override val options = ActionOptions(
    showInEditPage = true,
    icon = """
        <svg viewBox="0 0 24 24">
            ...
        </svg>
    """,
    style = "color: red;",
)
```

#### `showInEditPage`

Controls whether the action button is displayed on the edit/upsert page.

Defaults to `true`.

```kotlin
override val options = ActionOptions(
    showInEditPage = false,
)
```

When disabled, the action will not appear on edit/upsert pages.

#### `icon`

Optional inline SVG content used as the action button icon.

If `null`, no icon is displayed.

```kotlin
override val options = ActionOptions(
    icon = """
        <svg viewBox="0 0 24 24">
            ...
        </svg>
    """,
)
```

#### `style`

Optional CSS style string applied directly to the action button.

This can be used to customize the appearance of the action button.

```kotlin
override val options = ActionOptions(
    style = "color: red; background: #fee2e2;",
)
```

### `allowEditPageRedirect`

Controls whether the action can redirect back to an edit/upsert page after execution.

By default, `allowEditPageRedirect` is `false`.

If an action is executed from an edit page and this value is `false`, KtorAdmin redirects the user back to the resource list page instead.

```kotlin
override val allowEditPageRedirect = true
```

When enabled, the user can return to the edit page after the action completes.

---

# Registering Custom Actions

After creating a custom action, it must be registered in the KtorAdmin plugin.

There are two ways to register custom actions.

## Register for All Tables and Collections

To make an action available for all resources:

```kotlin
install(KtorAdmin) {
    registerCustomAdminActionForAll(MyCustomAction())
}
```

This action will automatically be available for all tables and collections.

---

## Register for Specific Tables or Collections

First, register the action:

```kotlin
install(KtorAdmin) {
    registerCustomAdminAction(MyCustomAction())
}
```

Then assign it to specific resources:

```kotlin
@AdminActions(
    actions = [Action.ADD, Action.DELETE],
    customActions = ["archive"]
)
object Tasks : Table("tasks")
```

The `Tasks` table now includes the custom `archive` action along with the selected default actions.

---

Using `KtorAdminAction`, you can create reusable and configurable actions while keeping full control over execution logic and admin panel behavior.
