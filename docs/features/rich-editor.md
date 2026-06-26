---
description: Using Rich Editor in KtorAdmin
---

# Rich Editor

KtorAdmin integrates **TinyMCE** as the rich text editor. To use it, simply annotate the desired field with `@RichEditor`:

```kotlin
@RichEditor
val description = text("description")
```

#### **Custom Configuration**

You can customize the TinyMCE configuration using `tinyMCEConfig` in the KtorAdmin setup:

```kotlin
install(KtorAdmin) {
    tinyMCEConfig = TinyMCEConfig(
        // Custom settings
    )
}
```

KtorAdmin provides two predefined configurations:

* **Basic (default)** → `TinyMCEConfig.Basic`
* **Professional** → `TinyMCEConfig.Professional`

**File Upload**

To enable file uploads, set the `uploadTarget` inside the configuration. Example:

```kotlin
tinyMCEConfig = TinyMCEConfig.Professional.copy(uploadTarget = UploadTarget.LocalFile(path = null))
```

> **Note:** Either `path` must be provided or `mediaPath` must be set beforehand. Additionally, `mediaRoot` is required.

#### **TinyMCEConfig Parameters**

* **`height`** → Editor height in pixels.
* **`language`** → UI language of the editor.
* **`directionality`** → Text direction (`ltr` or `rtl`).
* **`plugins`** → List of enabled plugins.
* **`toolbar`** → Space-separated list of toolbar buttons.
* **`branding`** → Shows or hides the TinyMCE branding.
* **`menubar`** → Enables or disables the menu bar.
* **`statusbar`** → Enables or disables the status bar.
* **`fontFormats`** → Defines available font families.
* **`uploadTarget`** → Defines the file upload target.

For more advanced customization, refer to the **TinyMCE documentation**.
