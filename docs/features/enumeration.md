---
description: Define enums in KtorAdmin using @Enumeration.
---

# Enumeration

In Hibernate, **enumerations are automatically detected**, meaning there is no need to explicitly define them using an annotation. The value is retrieved directly from the enum class. However, if you need to **customize the behavior**, you can do so manually.

#### **Using Enumeration in KtorAdmin**

In KtorAdmin, the `@Enumeration` annotation allows you to define **valid values** for an enum field.

**Example:**

```kotlin
enum class Priority {
    Low, Medium, High
}

@Enumeration("Low", "Medium", "High")
val priority = customEnumeration(
    "priority",
    "VARCHAR(50)",
    { Priority.valueOf(it as String) },
    { it.name }
)
```

**Example in Hibernate:**

```kotlin
@Enumerated(EnumType.STRING)
val priority: Priority = Priority.Low
```

#### **Styling Enumeration with StatusStyle**

If you want to **display enumerations in the admin panel with custom styling**, use `@StatusStyle`. This annotation applies colors based on the index of the enum values.

**Example with StatusStyle:**

```kotlin
@Enumeration("Low", "Medium", "High")
@StatusStyle("#5ab071", "#493391", "#d62454")
val priority = customEnumeration(
    "priority",
    "VARCHAR(50)",
    { Priority.valueOf(it as String) },
    { it.name }
)
```

> **Note:** `StatusStyle` assigns colors in the order they are defined in `@Enumeration`. The first value corresponds to the first color, and so on.
