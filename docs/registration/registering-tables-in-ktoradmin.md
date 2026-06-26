---
description: >-
  Learn how to register database tables using Hibernate and Exposed in
  KtorAdmin.
---

# Registering Tables in KtorAdmin

KtorAdmin allows registering database tables using annotations for both **Hibernate** and **Exposed** ORM frameworks. These annotations define metadata, such as table names, primary keys, UI representation, and grouping.

#### Registering Tables with Hibernate

For Hibernate, use the `@HibernateTable` annotation to define table properties. The actual table name is retrieved from the `@Table` annotation (if present); otherwise, it defaults to the class name. The primary key column is automatically detected from the field annotated with `@Id`.

**Parameters**

* **`singularName` (Optional):** Singular name of the table for UI elements. Defaults to `tableName` if empty.
* **`pluralName` (Optional):** Plural name of the table for collections. Defaults to `tableName` if empty.
* **`groupName` (Optional):** Group name under which the table is categorized.
* **`databaseKey` (Optional):** A custom key linking this table to a specific database.
* **`iconFile` (Optional):** Path or filename for the table’s icon in the UI.
* **`showInAdminPanel` (Default: `true`):** Determines if the table appears in the admin panel.

**Example Usage**

```kotlin
@Entity
@Table(name = "post")
@HibernateTable(singularName = "Post", pluralName = "Posts")
@Serializable
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
)
```

#### Registering Tables with Exposed

For Exposed, use the `@ExposedTable` annotation to define table metadata. The `tableName` and `primaryKey` parameters are **mandatory**.

**Parameters**

* **`tableName` (Required):** The database table name.
* **`primaryKey` (Required):** The column serving as the table’s primary key.
* **`singularName` (Optional):** Singular form of the table name for UI elements. Defaults to `tableName`.
* **`pluralName` (Optional):** Plural form for lists or collections. Defaults to `tableName`.
* **`groupName` (Optional):** Grouping category for organization.
* **`databaseKey` (Optional):** Custom key linking this table to a specific database.
* **`iconFile` (Optional):** Path or filename for the table’s icon in the UI.
* **`showInAdminPanel` (Default: `true`):** Controls table visibility in the admin panel.

**Example Usage**

```kotlin
@ExposedTable(
    tableName = "users",
    primaryKey = "id",
    singularName = "user",
    pluralName = "users",
    groupName = "profiles",
    iconFile = "/static/images/tasks.png"
)
object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", length = 100)
    val email = varchar("email", length = 150)
    val password = text("password")
    override val primaryKey = PrimaryKey(id)
}
```

By using these annotations, you can efficiently manage database tables within KtorAdmin, ensuring proper mapping and administration.
