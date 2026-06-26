---
description: Security Considerations in KtorAdmin
---

# Security Considerations

**1. Configuring CSRF Token Expiration Time**

KtorAdmin includes built-in CSRF protection by issuing a CSRF token for each session. By default, the CSRF token expires after **10 minutes** (`10 * 60 * 1000L` milliseconds). You can customize this expiration time using `csrfTokenExpirationTime`.

**Example:**

```kotlin
install(KtorAdmin) {
    csrfTokenExpirationTime = 500000 // Sets CSRF token expiration time to 500 seconds (500,000 milliseconds)
}
```

#### **2. Configuring Rate Limiting**

KtorAdmin includes rate limiting to prevent abuse and excessive requests. By default, if the `RateLimit` plugin is not installed, KtorAdmin will add it automatically with a default limit of **30 requests per minute**. You can modify the request limit per minute using `rateLimitPerMinutes`.

**Example:**

```kotlin
install(KtorAdmin) {
    rateLimitPerMinutes = 200 // Limits each user to 200 requests per minute
}
```

**Using Installed RateLimit Plugin**

If you have already installed the `RateLimit` plugin separately, you must explicitly call `configureKtorAdminRateLimit()` to ensure compatibility.

**Installation with Default Rate Limit**

```kotlin
install(RateLimit) {
    configureKtorAdminRateLimit()
}
```

**Installation with Custom Rate Limit**

```kotlin
install(RateLimit) {
    configureKtorAdminRateLimit(rateLimitPerMinutes = 200) // Sets custom rate limit per minute
}
```



**3. Enabling/Disabling Debug Mode**\
If you disable `debugMode`, error messages will not be displayed. This is helpful for production environments where you want to hide detailed error messages from users.

**Example:**

```kotlin
kotlinCopyEditinstall(KtorAdmin) {
    debugMode = false // Disables error messages in production
}
```

***

By adjusting these security configurations, you can enhance the protection of your KtorAdmin instance against CSRF attacks and excessive API requests.
