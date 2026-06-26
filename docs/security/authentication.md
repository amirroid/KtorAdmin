---
description: Authentication in KtorAdmin
---

# Authentication

Before implementing authentication, you need to install the authentication feature:

```kotlin
install(Authentication)
```

#### **Authentication Methods**

There are two ways to implement authentication in KtorAdmin. The recommended approach is using Token Authentication.

***

#### **1. Token Authentication (Recommended)**

In this method, you validate a token to authenticate users and issue tokens upon successful login.

```kotlin
ktorAdminTokenAuth(name = "admin") {
    validateToken { token ->
        if (token == "1234") {
            KtorAdminPrincipal("Amirreza", roles = listOf("admin"))
        } else null
    }
    validateForm { credentials ->
        if (credentials["username"] == "admin" && credentials["password"] == "password") {
            "1234"
        } else null
    }
}
```

**Explanation:**

* **`validateToken`** → Checks the provided token and returns a `KtorAdminPrincipal` if valid.
* **`validateForm`** → Generates a token when valid credentials are provided.

**KtorAdminPrincipal Properties**

* **`name`** → Represents the authenticated admin user's name or identifier.
* **`roles`** → A list of access roles or permissions assigned to the admin user. Can be `null`, in which case the user has access to all sections.
* **`dashboardAccess`** → A flag indicating whether the admin user has access to the admin dashboard. Defaults to `true`, allowing access by default.

> **Note:** These examples are not real implementations. The actual implementation depends on your specific requirements.

***

#### **2. Form-Based Authentication**

This method works similarly to JWT authentication but does not use tokens.

```kotlin
ktorAdminFormAuth(name = "admin") {
    validate { credentials ->
        if (credentials["username"] == "admin" && credentials["password"] == "password") {
            KtorAdminPrincipal("Amirreza", roles = listOf("admin"))
        } else null
    }
}
```

***

#### **Configuring Authentication in KtorAdmin**

Once the authentication method is set up, configure KtorAdmin to use it by specifying the `authenticateName`:

```kotlin
install(KtorAdmin) {
    authenticateName = "admin"
}
```

***

#### Customizing Login Fields

To customize login fields, define the `loginFields` property:

```kotlin
install(KtorAdmin) {
    loginFields = listOf(
        LoginFiled(
            name = "Username",
            key = "username",
            autoComplete = "username"
        ),
        LoginFiled(
            name = "Password",
            key = "password",
            autoComplete = "current-password",
            type = "password"
        )
    )
}
```

**Explanation of Properties:**

* **name** → The display name of the field (e.g., "Username" or "Email").
* **key** → The unique key for submitting the field to the server (e.g., "username").
* **type** → The input type (default is "text", but can be "password", "email", etc.).
* **autoComplete** → Defines the autocomplete attribute for the browser (e.g., "username", "current-password").

***

#### **Setting Up Roles for Tables or Collections**

You can restrict access to tables or collections using the `@AccessRoles` annotation.

* If a table **does not** have `@AccessRoles`, all users (even those without roles) can access it.
* If `@AccessRoles` is present, only users with matching roles from `KtorAdminPrincipal` can access it.

```kotlin
@AccessRoles(["admin", "editor"])
object Tasks : Table("tasks")
```

This ensures that only users with the `admin` or `editor` roles can access the `Tasks` table.

***

#### **Session Handling**

If you have installed the **Sessions** plugin, you must configure session handling for admin authentication using `configureAdminCookies()`:

```kotlin
install(Sessions) {
    configureAdminCookies()
}
```

You can also specify the session expiration time:

```kotlin
configureAdminCookies(maxAge = 1.days)
```

If you have **not** installed the Sessions plugin but still need to change the session expiration time, you can configure it directly in `KtorAdmin`:

```kotlin
install(KtorAdmin) {
    authenticationSessionMaxAge = 10.days
}
```

By following these methods, you can secure your **KtorAdmin** panel with authentication and role-based access control.

***

By following these methods, you can secure your KtorAdmin panel with authentication and role-based access control.
