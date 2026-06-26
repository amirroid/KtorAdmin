---
description: >-
  Defines relationships between tables, supporting OneToOne, ManyToOne, and
  ManyToMany in Exposed and Hibernate.
---

# Table Relationships

KtorAdmin supports three types of relationships between tables:

* **One-to-One (OneToOne)**
* **Many-to-One (ManyToOne)**
* **Many-to-Many (ManyToMany)**

#### **OneToOne & ManyToOne**

In Hibernate, **OneToOne** and **ManyToOne** relationships are automatically detected. These relationships are implemented in the same way.

**Parameters:**

* **`tableName`** → The name of the referenced table.
* **`foreignKey`** → The name of the foreign key column.

**Example in Exposed:**

```kotlin
@ManyToOneReferences("users", "id")
val userId = integer("user_id").references(Users.id)

@OneToOneReferences("users", "id")
val userId = integer("user_id").references(Users.id)
```

> **Note:** The referenced tables **must** be registered with either `ExposedTable` or `HibernateTable`.

**Example in Hibernate:**

```kotlin
@ManyToOne
@JoinColumn(name = "author_id", nullable = false)
var author: Author

@OneToOne
@JoinColumn(name = "author_id", nullable = false)
var author: Author
```

#### **ManyToMany**

For **ManyToMany** relationships, use `ManyToManyReferences`.

**Parameters:**

* **`tableName`** → The name of the related table.
* **`joinTable`** → The name of the join table.
* **`leftPrimaryKey`** → The primary key column of the current table in the join table.
* **`rightPrimaryKey`** → The primary key column of the referenced table in the join table.

**Example in Exposed:**

```kotlin
@ManyToManyReferences"users", "tasks_users", "task_id", "user_id")
val users = EmptyColumn()
```

> **Note:** `EmptyColumn` is designed specifically for Exposed. KtorAdmin recognizes it as a column in the admin panel. Any class that is not a subclass of `Column` in Exposed or `EmptyColumn` will not be detected.

**Example in Hibernate:**

```kotlin
@ManyToManyReferences("users", "tasks_users", "task_id", "user_id")
var users: List<User>
```

#### **Displaying References in Forms**

If you want to format how a reference is displayed in the edit or add page, you can use `@DisplayFormat`.

You can reference related columns using dot notation, like this:

```kotlin
@DisplayFormat(
    format = "{id} - User: {user_id.username}"
)
object Tasks : Table("tasks") {
    @IgnoreColumn
    val id = integer("id").autoIncrement()

    @ColumnInfo("user_id", verboseName = "Users")
    @OneToOneReferences("users", "id")
    val userId = integer("user_id").references(Users.id)
}
```

If a table references `task`, it will be displayed using the specified format in the admin panel. For example, if the format is `{id} - User: {user_id.username}`, a reference to a task with `id = 5` and `user_id.username = "JohnDoe"` will appear as:

**`5 - User: JohnDoe`**
