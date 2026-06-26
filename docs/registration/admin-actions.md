---
description: Admin Actions in KtorAdmin
---

# Admin Actions

In KtorAdmin, the following default actions are enabled for all tables and collections:

* `Action.ADD` → Allows adding new entries.
* `Action.EDIT` → Enables editing existing entries.
* `Action.DELETE` → Permits deleting entries.

However, you can customize which actions to keep using the `@AdminActions` annotation.

#### **Restricting Default Actions**

By default, all three actions are active, but you can disable specific actions by defining only the ones you want:

```kotlin
@AdminActions(
    actions = [Action.ADD, Action.DELETE]
)
object Tasks : Table("tasks")
```

In this example, the Edit action is disabled, leaving only Add and Delete.

***

#### **Custom Actions**

To define additional actions beyond the default ones, you need to create a custom action. Custom actions are identified by a unique `key` and executed based on selected database entries.

```kotlin
class MyCustomAction : CustomAdminAction {
    override var key: String = "delete"
    override val displayText: String
        get() = "Delete all"

    override suspend fun performAction(name: String, selectedIds: List<String>) {
        // Implementation of the action
    }
}
```

**Key Properties of Custom Actions:**

* **`key`** → A unique identifier for the action.
* **`displayText`** → The label shown in the admin panel.
* **`performAction`** → Defines the action logic, which runs on the selected database entries.

***

#### **Registering Custom Actions**

Once a custom action is defined, it must be registered in the KtorAdmin plugin. There are two ways to do this:

**1. Register for All Tables and Collections**

```kotlin
install(KtorAdmin) {
    registerCustomAdminActionForAll(MyCustomAction())
}
```

This applies the action globally to all tables and collections.

**2. Register for Specific Tables or Collections**

First, register the action in the plugin:

```kotlin
install(KtorAdmin) {
    registerCustomAdminAction(MyCustomAction())
}
```

Then, explicitly assign it to a table:

```kotlin
@AdminActions(
    actions = [Action.ADD, Action.DELETE],
    customActions = ["delete"]
)
object Tasks : Table("tasks")
```

Here, the table `Tasks` includes the custom action `delete` along with the default Add and Delete actions.

***

By using these methods, you can fine-tune which actions are available in your admin panel, ensuring greater control over your database operations.
