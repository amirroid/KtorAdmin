---
description: >-
  Create standalone custom pages with fully custom UI that live alongside your
  CRUD resources in the admin panel. Supports nested URL paths.
---

# Custom Pages

Custom pages allow you to create **standalone pages** in the admin interface that are not tied to any CRUD resource or database entity. They appear alongside your resources in the sidebar navigation and can render fully custom UI.

Custom pages support **nested URL paths** (e.g., `settings/theme`, `system/logs/viewer`), letting you organize pages hierarchically.

## Class-Based Approach

Create a class extending `CustomAdminPage`:

```kotlin
class SettingsPage : CustomAdminPage() {
    override val path = "settings"
    override val title = "Settings"
    override val description = "Application settings"
    override val icon = "/static/images/settings.svg"
    override val groupName = "Management"
    override val order = 1

    override suspend fun content(call: ApplicationCall): String {
        return """
            <div style="max-width:700px;">
                <h2>Application Settings</h2>
                <p>Configure your application here.</p>
            </div>
        """.trimIndent()
    }
}
```

### Properties

| Property | Type | Required | Description |
|---|---|---|---|
| `path` | `String` | Yes | URL path segment under `resources/`. Supports nested paths like `"settings/theme"`. |
| `title` | `String` | Yes | Display title shown in the sidebar and page header. |
| `description` | `String` | No | Optional description for documentation/metadata. |
| `icon` | `String` | No | SVG icon path for sidebar display (e.g., `"/static/images/settings.svg"`). |
| `groupName` | `String` | No | Sidebar group name. Pages are grouped together in the sidebar. |
| `order` | `Int` | No | Ordering position within the group (lower values appear first). Default: `0`. |
| `visible` | `Boolean` | No | Whether the page is visible in the sidebar. Default: `true`. |
| `permissions` | `List<String>` | No | List of required roles/permissions for access. |

### Methods

| Method | Description |
|---|---|
| `content(call: ApplicationCall): String` | Returns the HTML content for this page. Content is automatically wrapped in the admin shell. |
| `render(call: ApplicationCall): String` | Override to take full control of the entire page rendering, bypassing the admin shell. |

### Registration

```kotlin
install(KtorAdmin) {
    customPage(SettingsPage())
}
```

## DSL Approach

Use the `customPage` builder function for quick inline definitions:

```kotlin
install(KtorAdmin) {
    customPage("about") {
        title = "About"
        description = "About this application"
        icon = "/static/images/info.svg"
        groupName = "Support"
        order = 2

        render {
            """
                <div style="max-width:500px;">
                    <h2>About</h2>
                    <p>Built with KtorAdmin</p>
                </div>
            """.trimIndent()
        }
    }
}
```

### Builder Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Capitalized last path segment | Display title in sidebar and header. |
| `description` | `String?` | `null` | Optional description. |
| `icon` | `String?` | `null` | SVG icon path for sidebar. |
| `groupName` | `String?` | `null` | Sidebar group name. |
| `order` | `Int` | `0` | Sort order within the group. |
| `visible` | `Boolean` | `true` | Whether shown in sidebar. |
| `permissions` | `List<String>?` | `null` | Required roles for access. |

## Nested URLs

Custom pages support hierarchical path segments. This lets you organize related pages under a common prefix:

```kotlin
class ThemeSettingsPage : CustomAdminPage() {
    override val path = "settings/theme"
    override val title = "Theme Settings"
    // ...
}

class ProfileSettingsPage : CustomAdminPage() {
    override val path = "settings/profile"
    override val title = "Profile Settings"
    // ...
}

class LogsViewerPage : CustomAdminPage() {
    override val path = "system/logs/viewer"
    override val title = "Log Viewer"
    // ...
}
```

The resulting URLs will be:
- `/admin/resources/settings/theme`
- `/admin/resources/settings/profile`
- `/admin/resources/system/logs/viewer`

All pages appear in the sidebar under their respective `groupName` and can be navigated to directly via their URL.

## Sidebar Organization

Custom pages appear in the sidebar alongside your database resources. Group related pages using the `groupName` property:

```kotlin
// These will appear under the "Configuration" group in the sidebar
customPage("settings") { groupName = "Configuration"; ... }
customPage("settings/theme") { groupName = "Configuration"; ... }

// These will appear under the "Monitoring" group
customPage("system/status") { groupName = "Monitoring"; ... }
customPage("system/logs") { groupName = "Monitoring"; ... }
```

Pages within a group are sorted by their `order` property.

## Permissions

Restrict page access to specific roles:

```kotlin
class AdminDashboardPage : CustomAdminPage() {
    override val path = "admin/dashboard"
    override val title = "Admin Dashboard"
    override val permissions = listOf("admin", "super-admin")

    override suspend fun content(call: ApplicationCall): String {
        return "<h2>Admin Dashboard</h2>"
    }
}
```

Users without the required roles will receive a 403 Forbidden response.

## Full Control Override

Override the `render` method to bypass the admin shell entirely:

```kotlin
class FullPageCustom : CustomAdminPage() {
    override val path = "custom/full"
    override val title = "Full Page"
    override val visible = false // Hidden from sidebar

    override suspend fun render(call: ApplicationCall): String {
        return """
            <!DOCTYPE html>
            <html>
            <head><title>Full Page</title></head>
            <body>
                <h1>Completely custom layout</h1>
            </body>
            </html>
        """.trimIndent()
    }
}
```

## Complete Example

```kotlin
class SettingsPage : CustomAdminPage() {
    override val path = "settings"
    override val title = "Settings"
    override val description = "Application settings and configuration"
    override val icon = "/static/images/settings.svg"
    override val groupName = "Management"
    override val order = 1

    override suspend fun content(call: ApplicationCall): String {
        return """
            <div style="max-width:700px;">
                <h2 style="margin-bottom:16px;">Application Settings</h2>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;margin-bottom:16px;">
                    <h3 style="margin:0 0 8px 0;">General</h3>
                    <p style="margin:0;color:var(--transparent-black-50);">Configure general settings.</p>
                </div>
                <div style="background:var(--white-transparent-60);padding:20px;border-radius:12px;">
                    <h3 style="margin:0 0 8px 0;">Security</h3>
                    <p style="margin:0;color:var(--transparent-black-50);">Authentication and access control.</p>
                </div>
            </div>
        """.trimIndent()
    }
}

// In your Ktor module:
install(KtorAdmin) {
    customPage(SettingsPage())
    // or DSL:
    customPage("settings/theme") {
        title = "Theme"
        groupName = "Settings"
        render { "<h1>Theme Settings</h1>" }
    }
}
```
