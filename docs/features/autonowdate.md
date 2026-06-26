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

# AutoNowDate

In **KtorAdmin**, the `@AutoNowDate` annotation allows for automatic handling of date-time fields in the database without requiring manual intervention.

#### Use Case: Auto Set Creation Date

To automatically set the creation date for a field, apply the `@AutoNowDate` annotation to the field. This will ensure that the field is automatically populated with the current date and time when the record is created.

**Example:**

```kotlin
@AutoNowDate
val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
```

#### Use Case: Auto Update Date

If you want a field to be updated with the current date and time whenever the record is modified, you can use the `updateOnChange` parameter of the `@AutoNowDate` annotation.

**Example:**

```kotlin
@AutoNowDate(updateOnChange = true)
val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
```

This ensures that the `updatedAt` field is automatically updated whenever any change occurs in the corresponding record.

***

This should provide a clear and concise explanation of how to use the `@AutoNowDate` annotation in **KtorAdmin** for automatic date-time handling.
