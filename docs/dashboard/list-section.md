---
layout:
  width: default
  title:
    visible: true
  description:
    visible: false
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: true
  metadata:
    visible: true
  tags:
    visible: true
  actions:
    visible: true
---

# List Section

**Overview**

The `ListDashboardSection` is an abstract base class designed for displaying tabular data within a dashboard section. It provides a structured approach to retrieving and presenting list-based information from a database table.

#### **Required Properties**

* **`tableName`** (`String`): Specifies the database table from which data is retrieved.

#### **Optional Properties (with Defaults)**

* **`fields`** (`List<String>?`): A list of column names to include in the result. If `null`, it will use the predefined list specified with `@PanelDisplayList`. If that is also not defined, all fields will be displayed.
* **`limitCount`** (`Int?`): The maximum number of rows to fetch (default: `null`, meaning no limit).
* **`orderQuery`** (`String?`): An optional SQL-style sorting condition for retrieving data (default: `null`).

#### **Usage Notes**

* If **`fields`** is `null`, it will first check for a predefined field list using `@PanelDisplayList`. If no such list is found, all available columns from the table will be included.
* The **`orderQuery`** should be a valid SQL `ORDER BY` clause (e.g., `"created_at DESC"`).

#### **Example Usage**

```kotlin
class RecentOrdersSection : ListDashboardSection() {
    override val tableName = "orders"
    override val fields = listOf("id", "customer_name", "order_date", "total_price")
    override val orderQuery = "order_date DESC"
    override val limitCount = 10
}

class ActiveUsersSection : ListDashboardSection() {
    override val tableName = "users"
    override val fields = listOf("id", "username", "last_login")
    override val orderQuery = "last_login DESC"
}
```

For displaying the configured chart in the dashboard, refer to the section on [**how to configure the dashboard**.](managing-section-layouts.md)

#### **Conclusion**

To integrate a list-based widget into the dashboard panel, extend `ListDashboardSection` and implement the required properties. If `fields` is not explicitly defined, it will automatically use `@PanelDisplayList` or fall back to displaying all fields. For detailed instructions on configuring the dashboard, refer to the dashboard configuration section.
