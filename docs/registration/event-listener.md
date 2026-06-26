---
description: Event Listener in KtorAdmin
---

# Event Listener

KtorAdmin provides an **event listener** system that allows you to handle database-related events dynamically. By extending `AdminEventListener`, you can execute custom logic when **inserting**, **updating**, or **deleting** records in both **relational databases** (JDBC) and **MongoDB**.

#### Understanding `AdminEventListener`

The `AdminEventListener` class provides multiple methods to handle different database operations:

**Insert Events**

* **`onInsertJdbcData`** – Called when a new row is inserted into a **relational database table**.
  * **`tableName`**: The name of the table where data is inserted.
  * **`objectPrimaryKey`**: The primary key of the inserted object.
  * **`events`**: A list of `ColumnEvent` representing column changes.
* **`onInsertMongoData`** – Called when a new document is inserted into a **MongoDB collection**.
  * **`collectionName`**: The name of the collection where data is inserted.
  * **`objectPrimaryKey`**: The primary key of the inserted object.
  * **`events`**: A list of `FieldEvent` representing field changes.

**Update Events**

* **`onUpdateJdbcData`** – Called when a row is updated in a **relational database table**.
  * **`tableName`**: The name of the table where data is updated.
  * **`objectPrimaryKey`**: The primary key of the updated object.
  * **`events`**: A list of `ColumnEvent` representing column changes.
* **`onUpdateMongoData`** – Called when a document is updated in a **MongoDB collection**.
  * **`collectionName`**: The name of the collection where data is updated.
  * **`objectPrimaryKey`**: The primary key of the updated object.
  * **`events`**: A list of `FieldEvent` representing field changes.

**Delete Events**

* **`onDeleteJdbcObjects`** – Called when one or more rows are deleted from a **relational database table**.
  * **`tableName`**: The name of the table where data is deleted.
  * **`objectPrimaryKeys`**: A list of primary keys of the deleted objects.
* **`onDeleteMongoObjects`** – Called when one or more documents are deleted from a **MongoDB collection**.
  * **`collectionName`**: The name of the collection where data is deleted.
  * **`objectPrimaryKeys`**: A list of primary keys of the deleted objects.

#### `ColumnEvent` and `FieldEvent`

Whenever an event occurs, you receive a list of **`ColumnEvent`** (for relational databases) or **`FieldEvent`** (for MongoDB). Each event contains:

* **`changed`** – `true` if the value has changed.
* **`columnSet`** / **`fieldSet`** – Metadata about the column/field (e.g., name, type).
* **`value`** – The new value of the column or field. The type depends on the column definition.

#### Handling File Uploads in Events

If a **column** or **field** is of type **file**, its `value` will be an instance of:

```kotlin
FileEvent(
    val fileName: String,
    val bytes: ByteArray
)
```

You can access `fileName` and `bytes` to process the uploaded file. For example, if a row is inserted into the **`tasks`** table and contains a **video file**, you can generate a thumbnail automatically:

```kotlin
class AdminListener() : AdminEventListener() {
    override suspend fun onInsertJdbcData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>
    ) {
        if (tableName == "tasks") {
            events.find { it.columnSet.columnName == "file" }?.let {
                if (it.changed.not()) return
                (it.value as? FileEvent)?.also {
                    println("Processing file: ${it.fileName}")
                    // Handle Save Thumbnail
                }
            }
        }
    }
}
```

#### Registering the Event Listener

To activate the custom event listener, register it inside the Ktor module:

```kotlin
install(KtorAdmin) {
    registerEventListener(AdminListener())
}
```

With this setup, you can dynamically process **file uploads**, **log changes**, or **execute additional logic** based on database events.
