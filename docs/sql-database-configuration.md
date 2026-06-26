---
description: >-
  In this page, you will learn how to configure and manage SQL databases in
  KtorAdmin.
---

# SQL Database Configuration

KtorAdmin allows seamless integration with relational databases using JDBC. To configure an SQL database, use the `jdbc` function within `install(KtorAdmin)`.

#### Installing the Plugin

Before defining a database, ensure the KtorAdmin plugin is installed:

```kotlin
install(KtorAdmin) {
    jdbc()
}
```

This setup enables support for SQL databases in your Ktor application.

#### Defining an SQL (Relational) Database

To register a relational database securely, use environment variables instead of hardcoding sensitive information:

```kotlin
install(KtorAdmin) {
    jdbc(
        key = null, // This will be the default database for tables without a specific databaseKey
        url = environment.config.property("database.url").getString(),
        username = environment.config.property("database.username").getString(),
        password = environment.config.property("database.password").getString(),
        driver = JDBCDrivers.MYSQL
    )
}
```

#### Security Recommendation

For enhanced security:

* **Never hardcode credentials** in the source code.
* Use **environment variables** to store database credentials.
* Utilize tools like **Docker Secrets, Vault, or AWS Secrets Manager** for managing sensitive data securely.

#### Understanding `jdbc` Parameters

The `jdbc` function requires several parameters, each playing a critical role in database configuration:

* **`key: String?`** – A unique identifier for the database.
  * If `null`, this database will be considered the **default database**.
  * Each table in KtorAdmin has a `databaseKey`, which must match this value.
  * **Important:** The `key` must be **unique** across databases; otherwise, the project will throw an error.
* **`url: String`** – The JDBC connection URL for the database.
  * Specifies the database location and its connection parameters.
* **`username: String`** – The username used to authenticate with the database.
* **`password: String`** – The password for the given username.
* **`driver: String`** – The fully qualified class name of the JDBC driver.

#### Available JDBC Drivers

KtorAdmin provides built-in support for multiple relational database systems via `JDBCDrivers`, allowing you to use predefined driver names. However, ensure you have added the necessary dependencies for your chosen database beforehand.

* **PostgreSQL** → `JDBCDrivers.POSTGRES`
* **MySQL** → `JDBCDrivers.MYSQL`
* **MariaDB** → `JDBCDrivers.MARIADB`
* **SQLite** → `JDBCDrivers.SQLITE`
* **Microsoft SQL Server** → `JDBCDrivers.MSSQL`
* **Oracle** → `JDBCDrivers.ORACLE`
* **IBM Db2** → `JDBCDrivers.DB2`
* **Apache Derby (Embedded)** → `JDBCDrivers.DERBY_EMBEDDED`
* **Apache Derby (Network)** → `JDBCDrivers.DERBY_NETWORK`
* **H2 Database Engine** → `JDBCDrivers.H2`
* **Firebird SQL** → `JDBCDrivers.FIREBIRD`
* **Sybase ASE** → `JDBCDrivers.SYBASE`
* **ClickHouse** → `JDBCDrivers.CLICKHOUSE`
* **Amazon Redshift** → `JDBCDrivers.REDSHIFT`
* **Snowflake** → `JDBCDrivers.SNOWFLAKE`
* **Google Cloud Spanner** → `JDBCDrivers.SPANNER`
* **SAP HANA** → `JDBCDrivers.SAP_HANA`
* **Vertica** → `JDBCDrivers.VERTICA`
* **NuoDB** → `JDBCDrivers.NUODB`
* **Informix** → `JDBCDrivers.INFORMIX`

> **Info:** Not all of the listed databases have been fully tested in KtorAdmin. If you encounter any issues, please report them.

#### Example: Registering Multiple Databases Securely

You can register multiple databases, ensuring each has a unique `key`:

```kotlin
install(KtorAdmin) {
    jdbc(
        key = "mainDB",
        url = environment.config.property("maindb.url").getString(),
        username = environment.config.property("maindb.username").getString(),
        password = environment.config.property("maindb.password").getString(),
        driver = JDBCDrivers.POSTGRES
    )

    jdbc(
        key = "secondaryDB",
        url = environment.config.property("secondarydb.url").getString(),
        username = environment.config.property("secondarydb.username").getString(),
        password = environment.config.property("secondarydb.password").getString(),
        driver = JDBCDrivers.MYSQL
    )
}
```

Each table must specify which database it belongs to using `databaseKey`.

**Example:**

```kotlin
@ExposedTable(
    tableName = "tasks",
    primaryKey = "id",
    singularName = "task",
    pluralName = "tasks",
    iconFile = "/static/images/tasks.png",
    databaseKey = "mainDB"
)
object Tasks : Table("tasks")
```

This ensures that the `tasks` table is linked to `mainDB`. For further details on database usage, indexing, and transactions, refer to the **Database Documentation**. 🚀
