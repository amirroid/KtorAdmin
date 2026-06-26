# Default Admin Panel Customization

KtorAdmin provides a **default admin template customization system** that allows you to theme the entire UI using a single Kotlin configuration object. It is built on CSS custom properties, so changes are applied globally without modifying any HTML or CSS files.

***

### DefaultAdminTemplateSettings

The `DefaultAdminTemplateSettings` class controls all visual aspects of the admin panel:

* `colors` – light mode color palette
* `darkModeColors` – dark mode color palette
* `typography` – font family and scaling
* `shapes` – border radius for UI elements
* `spacing` – layout spacing values
* `sidebar` – sidebar width and behavior
* `header` – header content and size
* `animations` – transition settings

***

### Basic Usage

```kotlin
install(KtorAdmin) {
    template = DefaultAdminTemplate(
        settings = DefaultAdminTemplateSettings(
            colors = DefaultAdminTemplateSettings.Colors(
                primaryColor = "#1E40AF",
                secondaryColor = "#3B82F6",
                highlightColor = "#60A5FA"
            ),
            darkModeColors = DefaultAdminTemplateSettings.Colors(
                primaryColor = "#93C5FD",
                secondaryColor = "#60A5FA",
                highlightColor = "#93C5FD"
            )
        )
    )
}
```

***

### Colors

Controls the main UI palette and table styling.

```kotlin
Colors(
    primaryColor = "#292D32",
    secondaryColor = "#9A6C00",
    backgroundGradientStart = "#F3E7CB",
    backgroundGradientEnd = "#E3E5E6",
    highlightColor = "#ffb300",
    errorColor = "red",
    evenRowColor = "rgba(0,0,0,0.05)",
    oddRowColor = "transparent",
    hoverRowColor = "rgba(0,0,0,0.1)"
)
```

***

### Typography

```kotlin
Typography(
    font = FontFamily(
        name = "Inter",
        cssFamily = "'Inter', sans-serif",
        regular = "/static/fonts/Inter-Regular.ttf",
        bold = "/static/fonts/Inter-Bold.ttf"
    ),
    fontScale = 1.0
)
```

***

### Shapes

```kotlin
Shapes(
    sidebarBorderRadius = "24px",
    menuItemBorderRadius = "16px",
    dropdownBorderRadius = "12px"
)
```

***

### Spacing

```kotlin
Spacing(
    bodyPadding = "16px",
    sidebarMargin = "16px"
)
```

***

### Sidebar

```kotlin
SidebarStyle(
    width = "300px",
    backdropBlur = "8px",
    backgroundOpacity = 0.7
)
```

Sidebar modes:

* Floating (overlay)
* Fixed (push layout)
* Compact (icons only)

***

### Header

Text:

```kotlin
HeaderStyle(
    content = HeaderContent.Text("My", "App"),
    height = "54px"
)
```

Image:

```kotlin
HeaderStyle(
    content = HeaderContent.Image(
        url = "/static/logo.svg",
        altText = "My App",
        height = "32px"
    ),
    height = "54px"
)
```

***

### Animations

```kotlin
AnimationStyle(
    enabled = true,
    transitionDuration = "0.3s",
    transitionTiming = "ease"
)
```
