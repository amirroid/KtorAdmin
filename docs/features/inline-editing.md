# Inline Editing in List View

**Inline Editing** allows administrators to quickly modify simple fields (such as text, numbers, and boolean toggles) directly within the data list table without navigating to the full edit page.

***

### Overview

Inline editing is an opt-in feature enabled per column using the `@InlineEditable` annotation:

* **Text and Numbers**: Click on an annotated cell to open an inline input. Press <kbd>Enter</kbd> or click outside to save, or press <kbd>Escape</kbd> to discard changes.
* **Boolean Fields**: Rendered as interactive checkboxes that update instantly upon clicking.

***

### Usage

Annotate any supported property with `@InlineEditable`:

```kotlin
@InlineEditable
val name = varchar("name", 150)

@InlineEditable
val number = integer("number").default(1)

@InlineEditable
val checked = bool("checked").default(true)
```

***

### Supported & Eligible Fields

Columns and fields are eligible for inline editing when annotated with `@InlineEditable` and having one of the following data types:

| Data Type | Interaction | Description |
| :--- | :--- | :--- |
| `STRING` / `CHAR` | Text Input | Single-line text fields |
| `INTEGER`, `LONG`, `SHORT`, `DOUBLE`, `FLOAT`, `BIG_DECIMAL` | Numeric Input | Number fields respecting step & format |
| `BOOLEAN` | Checkbox Toggle | Instant toggle with automatic rollback on error |

#### Excluded Fields (Non-Inline Editable)

Even if annotated with `@InlineEditable`, complex or sensitive fields are **excluded** from inline editing to prevent data corruption or invalid states:

* **Read-only & Computed**: Fields marked with `readOnly = true` or `@Computed`.
* **Relations**: Foreign key references (`@ManyToOneReferences`, `@ManyToManyReferences`, `@OneToOneReferences`).
* **Uploads**: File and image upload columns (`@LocalUpload`, S3 uploads).
* **Rich Content**: Text areas and rich text editors (`@RichEditor`, `@TextAreaField`).
* **Confirmation Required**: Fields protected with `@ConfirmationField`.
* **Automated Dates**: Fields managed by `@AutoNowDate`.

***

### Complete Example

```kotlin
@PanelDisplayList("name", "number", "checked", "priority", "description")
@ExposedTable(
    tableName = "tasks",
    primaryKey = "id",
    singularName = "task",
    pluralName = "tasks"
)
object Tasks : Table("tasks") {
    val id = integer("id").autoIncrement()

    @InlineEditable
    val name = varchar("name", 150)           // ✅ Inline editable (text)

    @InlineEditable
    val number = integer("number").default(1)  // ✅ Inline editable (number)

    @InlineEditable
    val checked = bool("checked").default(true) // ✅ Inline editable (checkbox)

    val priority = varchar("priority", 50)     // ❌ Not annotated -> Read-only in list

    @TextAreaField
    val description = text("description")      // ❌ Excluded (TextArea)

    override val primaryKey = PrimaryKey(id)
}
```

* `name`, `number`, and `checked` can be modified directly in the list table.
* `priority` and `description` are displayed normally and can be edited via the detail edit page.

***

### User Interaction & Keyboard Shortcuts

* **Click Cell**: Activates the input box for text/number fields.
* <kbd>Enter</kbd>: Commits the change and sends a `PATCH` request.
* <kbd>Escape</kbd>: Cancels editing and reverts to the original value.
* **Blur (Click Outside)**: Automatically commits and saves the change.
* **Visual Indicators**:
  * **Hover**: Subtle border highlighting indicates editable cells.
  * **Saving**: Slight opacity while the request is in flight.
  * **Success Flash**: Green highlight indicates successful persistence.
  * **Error Flash**: Red highlight + alert toast if validation fails or permissions are denied.

***

### Backend Security & Validation

All inline edits are processed through a secure `PATCH` endpoint:

```http
PATCH /admin/resources/{pluralName}/{primaryKey}
```

1. **CSRF Protection**: Validates the anti-CSRF token passed in the `X-CSRF-Token` header or `_csrf` payload.
2. **Authorization**: Ensures the user has required roles (`@AccessRoles`) and that the panel has `Action.EDIT` enabled.
3. **Validation**: Enforces all `@Limits`, data types, nullability, and custom constraints.
4. **Audit & Event Listeners**: Dispatches `onUpdateJdbcData` / `onUpdateMongoData` events to registered `AdminEventListener` implementations.
