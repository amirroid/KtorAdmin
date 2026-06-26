---
description: >-
  Modify and transform data before storing or displaying it in KtorAdmin using
  ValueMappers.
---

# Value Mappers

Value mappers allow modifying data **before saving** and **before displaying** it. For example, passwords need to be encrypted before storage.

> <mark style="color:red;">**Important:**</mark> This feature is not supported for MongoDB at this time.

To define a **custom ValueMapper**, create an object implementing `KtorAdminValueMapper`:

```kotlin
object PasswordValueMapper : KtorAdminValueMapper {
    val cryptoManager = CryptManager()
    
    override fun map(value: Any?): Any? {
        return value?.let { cryptoManager.encrypt(value) }
    }

    override fun restore(value: Any?): Any? {
        return value
    }

    override val key: String
        get() = "password"
}
```

* **`map(value: Any?)`** → Transforms the value before storing it in the database. In this case, it encrypts the password.
* **`restore(value: Any?)`** → Restores the value when retrieving it from the database. Since passwords do not need to be displayed in the admin panel, decryption is not performed.
* **`key: String`** → A unique identifier for the ValueMapper.

{% hint style="info" %}
**Note:** Since passwords do not need to be displayed in the admin panel, we do not decrypt the stored value.
{% endhint %}

#### **Registering the ValueMapper**

```kotlin
install(KtorAdmin){
    registerValueMapper(PasswordValueMapper)
}
```

#### **Using `@ValueMapper` on Fields**

Apply `@ValueMapper` to fields requiring transformation:

```kotlin
@ValueMapper(key = "password")
val password = text("password")
```

This ensures passwords are encrypted before saving while keeping them hidden in the admin panel.
