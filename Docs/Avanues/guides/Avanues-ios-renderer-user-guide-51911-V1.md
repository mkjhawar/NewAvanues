# iOS SwiftUI Renderer - User Guide

**Version:** 1.0.0
**Last Updated:** 2025-11-19

---

## What is the iOS Renderer?

The iOS SwiftUI Renderer is part of the Avanues/AVAMagic cross-platform UI framework. It converts your UI components into native SwiftUI views, giving you:

- **Native performance** - Renders as real SwiftUI, not WebView
- **Platform consistency** - Same code renders on Android, iOS, and Web
- **Material 3 Expressive** - Modern design system support
- **Voice-first** - Ready for VoiceOS/AVA integration

---

## Getting Started

### Step 1: Add Dependency

In your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.augmentalis.avanues:avamagic-ios-renderer:1.0.0")
}
```

### Step 2: Create Your First UI

```kotlin
import com.augmentalis.avaelements.renderer.ios.*
import com.augmentalis.avaelements.core.*

fun createWelcomeScreen(): SwiftUIView {
    val renderer = OptimizedSwiftUIRenderer.withMaterial3()

    val ui = ColumnComponent(
        children = listOf(
            TextComponent(
                text = "Welcome to Avanues",
                style = TextStyle.Headline
            ),
            TextComponent(
                text = "Cross-platform UI made easy",
                style = TextStyle.Body
            ),
            ButtonComponent(
                label = "Get Started",
                onClick = { /* Navigate */ }
            )
        )
    )

    return renderer.render(ui) as SwiftUIView
}
```

### Step 3: Display in Swift

```swift
import SwiftUI

struct ContentView: View {
    let welcomeScreen = createWelcomeScreen()

    var body: some View {
        AvaElementsView(component: welcomeScreen)
    }
}
```

---

## Available Components

### Basic Components (8)
- Text, Button, TextField, Checkbox, Switch, Icon, Image, Divider

### Layout Components (15)
- Column, Row, Container, Card, ScrollView, Grid, Stack, Box, Surface, Spacer, LazyColumn, LazyRow, Scaffold, ListTile, Drawer

### Input Components (20)
- TextField, Checkbox, Switch, Radio, RadioGroup, Slider, DatePicker, TimePicker, Dropdown, SearchBar, Autocomplete, FileUpload, MultiSelect, DateRangePicker, TagInput, Toggle, ToggleButtonGroup, Stepper, IconPicker, Rating, ColorPicker

### Display Components (15)
- Text, Icon, Image, Badge, Chip, Avatar, Skeleton, Spinner, ProgressBar, ProgressCircle, Tooltip, StatCard, EmptyState, Paper

### Navigation Components (8)
- AppBar, BottomNav, Tabs, TabBar, Breadcrumb, Pagination, NavigationDrawer, NavigationRail

### Feedback Components (12)
- Alert, Toast, Snackbar, Modal, Dialog, Banner, BottomSheet, LoadingDialog, Confirm, ContextMenu, NotificationCenter

### Data Components (5)
- Accordion, Timeline, DataGrid, TreeView, Table

### Button Variants (6)
- Button, TextButton, OutlinedButton, FilledButton, IconButton, SegmentedButton, FAB

**Total: 81+ Components**

---

## Theming

### Built-in Themes

```kotlin
// Material Design 3 (Default)
val renderer = OptimizedSwiftUIRenderer.withMaterial3()

// iOS 26 Liquid Glass
val renderer = OptimizedSwiftUIRenderer.withLiquidGlass()

