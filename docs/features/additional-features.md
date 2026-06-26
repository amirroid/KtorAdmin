---
description: Additional Features for Columns and Fields
---

# Additional Features

KtorAdmin provides various enhancements for defining and managing database columns. Below are some key features:

#### **Computed Columns**

Use `@Computed` to define a column whose value is dynamically computed using a JavaScript expression. This allows referencing other columns and transforming their values.

```kotlin
val name = varchar("name", 150)

@Computed(
    compute = "{name}.toLowerCase().replaceAll(' ', '-')"
)
val slug = varchar("slug", 500)
```

For example, if `name` is **"Ktor Admin"**, the generated `slug` will be **"ktor-admin"**.

**Parameters:**

* **`compute`** → A JavaScript expression to compute the column value dynamically.
* **`readOnly`** → Indicates if the column is read-only. If set, it cannot be manually updated.
  * **Note:** `readOnly` in `@Computed` takes precedence over `readOnly` in `@ColumnInfo`.

***

#### **Confirmation Fields**

Use `@Confirmation` for fields that require user confirmation before editing. This ensures sensitive values like passwords are not changed accidentally.

```kotlin
@Confirmation
val password = text("password")
```

***

#### **Overriding Column Type**

If a column type is incorrectly detected or unsupported, you can manually specify its type using `@OverrideColumnType`.

```kotlin
@OverrideColumnType(ColumnType.DATETIME)
val date: Instant
```

This ensures the correct database type is applied to the column definition.

***

#### Text Area Fields

Use `@TextAreaField` for fields that require multi-line text input, such as descriptions or long-form content. This annotation ensures proper handling in both MongoDB and SQL databases.

**Example:**

```kotlin
@TextAreaField
val description = text("description")
```

This annotation is useful for cases where a standard text input is not sufficient.

You can also use **RichEditor** for a more advanced editing experience. [Learn more](rich-editor.md)
