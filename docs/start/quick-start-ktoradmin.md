---
description: >-
  Quickly set up KtorAdmin in your Ktor application with just a few simple
  steps. Add dependencies, install the plugin, and access your admin panel in
  minutes.
---

# Quick Start: KtorAdmin

#### **Getting Started**

Setting up **KtorAdmin** is straightforward and follows the same installation process as any other Ktor plugin. With just a few simple steps, you can integrate an admin panel into your Ktor application effortlessly.

**Step 1: Add Dependency**

First, add KtorAdmin to your `build.gradle.kts`:&#x20;

![Maven Central](https://img.shields.io/maven-central/v/io.github.amirroid/KtorAdmin)&#x20;

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

repositories {
    mavenCentral()
}

dependencies {
    // KtorAdmin library
    implementation("io.github.amirroid:KtorAdmin:lastate_version")
    ksp("io.github.amirroid:KtorAdmin:lastate_version")
}
```

**Step 2: Install KtorAdmin Plugin**

Next, install the **KtorAdmin** plugin in your Ktor application by adding the following code:

```kotlin
fun Application.configureAdmin() {
    install(KtorAdmin)
}
```

**Step 3: Enable KtorAdmin in Your Application Module**

Finally, integrate `configureAdmin()` into your application's main module:

```kotlin
fun Application.module() {
    ...
    configureAdmin()
}
```

Now, you can access the **admin panel** by navigating to:

```
http://localhost:8080/admin
```
