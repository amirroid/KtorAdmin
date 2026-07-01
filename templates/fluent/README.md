# KtorAdmin Fluent Template

A [Microsoft Fluent UI](https://fluent2.microsoft.design/) themed admin template for
[KtorAdmin](https://github.com/Amirroid/KtorAdmin).

It renders every KtorAdmin view (dashboard, list, upsert/confirmation pages for both JDBC and MongoDB,
and the login page) with a clean, Fluent-inspired look. Colors, typography, shapes, spacing, sidebar,
header and animations are all customizable, and a built-in dark mode is included.

## Installation

Add the KtorAdmin core library and the Fluent template to your dependencies:

```kotlin
dependencies {
    implementation("io.github.amirroid:KtorAdmin:<version>")
    ksp("io.github.amirroid:KtorAdmin:<version>")

    // Fluent template
    implementation("ir.amirroid.ktoradmin.templates:fluent:<version>")
}
```

## Usage

Set the `template` inside the `KtorAdmin` plugin configuration:

```kotlin
import ir.amirroid.ktoradmin.plugins.KtorAdmin
import ir.amirroid.ktoradmin.templates.fluent.FluentAdminTemplate

fun Application.configureAdmin() {
    install(KtorAdmin) {
        template = FluentAdminTemplate()
    }
}
```

That's it — all admin pages are now rendered with the Fluent theme using its default settings.

## Customization

Pass a `FluentAdminTemplateSettings` instance to customize the appearance:

```kotlin
import ir.amirroid.ktoradmin.templates.fluent.FluentAdminTemplate
import ir.amirroid.ktoradmin.templates.fluent.FluentAdminTemplateSettings

template = FluentAdminTemplate(
    settings = FluentAdminTemplateSettings(
        colors = FluentAdminTemplateSettings.Colors(
            brandColor = "#0078D4",
            brandHoverColor = "#106EBE",
            brandPressedColor = "#005A9E",
        ),
        shapes = FluentAdminTemplateSettings.Shapes(
            borderRadiusMedium = "8px",
        ),
        header = FluentAdminTemplateSettings.HeaderStyle(
            content = FluentAdminTemplateSettings.HeaderContent.Text(
                prefix = "Ktor",
                text = "Admin",
            ),
        ),
    ),
)
```

### Settings overview

| Property          | Type              | Description                                                        |
|-------------------|-------------------|--------------------------------------------------------------------|
| `colors`          | `Colors`          | Color palette used in light mode.                                  |
| `darkModeColors`  | `Colors`          | Color palette used in dark mode (sensible Fluent defaults).        |
| `typography`      | `Typography`      | Font family and font scale.                                        |
| `shapes`          | `Shapes`          | Small / medium / large border radii.                               |
| `spacing`         | `Spacing`         | Body and content padding.                                          |
| `sidebar`         | `SidebarStyle`    | Sidebar width.                                                     |
| `header`          | `HeaderStyle`     | Header height and content (text or image).                         |
| `animations`      | `AnimationStyle`  | Enable/disable animations and configure transition timing.         |

#### Header content

The header can display either text or an image:

```kotlin
// Text header
header = FluentAdminTemplateSettings.HeaderStyle(
    content = FluentAdminTemplateSettings.HeaderContent.Text(
        prefix = "Ktor",
        text = "Admin",
    ),
)

// Image (logo) header
header = FluentAdminTemplateSettings.HeaderStyle(
    content = FluentAdminTemplateSettings.HeaderContent.Image(
        url = "/static/logo.png",
        altText = "My Company",
        height = "24px",
    ),
)
```

#### Typography

The default font is Segoe UI (`FluentFontFamily.SegoeUi`) with system fallbacks. You can supply any
KtorAdmin `FontFamily`:

```kotlin
typography = FluentAdminTemplateSettings.Typography(
    font = FontFamily.fromGoogleFonts("Roboto", weights = listOf(300, 400, 700)),
    fontScale = 1.0,
)
```

#### Dark mode

A dark palette is provided out of the box via `FluentAdminTemplateSettings.DefaultDarkModeColors`.
Override `darkModeColors` to tweak it:

```kotlin
darkModeColors = FluentAdminTemplateSettings.Colors(
    brandColor = "#5BA3D9",
    surfaceColor = "#1E1E1E",
    backgroundLayerColor = "#141414",
)
```

## License

Released under the **MIT License**, same as KtorAdmin.
