---
description: Here’s a brief configuration guide
---

# KtorAdmin Configuration Guide

**KtorAdmin Configuration Guide**

This guide covers the main configurations available in **KtorAdmin**, including database setup, storage providers, authentication settings, admin panel customizations, and more.

***

### **1. Database Configuration**

KtorAdmin supports both **JDBC** and **MongoDB** as database sources.

#### **Registering a JDBC Database**

Registers a new **JDBC database**. If the key is `null`, the database will be considered the default one.

```kotlin
jdbc(
    key = null, // This will be the default database for tables without a specific databaseKey
    url = environment.config.property("database.url").getString(),
    username = environment.config.property("database.username").getString(),
    password = environment.config.property("database.password").getString(),
    driver = JDBCDrivers.MYSQL
)
```

[🔗 **More Details**](sql-database-configuration.md)

#### **Registering a MongoDB Client**

Registers a **MongoDB database** with a server address.

```kotlin
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
```

[🔗 **More Details**](mongodb-configuration.md)

***

### **2. Storage Configuration**

KtorAdmin supports **custom storage providers, AWS S3, and local storage**.

#### **Custom Storage Provider**

To use a **custom storage provider** for file handling, register it using:

```kotlin
registerStorageProvider(MyStorageProvider)
```

[🔗 **More Details**](features/file-upload-in-ktoradmin.md)

#### **AWS S3 Storage Configuration**

KtorAdmin supports **AWS S3 storage**, requiring the following configurations:

```kotlin
registerS3Client(
    accessKey = "your-access-key",
    secretKey = "your-secret-key",
    region = "us-east-1",
    endpoint = "https://s3.amazonaws.com"
)
defaultAwsS3Bucket = "my-default-bucket"
awsS3SignatureDuration = Duration.minutes(30)
```

[🔗 **More Details**](features/file-upload-in-ktoradmin.md)

#### **Local Storage Configuration**

For **local storage**, both `mediaPath` and `mediaRoot` need to be set:

```kotlin
mediaPath = "/storage/media"
mediaRoot = "/var/www/media"
```

[🔗 **More Details**](features/file-upload-in-ktoradmin.md#id-1.-local-upload)

#### File Deletion Strategy

KtorAdmin allows you to define how linked files are handled when a database record is deleted.\
You can configure this behavior using:

```kotlin
fileDeleteStrategy = FileDeleteStrategy.DELETE
```

By default, the strategy is set to `KEEP`, meaning files remain in storage even after the record is deleted.

🔗[ **More Details**](features/file-upload-in-ktoradmin.md#file-deletion-strategy)

***

### **3. Authentication & Security**

KtorAdmin provides authentication settings, including **login fields, CSRF protection, session management, and rate limiting**.

#### **Defining Login Fields**

Specifies the **authentication fields** required for login.

```kotlin
loginFields = listOf(
    LoginField("username"),
    LoginField("password")
)
```

[🔗 **More Details**](security/authentication.md)

#### **Setting CSRF Token Expiration**

Defines the **expiration time** for CSRF tokens in milliseconds.

```kotlin
csrfTokenExpirationTime = 3_600_000L  // 1 hour
```

[🔗 **More Details**](security/security-considerations.md)

#### **Configuring Authentication Session Max Age**

Sets the **maximum session duration** for authenticated users.

```kotlin
authenticationSessionMaxAge = Duration.hours(2)
```

[🔗 **More Details**](security/authentication.md)

#### **Setting Authentication Name**

Defines a **custom authentication scheme name**.

```kotlin
authenticateName = "MyAuthScheme"
```

[🔗 **More Details**](security/authentication.md)

#### **Setting API Rate Limits**

Limits the number of **allowed API requests per minute**.

```kotlin
rateLimitPerMinutes = 100
```

[🔗 **More Details**](security/security-considerations.md)

#### Debug Mode Configuration

Enables or disables the debug mode for the admin panel.

```kotlin
debugMode = true // Set to true to enable debug mode for detailed logging
```

***

### **4. Admin Panel & Custom Actions**

KtorAdmin allows **custom dashboards, admin actions, and event listeners**.

#### **Registering a Custom Admin Dashboard**

Registers a **custom admin dashboard**.

```kotlin
adminDashboard = MyCustomDashboard()
```

[🔗 **More Details**](dashboard/managing-section-layouts.md)

#### **Registering a Custom Admin Action**

Registers a **custom action** in the admin panel.

```kotlin
registerCustomAdminAction(MyAdminAction)
```

[🔗 **More Details**](registration/admin-actions.md)

#### **Registering an Admin Event Listener**

Registers an **event listener** for admin-related events.

```kotlin
registerEventListener(MyEventListener)
```

[🔗 **More Details**](registration/event-listener.md)

**Admin Path Configuration**

Defines the base path for accessing the admin panel. You can change it from the default `admin` to any custom path you prefer.

```kotlin
adminPath = "custom-admin" // Customize the path to the admin panel
```

**Providing Custom Menus:**

The `provideMenu` function allows adding custom menu items to specific predefined menus in the admin panel. It takes the table name as input and returns a list of menu items to be added. For example, you can add external links or related sections to the menu.

```kotlin
provideMenu { name ->
    if (name == "tasks") listOf(
        Menu(title = "Github", link = "https://github.com/Amirroid")
    ) else emptyList()
}
```

[🔗 **More Details**](registration/custom-menu-integration.md)

***

### **5. Additional Configurations**

#### **Configuring TinyMCE**

Defines the **TinyMCE editor** settings for text editing.

```kotlin
tinyMCEConfig = TinyMCEConfig(...)
```

[🔗 **More Details**](features/rich-editor.md)

#### **Enabling CSV & PDF Data Download**

Allows **data to be downloaded** in different formats.

```kotlin
canDownloadDataAsCsv = true
canDownloadDataAsPdf = true
```

#### **Registering a Custom Value Mapper**

Registers a **custom value mapper** for handling data transformations.

```kotlin
registerValueMapper(MyValueMapper)
```

[🔗 **More Details**](features/value-mappers.md)

#### **Registering a Custom Preview Handler**

Registers a **preview handler** for specific data types.

```kotlin
registerPreview(MyPreview)
```

[🔗 **More Details**](features/preview.md)

***

This guide provides a concise overview of KtorAdmin’s key configurations. Make sure to check out other sections of the documentation for more details. 🚀
