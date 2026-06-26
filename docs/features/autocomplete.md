# AutoComplete

AutoComplete is a feature in KtorAdmin that replaces foreign-key input fields with a searchable dropdown (similar to Django Admin).

***

### Usage

<pre class="language-kotlin"><code class="lang-kotlin">@ColumnInfo("user_id", verboseName = "Users")
@ManyToOneReferences("users", "id")
@AutoComplete(
    searchFields = ["username", "email"]
)
<strong>val userId = integer("user_id").references(Users.id)
</strong></code></pre>

AutoComplete enables a searchable dropdown for foreign-key fields using `@AutoComplete` on `ManyToOneReferences` and `OneToOneReferences`.

`searchFields` defines which columns in the referenced table will be used for searching. For example, if you set `["username", "email"]`, the dropdown search will match results against those fields instead of only the id.

***

### DisplayFormat

To control how items are displayed in the dropdown, use `@DisplayFormat`:

```kotlin
@DisplayFormat("{id} - {username}")
object User
```

***

### Fallback Display

If `@DisplayFormat` is not defined, the default display format is:

```
Object {id}
```

***

### Features

* Searchable foreign-key dropdown
* Async selection UI



* Human-readable display via `DisplayFormat`
