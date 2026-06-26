---
description: >-
  For displaying the configured chart in the dashboard, refer to the section on
  how to configure the dashboard.
---

# Managing Section Layouts

**Overview**

To define the layout of dashboard sections in **KtorAdmin**, you need to create a custom class that extends `KtorAdminDashboard`. This class allows you to structure the arrangement of different sections using an internal **grid system**. The grid ensures that all dashboard elements are placed properly and can adapt responsively based on screen size.

#### **Defining a Custom Dashboard**

To define a custom dashboard layout, create a new class that extends `KtorAdminDashboard` and implement the `configure` method. Inside this method, use `configureLayout` to set up the grid and specify how sections should be positioned.

#### **Key Methods for Layout Configuration**

**1. `addSection`**

This method is used to add sections to the dashboard. Each section represents a widget that displays specific data. It accepts the following parameters:

* `section`: The dashboard section instance (e.g., `TaskTextSection()`).
* `height`: _(Optional)_ The height of the section (default: `"350px"`).
* `span`: _(Optional)_ Defines how many columns the section should span in the grid (default: `1`).

**Example Usage:**

```kotlin
addSection(section = TaskLastSection(), height = "200px")
addSection(section = TaskSummaryTextSection(), height = "200px")
addSection(section = TaskSumTextSection(), height = "200px")
addSection(section = TaskAverageTextSection(), height = "200px")
addSection(span = 2, section = TaskChartSection())
addSection(span = 2, section = TaskListChartSection())
```

**2. `media` for Responsive Layout**

You can define different layouts for smaller screens using the `media` function. It allows you to specify a maximum width and a template that dictates how sections should be arranged.

**Example Usage:**

```kotlin
media(maxWidth = "600px", template = listOf(1))
```

This ensures that on screens smaller than `600px`, all sections are displayed in a single-column layout.

**3. `setLayoutTemplate`**

By default, the grid layout follows `listOf(1, 1, 1, 1)`, meaning each section takes one column. You can customize this default layout using `setLayoutTemplate`.

**Example Usage:**

```kotlin
setLayoutTemplate(template = listOf(1, 2, 3))
```

#### **Implementing a Custom Dashboard Class**

Below is a complete example of how to create and configure a custom dashboard layout:

```kotlin
class CustomDashboard : KtorAdminDashboard() {
    override fun KtorAdminDashboard.configure() {
        configureLayout {
            addSection(section = TaskLastSection(), height = "200px")
            addSection(section = TaskSummaryTextSection(), height = "200px")
            addSection(section = TaskSumTextSection(), height = "200px")
            addSection(section = TaskAverageTextSection(), height = "200px")
            addSection(span = 2, section = TaskChartSection())
            addSection(span = 2, section = TaskListChartSection())
            media(maxWidth = "600px", template = listOf(1))
        }
    }
}
```

#### **Registering the Dashboard in Ktor**

Once the dashboard layout is defined, it must be registered in the **KtorAdmin** plugin to take effect. This is done using the `install` function:

```kotlin
install(KtorAdmin) {
    adminDashboard = CustomDashboard()
}
```

This ensures that the **CustomDashboard** is loaded when the application starts, displaying the sections in the configured layout.
