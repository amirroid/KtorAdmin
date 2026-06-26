---
description: >-
  Enable previews for fields in the edit or create page, such as video, image,
  or custom HTML previews.
---

# Preview

The **Preview** feature allows displaying a **preview of a field's data** when editing or adding a record. This is useful for previewing videos, images, or any other content.

To define a **custom Preview**, create a class extending `KtorAdminPreview`:

```kotlin
class VideoPreview : KtorAdminPreview() {
    override val key: String
        get() = "video_preview"

    override fun createPreview(tableName: String, name: String, value: Any?): String? {
        return value?.toString()?.let { video ->
            expandable(title = "Video preview") {
                video(src = video)
            }
        }
    }
}
```

* **`key: String`** → A unique identifier for the preview.
* **`createPreview(tableName: String, name: String, value: Any?): String?`** → Generates an HTML string for the preview.
  * **`tableName`** → The name of the table.
  * **`name`** → The column name or field name in collections.
  * **`value`** → The stored value of the field.
  * **Returns** → A **String** containing HTML content for the preview. If `null` is returned, no preview will be displayed.

#### **Prebuilt Template Functions**

Several helper functions are available to simplify preview creation:

* **`expandable(title: String, content: () -> String): String`** → Creates an expandable section.
* **`video(src: String): String`** → Generates an HTML video element.
* **`image(src: String): String`** → Generates an HTML image element.

#### **Registering the Preview**

```kotlin
install(KtorAdmin){
    registerPreview(VideoPreview())
}
```

#### **Using `@Preview` on Fields**

Apply `@Preview` to fields requiring a preview:

```kotlin
@Preview(key = "video_preview")
@Limits(
    maxBytes = 1024 * 1024 * 20,
    allowedMimeTypes = ["video/mp4"]
)
@LocalUpload
val file = varchar("file", 1000).nullable()
```

This ensures a video preview is displayed when editing or adding a record.
