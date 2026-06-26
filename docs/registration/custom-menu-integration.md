---
description: Custom Menu Integration in KtorAdmin
---

# Custom Menu Integration

The Custom Menu feature allows adding additional menu items to predefined menus in the admin panel. This can be useful for linking external resources or providing quick access to related sections.

To define a custom menu, use the `provideMenu` function within the KtorAdmin configuration:

```kotlin
install(KtorAdmin){
    provideMenu { name ->
        if (name == "tasks") listOf(
            Menu(
                title = "Github", link = "https://github.com/Amirroid"
            )
        ) else emptyList()
    }
}
```

#### Function Parameters

* `name: String?` → Refers to the table name or collection. If the name is `null`, it refers to the dashboard.
* **Returns** → A list of `Menu` items to be added to the specified menu.

#### Menu Class Structure

Each menu item is defined using the `Menu` class:

```kotlin
class Menu(val title: String, val link: String)
```

* **title: String** → The display name of the menu item.
* **link: String** → The URL to which the menu item redirects.

#### Example

If the table name is "tasks" the above configuration will add a menu item titled "Github" that redirects to `https://github.com/Amirroid`.

#### Registering Custom Menus

Ensure that `provideMenu` is set within the `install(KtorAdmin)` block to properly register the custom menus.

By utilizing this feature, administrators can enhance navigation and accessibility within the admin panel.
