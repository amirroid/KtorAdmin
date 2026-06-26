---
description: >-
  Learn how to define database column attributes in **KtorAdmin**, including
  metadata, constraints, and validation rules.
---

# Column Attributes in KtorAdmin

In this section, we will define various attributes for database columns using annotations in KtorAdmin. These attributes help specify metadata, constraints, and behavior of database columns in an admin panel.

#### Column Metadata

The `@ColumnInfo` annotation allows defining essential properties of a column:

```kotlin
@ColumnInfo(
    columnName = "description",
    verboseName = "Description",
    blank = false,
    nullable = false,
    defaultValue = "Default description",
    readOnly = false,
    unique = false
)
val desc = text("description")
```

* **`columnName`** _(Default: Property name)_: Specifies the actual column name in the database. If not provided, the property name is used.
* **`verboseName`** _(Default: Column or Property name)_: A user-friendly name for display purposes.
* **`defaultValue`** _(Default: Empty String → null)_: Sets a default value if no data is provided.
* **`nullable`** _(Default: false)_: Determines if the column allows null values.
* **`unique`** _(Default: false)_: Ensures values are unique across the table.
* **`blank`** _(Default: true)_: Indicates whether the field can be left empty.
* **`readOnly`** _(Default: false)_: Prevents modification after initial creation.

#### Ignoring Columns in Admin Panel

Use `@IgnoreColumn` for columns that should not appear in the admin panel, such as auto-generated IDs:

```kotlin
@IgnoreColumn
val id = integer("id").autoIncrement()
```

#### Applying Constraints with `@Limits`

The `@Limits` annotation allows defining validation constraints for columns. By default, no constraints are applied unless explicitly specified.

**String Constraints:**

```kotlin
@Limits(
    maxLength = 255,
    minLength = 5,
    regexPattern = "[A-Za-z0-9 ]*"
)
val title = varchar("title", 255)
```

* **`maxLength / minLength`** _(Default: No limit)_: Defines string length limits.
* **`regexPattern`** _(Default: No pattern)_: Ensures the value matches a specific pattern.

**Email Validation:**

```kotlin
@Limits(
    regexPattern = """[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""
)
val email = varchar("email", 150)
```

* **Ensures the email format is valid.**

**Numeric Constraints:**

```kotlin
@Limits(
    maxCount = 100.0,
    minCount = 1.0
)
val quantity = integer("quantity")
```

* **`maxCount / minCount`** _(Default: No limit)_: Specifies numerical limits.

**Date Constraints:**

```kotlin
@Limits(
    minDateRelativeToNow = 10 * 24 * 60 * 60 * 1000L, // At least 10 days before now
    maxDateRelativeToNow = 7 * 24 * 60 * 60 * 1000L // Up to 7 days in the future
)
val createdAt = datetime("created_at")
```

* **`minDateRelativeToNow / maxDateRelativeToNow`** _(Default: No restriction)_: Defines a valid time range relative to the current moment. The system subtracts `minDateRelativeToNow` from the current time, ensuring the value is not earlier than the calculated result. `maxDateRelativeToNow` ensures the date does not exceed the specified limit into the future. The system automatically applies these constraints based on the current timestamp.

**File Constraints:**

```kotlin
@LocalUpload
@Limits(
    maxBytes = 1024 * 1024 * 20, // 20MB
    allowedMimeTypes = ["video/mp4"]
)
val file = varchar("file", 1000).nullable()
```

* **`maxBytes`** _(Default: No limit)_: Limits the file size.
* **`allowedMimeTypes`** _(Default: No restriction)_: Restricts the file type.

By applying these annotations, you can effectively manage database column properties and enforce necessary validations in KtorAdmin.
