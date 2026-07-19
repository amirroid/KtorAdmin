---
description: >-
  Create and register dashboards with grid-based layouts. Supports multiple
  dashboards, grouping, and a designated primary dashboard.
---

# Managing Dashboards

## Overview

Dashboards are the visual backbone of KtorAdmin's landing pages. Each dashboard is a grid of sections (charts, text widgets, lists, or custom HTML) that adapt responsively to screen size.

KtorAdmin supports **multiple dashboards**, each registered with its own path and sidebar group. You can designate one dashboard as the **primary** landing page at the admin root.

## Two Approaches

### Class-Based Dashboard

Create a class extending `KtorAdminDashboard`:

```kotlin
class CustomDashboard : KtorAdminDashboard() {
    override val title = "Overview"
    override val icon = "/static/images/dashboard.svg"
    override val groupName = "Analytics"
    override val isPrimary = true

    override fun KtorAdminDashboard.configure() {
        configureLayout {
            addSection(section = TaskLastSection(), height = "200px")
            addSection(section = TaskChartSection(), span = 2)
            media(maxWidth = "600px", template = listOf(1))
        }
    }
}
```

### Inline DSL

Use the `page(...)` builder for quick inline dashboards:

```kotlin
install(KtorAdmin) {
    dashboard {
        page("analytics") {
            title = "Analytics"
            icon = "/static/images/info.svg"
            groupName = "Operations"
            isPrimary = true

            configureLayout {
                addSection(section = ServerStatusSection(), height = "200px")
                addSection(section = QuickLinksSection(), height = "200px")
                media(maxWidth = "600px", template = listOf(1))
            }
        }
    }
}
```

## Registration

All dashboards are registered inside the `dashboard { }` block:

```kotlin
install(KtorAdmin) {
    dashboard {
        // Class-based
        register(CustomDashboard())

        // Inline DSL
        page("analytics") {
            title = "Analytics"
            configureLayout {
                addSection(section = ServerStatusSection(), height = "200px")
            }
        }
    }
}
```

You can register as many dashboards as needed. Each gets its own sidebar entry and URL path.

## Dashboard Properties

| Property    | Type      | Default       | Description                                                                    |
|-------------|-----------|---------------|--------------------------------------------------------------------------------|
| `title`     | `String`  | `"Dashboard"` | Display title in sidebar and page header.                                      |
| `icon`      | `String?` | `null`        | SVG icon path for sidebar display.                                             |
| `groupName` | `String?` | `null`        | Sidebar group name. Dashboards are grouped together in navigation.             |
| `order`     | `Int`     | `0`           | Sort order within the group (lower values appear first).                       |
| `visible`   | `Boolean` | `true`        | Whether the dashboard appears in the sidebar.                                  |
| `path`      | `String?` | `null`        | URL path segment. If null, derived from the class simple name. Must be unique. |
| `isPrimary` | `Boolean` | `false`       | Whether this dashboard is the default landing page at the admin root.          |

## Primary Dashboard

Set `isPrimary = true` on exactly one dashboard to make it the default landing page when visiting the admin panel root:

```kotlin
class HomeDashboard : KtorAdminDashboard() {
    override val title = "Home"
    override val isPrimary = true
    // ...
}
```

- If **one** dashboard has `isPrimary = true`, it renders at `/{adminPath}`.
- If **no** dashboard is marked primary, the admin root shows the table/resource list instead.
- If **multiple** dashboards are marked primary, the first registered one wins.

## Multiple Dashboards

```kotlin
install(KtorAdmin) {
    dashboard {
        register(HomeDashboard())       // isPrimary = true → landing page
        register(SalesDashboard())      // accessible at /admin/resources/sales

        page("team") {
            title = "Team"
            groupName = "People"
            configureLayout {
                addSection(section = TeamSection(), height = "300px")
            }
        }
    }
}
```

Each dashboard is accessible at `/{adminPath}/resources/{path}` and appears as a separate sidebar entry.

## Sidebar Grouping

Dashboards are grouped in the sidebar by `groupName`. Set the same `groupName` on related dashboards to cluster them:

```kotlin
install(KtorAdmin) {
    dashboard {
        register(HomeDashboard())              // "Overview" group
        register(SalesDashboard())             // "Analytics" group

        page("team") {
            groupName = "People"
            // ...
        }

        page("roles") {
            groupName = "People"
            // ...
        }
    }
}
```

## Layout Configuration

All dashboards use the same grid system. Inside `configureLayout { }`:

### `addSection`

Adds a section widget to the grid.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `section` | `DashboardSection` | required | The section instance (e.g., `TaskTextSection()`). |
| `height` | `String` | `"350px"` | CSS height of the section. |
| `span` | `Int` | `1` | Number of grid columns the section spans. |

```kotlin
addSection(section = TaskLastSection(), height = "200px")
addSection(span = 2, section = TaskChartSection())
```

### `media` for Responsive Layout

Define different layouts for smaller screens:

```kotlin
media(maxWidth = "600px", template = listOf(1))
```

On screens narrower than `600px`, all sections stack into a single column.

### `setLayoutTemplate`

Override the default grid template (all single-column):

```kotlin
setLayoutTemplate(template = listOf(1, 2, 3))
```

## Complete Example

```kotlin
class AdminOverview : KtorAdminDashboard() {
    override val title = "Admin Overview"
    override val icon = "/static/images/dashboard.svg"
    override val groupName = "Dashboards"
    override val order = 0
    override val isPrimary = true

    override fun KtorAdminDashboard.configure() {
        configureLayout {
            addSection(section = TotalUsersSection(), height = "200px")
            addSection(section = RevenueChartSection(), height = "300px")
            addSection(span = 2, section = RecentOrdersSection(), height = "400px")
            media(maxWidth = "600px", template = listOf(1))
        }
    }
}

install(KtorAdmin) {
    dashboard {
        register(AdminOverview())

        page("quick-stats") {
            title = "Quick Stats"
            groupName = "Dashboards"
            configureLayout {
                addSection(section = ActiveUsersSection(), height = "200px")
            }
        }
    }
}
```

## Related

- [Chart Section](chart-section.md)
- [Text Section](text-section.md)
- [List Section](list-section.md)
- [Render Section](render-section.md)
