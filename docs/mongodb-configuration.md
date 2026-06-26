---
description: Configuring MongoDB in KtorAdmin
---

# MongoDB Configuration

Configuring MongoDB in KtorAdmin is very similar to setting up SQL databases, with only minor differences.

#### MongoDB Configuration

To configure MongoDB, use the `mongo` function inside the `install(KtorAdmin)` block:

```kotlin
install(KtorAdmin) {
    mongo(
        key = null, // This will be the default database for collections without a specific databaseKey
        databaseName = environment.config.property("mongo.database").getString(),
        address = MongoServerAddress(
            environment.config.property("mongo.host").getString(),
            environment.config.property("mongo.port").getString().toInt()
        ),
        credential = MongoCredential(
            environment.config.property("mongo.username").getString(),
            environment.config.property("mongo.authDatabase").getString(),
            environment.config.property("mongo.password").getString()
        ),
    )
}
```

#### Understanding MongoDB Configuration Parameters

* **`key`** – A unique identifier for the database. If set to `null` and no specific `databaseKey` is defined for a collection, this database will be used as the default.
* **`databaseName`** – The name of the MongoDB database.
* **`address`** – The MongoDB server address and port.
* **`credential`** – The authentication credentials for connecting to the database.

#### Defining MongoDB Collections

To expose a MongoDB collection in KtorAdmin, you need to annotate it with metadata. Below is an example configuration:

```kotlin
@MongoCollection(
    collectionName = "products",
    primaryKey = "product_id",
    singularName = "Product",
    pluralName = "Products",
    groupName = "Inventory",
    databaseKey = "mongo_inventory",
    iconFile = "product_icon.png",
    showInAdminPanel = true
)
```

> <mark style="color:red;">**Important**</mark><mark style="color:red;">:</mark> Currently, complex and nested documents are not supported. Only simple types are supported at this time.

#### Collection Properties

* **`collectionName`** – The name of the MongoDB collection.
* **`primaryKey`** – The primary key field of the collection.
* **`singularName`** – (Optional) The singular name used in UI or forms.
* **`pluralName`** – (Optional) The plural name used in lists or collections.
* **`groupName`** – (Optional) A group name for organizing collections.
* **`databaseKey`** – (Optional) A custom key to identify the collection’s database. **If used, you must register this key in the MongoDB configuration.**
* **`iconFile`** – (Optional) The icon file representing the collection in the UI.
* **`showInAdminPanel`** – Determines whether the collection appears in the admin panel (`true` by default).
