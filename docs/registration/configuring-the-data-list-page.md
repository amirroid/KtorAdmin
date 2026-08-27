# Configuring the Data List Page

In this section, we will go through different annotations that help configure how data is displayed in the list view within KtorAdmin.

***

#### **1. Displaying Specific Fields with `@PanelDisplayList`**

By default, all fields in a table are displayed in the list view. However, if you want to specify which fields should be shown, you can use the `@PanelDisplayList` annotation.

**Usage Example:**

```kotlin
@PanelDisplayList("name", "priority", "file", "checked", "user_id")
object Tasks : Table("tasks")
```

**Explanation:**

* This annotation ensures that only the specified fields (`name`, `priority`, `file`, `checked`, and `user_id`) will be displayed in the list view.
* Any other fields present in the table but not included in `@PanelDisplayList` will be hidden from the list.

***

#### **2. Configuring Search and Filter Options with `@AdminQueries`**

The `@AdminQueries` annotation is used to define which fields should be searchable and which should be available as filters in the list view.

**Usage Example:**

```kotlin
@AdminQueries(
    searches = ["user_id.username", "description"],
    filters = ["priority", "checked", "user_id"]
)
object Tasks : Table("tasks")
```

**Explanation:**

* **`searches`** → Defines which fields can be searched. In this case:
  * `user_id.username` → Enables searching by the `username` of the user related to `user_id`.
  * `description` → Allows searching based on the `description` field.
* **`filters`** → Defines which fields can be used as filters. Here:
  * `priority` → Enables filtering based on priority values.
  * `checked` → Allows filtering based on whether the task is checked.
  * `user_id` → Enables filtering by user.

**Notes:**

* You can use relations in `searches`, such as `user_id.username`, to search within related entities.
* However, Many-to-Many (`ManyToMany`) relationships are **not** supported for search and filter fields.
* Filters are only supported for the following data types:

```kotlin
enum class FilterTypes {
    ENUMERATION,  // For enum values
    DATE,         // For date fields
    DATETIME,     // For date-time fields
    REFERENCE,    // For foreign key references
    BOOLEAN       // For true/false values
}
```

* If a filter field is of an unsupported type, an error will be thrown.

***

#### **3. Setting Default Sorting with `@DefaultOrder`**

The `@DefaultOrder` annotation allows you to define the default sorting order of records when the list view is loaded.

**Usage Example:**

```kotlin
@DefaultOrder(name = "priority", direction = "ASC")
object Tasks : Table("tasks")
```

**Explanation:**

* **`name`** → Specifies the field by which the data should be sorted (e.g., `priority`).
* **`direction`** → Defines the sorting order:
  * `ASC` (Ascending) → Sorts in increasing order (default).
  * `DESC` (Descending) → Sorts in decreasing order.

***

#### **4. Inline Editing with `@InlineEditable`**

You can enable direct in-table editing for specific columns using the `@InlineEditable` annotation:

* **Text/Numeric Fields**: Annotated fields allow quick modification directly in the table. Press <kbd>Enter</kbd> or click outside to save, or <kbd>Escape</kbd> to cancel.
* **Boolean Fields**: Annotated boolean fields are rendered as clickable checkboxes that toggle and persist status in real time.
* **Security & Validation**: Every change is validated server-side via `PATCH` requests with CSRF protection, role authorization, and event notifications.

For detailed information, see the [Inline Editing Documentation](../features/inline-editing.md).

***

#### **Conclusion**

These annotations and features provide flexibility in managing how data is displayed, searched, filtered, and edited in the list view. By using `@PanelDisplayList`, `@AdminQueries`, `@DefaultOrder`, and Inline Editing, you can customize the admin panel's data presentation and workflows efficiently.
