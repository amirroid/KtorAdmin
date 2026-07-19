---
description: >-
  Learn how to create custom dashboard sections with server-side HTML rendering
  using the RenderDashboardSection abstract class.
---

# Render Section

**Overview**

The `RenderDashboardSection` class allows you to create fully custom dashboard sections by generating HTML directly on the server. Unlike the built-in chart, text, and list sections, a render section gives you complete control over the output while still benefiting from suspend operations such as database queries, external API calls, or any other coroutine-based work.

The HTML you return from the `render()` function is injected directly into the dashboard template without any additional processing.

### **Creating a Render Section**

To create a custom render section, extend `RenderDashboardSection` and implement the required members:

* **`index`** → A unique integer identifier for this section. Must not collide with other section indices.
* **`sectionName`** → The display title shown in the section header.
* **`render()`** → A suspend function that returns the HTML string to display.

### **Example: Server Status Section**

```kotlin
class ServerStatusSection : RenderDashboardSection() {
    override val index: Int = 100
    override val sectionName: String = "Server Status"

    override suspend fun render(): String {
        val activeUsers = database.query("SELECT COUNT(*) FROM users WHERE active = true")
        val uptime = getServerUptime()

        return """
            <div style="padding: 20px;">
                <h3>$sectionName</h3>
                <p>Active users: $activeUsers</p>
                <p>Server uptime: $uptime</p>
            </div>
        """.trimIndent()
    }
}
```

### **Example: Dynamic Data Table**

```kotlin
class RecentOrdersSection : RenderDashboardSection() {
    override val index: Int = 101
    override val sectionName: String = "Recent Orders"

    override suspend fun render(): String {
        val orders = orderRepository.findRecent(limit = 10)

        val rows = orders.joinToString("\n") { order ->
            """
                <tr>
                    <td>${order.id}</td>
                    <td>${order.customerName}</td>
                    <td>$${order.total}</td>
                </tr>
            """.trimIndent()
        }

        return """
            <table style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr>
                        <th>Order ID</th>
                        <th>Customer</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
        """.trimIndent()
    }
}
```

### **Adding to the Dashboard**

Once you have defined your render section, register a dashboard and add it to the layout using `addSection`:

```kotlin
class CustomDashboard : KtorAdminDashboard() {
    override fun KtorAdminDashboard.configure() {
        configureLayout {
            addSection(section = ServerStatusSection(), height = "200px")
            addSection(section = RecentOrdersSection(), span = 2, height = "400px")
            media(maxWidth = "600px", template = listOf(1))
        }
    }
}

install(KtorAdmin) {
    dashboard {
        register(CustomDashboard())
    }
}
```

### **Notes**

* The `render()` function is a `suspend` function, so you can call any suspend function inside it (database queries, HTTP requests, etc.).
* The returned HTML is rendered as-is — no escaping or template processing is applied.
* Render sections integrate with the existing grid layout system, so you can use `span` and `height` parameters just like other section types.
