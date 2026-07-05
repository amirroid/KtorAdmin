# KtorAdmin

### Modern Admin Panels for Ktor Applications 🚀

![KtorAdmin Banner](/art/banner.jpg)

![Java 21](https://img.shields.io/badge/Java-21-blue?style=flat-square)
![Maven Central](https://img.shields.io/maven-central/v/io.github.amirroid/KtorAdmin?style=flat-square)
![Ktor](https://img.shields.io/badge/Ktor-Supported-brightgreen?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supported-brightgreen?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-Supported-brightgreen?style=flat-square)
![JDBC](https://img.shields.io/badge/JDBC-Supported-brightgreen?style=flat-square)
![SQL](https://img.shields.io/badge/SQL-Supported-brightgreen?style=flat-square)
![MongoDB](https://img.shields.io/badge/MongoDB-Supported-brightgreen?style=flat-square)

## ✨ Overview

**KtorAdmin** is a powerful, extensible, and schema-independent admin panel framework built specifically for **Ktor**.

Instead of relying on predefined database schemas, KtorAdmin automatically discovers your entities and generates a fully functional admin interface at runtime. Whether you're using relational databases through **Hibernate** or **Exposed**, or working with **MongoDB**, KtorAdmin adapts to your data model with minimal configuration.

Build production-ready admin panels in minutes, not days.

| Feature       | Dark Mode                        | Light Mode                         |
| ------------- | -------------------------------- | ---------------------------------- |
| Dashboard     | ![Dark](/art/dark_dashboard.png) | ![Light](/art/light_dashboard.png) |
| Data Panel    | ![Dark](/art/panel_dark.png)     | ![Light](/art/panel_light.png)     |
| Create & Edit | ![Dark](/art/upsert_dark.png)    | ![Light](/art/upsert_light.png)    |

---

## 🎬 Live Demo

🔗 **Demo Source Code**
https://github.com/Amirroid/KtorAdminDemo

---

## 🚀 Features

* Automatic admin panel generation from your entities
* Support for **Exposed**, **Hibernate**, and **MongoDB**
* Schema-independent architecture
* Role-based access control (RBAC)
* Multiple authentication providers
* Custom admin actions
* Event system for create, update, and delete operations
* Built-in file management and thumbnail generation
* Rich text editor support
* Advanced filtering, searching, and sorting
* Data export capabilities
* Localization and multilingual support
* Production-ready performance and scalability
* Fully customizable admin panel templates
* Extensible template system

---

## 💡 Why KtorAdmin?

Most admin panel solutions require extensive setup, custom dashboards, or tightly coupled database schemas.

KtorAdmin takes a different approach:

* Less boilerplate
* Faster development
* Dynamic entity discovery
* Highly extensible architecture
* Works across SQL and NoSQL databases

Focus on building your application while KtorAdmin handles the administrative interface.

---

# 🚀 Getting Started

Integrating KtorAdmin into an existing Ktor application takes only a few steps.

## 1. Add Dependencies

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.amirroid:KtorAdmin:<version>")
    ksp("io.github.amirroid:KtorAdmin:<version>")
}
```

---

## 2. Install the Plugin

```kotlin
fun Application.configureAdmin() {
    install(KtorAdmin)
}
```

---

## 3. Enable KtorAdmin

```kotlin
fun Application.module() {
    configureAdmin()
}
```

---

## 4. Open the Admin Panel

```text
http://localhost:8080/admin
```

---

## 📚 Documentation

Full documentation, guides, and advanced configuration options are available here:

https://amirroid.gitbook.io/ktor-admin

---

## 📄 License

KtorAdmin is developed and maintained by **amirroid** and released under the **MIT License**.