// Custom theme
renderer.applyTheme(myCustomTheme)
```

### Theme Features

- **Material 3 Expressive**: 35 expressive shapes, motion springs, dynamic color
- **Dark Mode**: Automatic dark mode support
- **Accessibility**: High contrast and reduced motion support

---

## Common Patterns

### Creating a Form

```kotlin
val form = ColumnComponent(
    spacing = 16f,
    children = listOf(
        TextFieldComponent(
            label = "Email",
            placeholder = "Enter your email"
        ),
        TextFieldComponent(
            label = "Password",
            isSecure = true
        ),
        ButtonComponent(
            label = "Sign In",
            variant = ButtonVariant.Filled
        )
    )
)
```

### Creating a Card

```kotlin
val card = CardComponent(
    elevation = 4,
    children = listOf(
        ImageComponent(src = "header.jpg"),
        TextComponent(text = "Card Title", style = TextStyle.Headline),
        TextComponent(text = "Card description goes here"),
        RowComponent(
            children = listOf(
                TextButtonComponent(label = "Cancel"),
                FilledButtonComponent(label = "Confirm")
            )
        )
    )
)
```

### Creating a List

```kotlin
val list = LazyColumnComponent(
    items = users.map { user ->
        ListTileComponent(
            leading = AvatarComponent(initials = user.initials),
            title = user.name,
            subtitle = user.email,
            trailing = IconButtonComponent(icon = "chevron.right")
        )
    }
)
```

### Showing a Dialog

```kotlin
val dialog = DialogComponent(
    title = "Confirm Action",
    content = TextComponent(text = "Are you sure you want to proceed?"),
    confirmButton = "Yes",
    dismissButton = "No",
    isVisible = showDialog
)
```

---

## Performance Tips

### DO

1. **Use OptimizedSwiftUIRenderer** - 2-3x faster than standard renderer
2. **Reuse renderer instances** - Don't create new renderer per component
3. **Batch render lists** - Use `renderBatch()` for multiple items
4. **Enable caching** - Default on, leave it enabled

### DON'T

1. **Create renderers in loops** - Expensive to initialize
2. **Re-render unchanged components** - Cache handles this
3. **Nest too deeply** - Keep hierarchy under 10 levels
4. **Use Custom ViewType unnecessarily** - Stick to built-in types

---

## Troubleshooting

### Component Not Rendering

**Symptom:** Empty view or "Unknown component" error

**Solution:**
1. Check component type is supported
2. Ensure proper imports
3. Verify mapper is registered (for custom components)

### Styling Not Applied

**Symptom:** Component appears but without expected styling

**Solution:**
1. Check modifier syntax
2. Verify theme is applied
3. Ensure colors are in 0-1 range (not 0-255)

### Poor Performance

**Symptom:** UI feels sluggish or drops frames

**Solution:**
1. Switch to OptimizedSwiftUIRenderer
2. Check cache hit rate (should be >70%)
3. Reduce component hierarchy depth
4. Use LazyColumn/LazyRow for lists

---

## Examples

### Dashboard Screen

```kotlin
fun createDashboard(): SwiftUIView {
    val renderer = OptimizedSwiftUIRenderer.withMaterial3()

    return renderer.render(
        ScaffoldComponent(
            topBar = AppBarComponent(title = "Dashboard"),
            content = ColumnComponent(
                children = listOf(
                    // Stats row
                    RowComponent(
                        children = listOf(
                            StatCardComponent(
                                label = "Users",
                                value = "1,234",
                                trend = TrendDirection.Up,
                                trendValue = 12
                            ),
                            StatCardComponent(
                                label = "Revenue",
                                value = "$45,678",
                                trend = TrendDirection.Up,
                                trendValue = 8
                            )
                        )
                    ),
                    // Recent activity
                    CardComponent(
                        children = listOf(
                            TextComponent(text = "Recent Activity"),
                            TimelineComponent(
                                items = recentEvents
                            )
                        )
                    )
                )
            ),
            bottomBar = BottomNavComponent(
                items = listOf(
                    BottomNavItem("Home", "house"),
                    BottomNavItem("Search", "magnifyingglass"),
                    BottomNavItem("Profile", "person")
                )
            )
        )
    ) as SwiftUIView
}
```

### Settings Screen

```kotlin
fun createSettings(): SwiftUIView {
    val renderer = OptimizedSwiftUIRenderer.withMaterial3()

    return renderer.render(
        LazyColumnComponent(
            items = listOf(
                // Account section
                ListTileComponent(
                    leading = IconComponent("person.circle"),
                    title = "Account",
                    trailing = IconComponent("chevron.right")
                ),
                // Notifications toggle
                ToggleComponent(
                    label = "Notifications",
                    description = "Receive push notifications",
                    checked = true
                ),
                // Theme selection
                ListTileComponent(
                    leading = IconComponent("paintbrush"),
                    title = "Theme",
                    subtitle = "Material 3 Light",
                    trailing = IconComponent("chevron.right")
                ),
                // Volume slider
                SliderComponent(
                    label = "Volume",
                    value = 0.7f,
                    min = 0f,
                    max = 1f
                )
            )
        )
    ) as SwiftUIView
}
```

---

## FAQ

**Q: Can I use custom SwiftUI views?**
A: Yes, use `ViewType.Custom("MyViewName")` and handle it in Swift.

**Q: How do I handle actions/callbacks?**
A: Actions are passed as strings in properties. Handle them in your Swift code.

**Q: Does it support animations?**
A: SwiftUI animations work normally. Use SwiftUI's animation modifiers in Swift.

**Q: Can I mix with native SwiftUI?**
A: Yes! SwiftUIView is just data. You can wrap it or combine with native views.

**Q: What iOS versions are supported?**
A: iOS 15.0+ (SwiftUI 3.0+)

---

## Support

- **Documentation:** `/docs/guides/`
- **Issues:** Report at project repository
- **Contact:** manoj@ideahq.net

---

**Created by:** Manoj Jhawar (manoj@ideahq.net)
**IDEACODE Version:** 8.4
