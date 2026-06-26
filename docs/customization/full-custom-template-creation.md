# Full Custom Template Creation

Here’s the cleaned version with your requested removals and the small addition about template loaders:

***

## Full Custom Template Creation

KtorAdmin allows you to fully replace the admin UI by implementing the `AdminTemplate` interface.\
This gives you complete control over rendering using Ktor HTML DSL, FreeMarker, Thymeleaf, or any other template engine.

***

### Core Flow

```
Controller → TemplateModel → AdminTemplate.render* → HTTP Response
```

Controllers only prepare data. Rendering is fully handled by your template implementation.

***

### AdminTemplate Interface

```kotlin
interface AdminTemplate {
    suspend fun renderDashboard(call: ApplicationCall, model: TemplateModel)
    suspend fun renderPanelList(call: ApplicationCall, model: TemplateModel)
    suspend fun renderJdbcUpsert(call: ApplicationCall, model: TemplateModel)
    suspend fun renderMongoUpsert(call: ApplicationCall, model: TemplateModel)
    suspend fun renderLogin(call: ApplicationCall, model: TemplateModel)
    suspend fun renderJdbcConfirmation(call: ApplicationCall, model: TemplateModel)
    suspend fun renderMongoConfirmation(call: ApplicationCall, model: TemplateModel)
}
```

Each method represents a full page in the admin panel and must be implemented.

***

### TemplateModel

A simple data container passed to all render methods:

```kotlin
data class TemplateModel(
    val data: Map<String, Any?> = emptyMap()
)
```

Access values via:

```kotlin
model["key"]
```

***

### Common Model Keys

| Key                | Type                 | Description       |
| ------------------ | -------------------- | ----------------- |
| `panelGroups`      | List                 | Sidebar structure |
| `counts`           | Map\<String, Long>   | Table row counts  |
| `username`         | String               | Current user      |
| `adminPath`        | String               | Base route        |
| `hasAuthenticate`  | Boolean              | Auth enabled      |
| `translations`     | Map\<String, String> | Localization      |
| `layout_direction` | String               | `ltr` / `rtl`     |
| `menus`            | List                 | Custom menu items |

#### Upsert-specific

| Key                | Type   | Description             |
| ------------------ | ------ | ----------------------- |
| `columns / fields` | List   | Form schema             |
| `item / object`    | Map    | Existing row data       |
| `tinyMCEConfig`    | String | Rich text editor config |

***

### Basic Implementation

Example using Ktor HTML DSL:

```kotlin
class MyTemplate : AdminTemplate {

    override suspend fun renderDashboard(call: ApplicationCall, model: TemplateModel) {
        call.respondHtml {
            body { h1 { +"Dashboard" } }
        }
    }

    override suspend fun renderPanelList(call: ApplicationCall, model: TemplateModel) {
        call.respondHtml {
            body {
                h1 { +(model["tableName"] as? String ?: "List") }
            }
        }
    }

    override suspend fun renderJdbcUpsert(call: ApplicationCall, model: TemplateModel) {
        call.respondHtml {
            body {
                form {
                    // fields from model["columns"]
                }
            }
        }
    }

    override suspend fun renderMongoUpsert(call: ApplicationCall, model: TemplateModel) =
        renderJdbcUpsert(call, model)

    override suspend fun renderLogin(call: ApplicationCall, model: TemplateModel) {
        call.respondHtml {
            body { h1 { +"Login" } }
        }
    }

    override suspend fun renderJdbcConfirmation(call: ApplicationCall, model: TemplateModel) =
        renderJdbcUpsert(call, model)

    override suspend fun renderMongoConfirmation(call: ApplicationCall, model: TemplateModel) =
        renderMongoUpsert(call, model)
}
```

***

### Registration

```kotlin
install(KtorAdmin) {
    template = MyTemplate()
}
```

This completely replaces the default template system.

***

### Template Loader Support

You can use **any template loader or rendering engine** you want.\
KtorAdmin does not restrict you to Ktor HTML DSL.

Examples include:

* FreeMarker
* Thymeleaf
* Mustache
* Custom file-based loaders
* Hybrid rendering systems

Your `AdminTemplate` implementation is responsible for integrating and rendering using the engine of your choice.
