# AvaElements User Manual

**Version:** 2.0.0
**Last Updated:** 2025-11-13
**Target Audience:** App Developers, UI Designers

---

## Welcome to AvaElements! 🎨

AvaElements is a modern, declarative UI framework that lets you build beautiful, cross-platform applications using Kotlin. Think of it as Flutter or SwiftUI, but with the power and flexibility of Kotlin Multiplatform.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Basic Concepts](#basic-concepts)
3. [Building Your First App](#building-your-first-app)
4. [Component Gallery](#component-gallery)
5. [Styling & Theming](#styling--theming)
6. [Handling User Input](#handling-user-input)
7. [Working with Assets](#working-with-assets)
8. [Common Patterns](#common-patterns)
9. [Troubleshooting](#troubleshooting)
10. [FAQ](#faq)

---

## 1. Quick Start

### Installation (5 minutes)

**Step 1:** Add AvaElements to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":modules:MagicIdea:UI:Core"))
    implementation(project(":modules:MagicIdea:Components:Renderers:Android"))
    implementation(project(":modules:MagicIdea:Components:AssetManager:AssetManager"))
}
```

**Step 2:** Create your first screen:

```kotlin
import com.augmentalis.avamagic.ui.core.layout.ColumnComponent
import com.augmentalis.avamagic.ui.core.display.TextComponent

@Composable
fun MyFirstScreen() {
    val renderer = ComposeRenderer()

    val ui = ColumnComponent(
        children = listOf(
            TextComponent(text = "Hello, AvaElements!")
        )
    )

    renderer.render(ui)()
}
```

**Step 3:** Run your app! 🚀

---

## 2. Basic Concepts

### What is a Component?

A **component** is a building block of your UI. Just like LEGO bricks, you combine small components to create complex interfaces.

```kotlin
// Simple component
TextComponent(text = "Welcome!")

// Nested components
ColumnComponent(
    children = listOf(
        TextComponent(text = "Title"),
        TextComponent(text = "Subtitle")
    )
)
```

### The Component Tree

Your UI is a tree of components:

```
ColumnComponent (root)
├── TextComponent ("Welcome")
├── ButtonComponent ("Get Started")
└── RowComponent
    ├── IconComponent (star)
    └── TextComponent ("Rating: 5.0")
```

### Declarative vs Imperative

**Imperative (old way):**
```kotlin
val textView = TextView(context)
textView.text = "Hello"
textView.setTextColor(Color.BLUE)
layout.addView(textView)
```

**Declarative (AvaElements way):**
```kotlin
TextComponent(
    text = "Hello",
    color = Color.Blue
)
```

---

## 3. Building Your First App

### Example: Todo List App

```kotlin
@Composable
fun TodoApp() {
    var todos by remember { mutableStateOf(listOf("Buy milk", "Walk dog")) }
    var newTodo by remember { mutableStateOf("") }

    val renderer = ComposeRenderer()

    val ui = ColumnComponent(
        children = listOf(
            // Header
            TextComponent(
                text = "My Todo List",
                style = MaterialTheme.typography.headlineMedium
            ),

            // Input field
            RowComponent(
                children = listOf(
                    TextFieldComponent(
                        value = newTodo,
                        onValueChange = { newTodo = it },
                        label = "New todo",
                        modifiers = listOf(Modifier.weight(1f))
                    ),
                    ButtonComponent(
                        text = "Add",
                        onClick = {
                            if (newTodo.isNotBlank()) {
                                todos = todos + newTodo
                                newTodo = ""
                            }
                        }
                    )
                )
            ),

            // Todo list
            ListComponent(
                items = todos.map { todo ->
                    RowComponent(
                        children = listOf(
                            CheckboxComponent(
                                checked = false,
                                onCheckedChange = { /* mark complete */ }
                            ),
                            TextComponent(text = todo)
                        )
                    )
                }
            )
        )
    )

    renderer.render(ui)()
}
```

---

## 4. Component Gallery

### Text Components

#### Basic Text

```kotlin
TextComponent(text = "Hello, World!")
```

**Output:** Hello, World!

#### Styled Text

```kotlin
TextComponent(
    text = "Important Message",
    style = MaterialTheme.typography.headlineLarge,
    color = Color.Red
)
```

**Output:** **Important Message** (large, red)

#### Multiline Text

```kotlin
TextComponent(
    text = "This is a very long text that will wrap to multiple lines automatically",
    maxLines = 3,
    overflow = TextOverflow.Ellipsis
)
```

### Button Components

#### Standard Button

```kotlin
ButtonComponent(
    text = "Click Me",
    onClick = { println("Button clicked!") }
)
```

#### Button with Icon

```kotlin
ButtonComponent(
    text = "Favorite",
    icon = "favorite",
    onClick = { /* add to favorites */ }
)
```

#### Disabled Button

```kotlin
ButtonComponent(
    text = "Submit",
    enabled = false
)
```

### Input Components

#### Text Field

```kotlin
var name by remember { mutableStateOf("") }

TextFieldComponent(
    value = name,
    onValueChange = { name = it },
    label = "Enter your name",
    placeholder = "John Doe"
)
```

#### Checkbox

```kotlin
var agreed by remember { mutableStateOf(false) }

CheckboxComponent(
    checked = agreed,
    onCheckedChange = { agreed = it },
    label = "I agree to the terms"
)
```

#### Switch

```kotlin
var notificationsEnabled by remember { mutableStateOf(true) }

SwitchComponent(
    checked = notificationsEnabled,
    onCheckedChange = { notificationsEnabled = it },
    label = "Enable notifications"
)
```

#### Slider

```kotlin
var volume by remember { mutableStateOf(50f) }

SliderComponent(
    value = volume,
    onValueChange = { volume = it },
    valueRange = 0f..100f,
    showLabel = true,
    labelFormatter = { "${it.toInt()}%" }
)
```

#### Radio Buttons

```kotlin
var selectedOption by remember { mutableStateOf("Option1") }

RadioComponent(
    options = listOf("Option1", "Option2", "Option3"),
    selectedOption = selectedOption,
    onOptionSelected = { selectedOption = it },
    orientation = Orientation.Vertical
)
```

#### Dropdown

```kotlin
var selectedCity by remember { mutableStateOf("") }

DropdownComponent(
    options = listOf("New York", "London", "Tokyo"),
    selectedOption = selectedCity,
    onOptionSelected = { selectedCity = it },
    label = "Select a city"
)
```

### Layout Components

#### Column (Vertical Stack)

```kotlin
ColumnComponent(
    children = listOf(
        TextComponent(text = "Item 1"),
        TextComponent(text = "Item 2"),
        TextComponent(text = "Item 3")
    ),
    spacing = 16.dp
)
```

**Output:**
```
Item 1
Item 2
Item 3
```

#### Row (Horizontal Stack)

```kotlin
RowComponent(
    children = listOf(
        IconComponent(name = "star"),
        TextComponent(text = "5.0"),
        TextComponent(text = "(123 reviews)")
    ),
    spacing = 8.dp
)
```

**Output:** ⭐ 5.0 (123 reviews)

#### Card

```kotlin
CardComponent(
    elevation = 4.dp,
    shape = RoundedCornerShape(12.dp),
    children = listOf(
        TextComponent(text = "Card Title"),
        TextComponent(text = "Card content goes here")
    )
)
```

#### Container (Box)

```kotlin
ContainerComponent(
    padding = 16.dp,
    backgroundColor = Color.LightGray,
    child = TextComponent(text = "Padded content")
)
```

### Feedback Components

#### Dialog

```kotlin
var showDialog by remember { mutableStateOf(false) }

if (showDialog) {
    DialogComponent(
        isOpen = true,
        dismissible = true,
        onDismiss = { showDialog = false },
        title = "Confirm Action",
        content = TextComponent(text = "Are you sure you want to proceed?"),
        actions = listOf(
            DialogAction(label = "Cancel", onClick = { showDialog = false }),
            DialogAction(label = "Confirm", onClick = { /* confirm action */ })
        )
    )
}
```

#### Toast/Snackbar

```kotlin
ToastComponent(
    message = "Item added to cart",
    duration = 3000L,
    position = ToastPosition.Bottom
)
```

#### Progress Bar

```kotlin
// Determinate (with progress value)
ProgressBarComponent(
    value = 0.75f,  // 75%
    showLabel = true
)

// Indeterminate (loading)
ProgressBarComponent(
    indeterminate = true
)
```

### Display Components

#### Icon

```kotlin
IconComponent(
    name = "favorite",  // Material icon
    size = 32.dp,
    tint = Color.Red
)
```

Available icons: 2,235 Material Design icons (search: MaterialIconsLibrary.searchIcons("search term"))

#### Image

```kotlin
ImageComponent(
    source = "logo",  // Asset name
    contentDescription = "Company logo",
    contentScale = ContentScale.Fit
)
```

#### Avatar

```kotlin
AvatarComponent(
    text = "JD",  // Initials
    size = AvatarSize.Large,
    shape = AvatarShape.Circle
)
```

#### Badge

```kotlin
BadgeComponent(
    content = "5"  // Notification count
)
```

#### Chip

```kotlin
ChipComponent(
    label = "Technology",
    selected = true,
    deletable = true,
    onClick = { /* handle selection */ },
    onDelete = { /* remove chip */ }
)
```

---

## 5. Styling & Theming

### Using Modifiers

Modifiers let you adjust the appearance and behavior of components:

```kotlin
TextComponent(
    text = "Styled Text",
    modifiers = listOf(
        Modifier.padding(16.dp),
        Modifier.background(Color.LightGray),
        Modifier.width(200.dp),
        Modifier.height(50.dp),
        Modifier.clickable { /* handle click */ }
    )
)
```

### Common Modifiers

```kotlin
// Size
Modifier.size(100.dp)
Modifier.width(200.dp)
Modifier.height(150.dp)
Modifier.fillMaxWidth()
Modifier.fillMaxHeight()
Modifier.fillMaxSize()

// Spacing
Modifier.padding(16.dp)
Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
Modifier.margin(8.dp)

// Background
Modifier.background(Color.Blue)
Modifier.background(MaterialTheme.colorScheme.primary)

// Border
Modifier.border(2.dp, Color.Black)

// Shape
Modifier.clip(RoundedCornerShape(8.dp))
Modifier.clip(CircleShape)

// Interaction
Modifier.clickable { /* action */ }
Modifier.enabled(false)
```

### Using Theme Colors

```kotlin
TextComponent(
    text = "Primary Color Text",
    color = MaterialTheme.colorScheme.primary
)

ButtonComponent(
    text = "Secondary Button",
    backgroundColor = MaterialTheme.colorScheme.secondary
)

CardComponent(
    backgroundColor = MaterialTheme.colorScheme.surface
)
```

### Custom Theme

```kotlin
val customColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC6),
    background = Color(0xFFF5F5F5)
)

MaterialTheme(colorScheme = customColors) {
    MyApp()
}
```

---

## 6. Handling User Input

### Text Input

```kotlin
var email by remember { mutableStateOf("") }
var isValid by remember(email) { mutableStateOf(email.contains("@")) }

TextFieldComponent(
    value = email,
    onValueChange = { email = it },
    label = "Email",
    enabled = true,
    modifiers = listOf(
        if (!isValid) Modifier.border(1.dp, Color.Red) else Modifier
    )
)

if (!isValid) {
    TextComponent(
        text = "Please enter a valid email",
        color = Color.Red
    )
}
```

### Button Actions

```kotlin
var count by remember { mutableStateOf(0) }

ColumnComponent(
    children = listOf(
        TextComponent(text = "Count: $count"),
        ButtonComponent(
            text = "Increment",
            onClick = { count++ }
        ),
        ButtonComponent(
            text = "Reset",
            onClick = { count = 0 }
        )
    )
)
```

### Form Validation

```kotlin
var username by remember { mutableStateOf("") }
var password by remember { mutableStateOf("") }
var canSubmit by remember(username, password) {
    mutableStateOf(username.length >= 3 && password.length >= 8)
}

ColumnComponent(
    children = listOf(
        TextFieldComponent(
            value = username,
            onValueChange = { username = it },
            label = "Username (min 3 characters)"
        ),
        TextFieldComponent(
            value = password,
            onValueChange = { password = it },
            label = "Password (min 8 characters)"
        ),
        ButtonComponent(
            text = "Submit",
            enabled = canSubmit,
            onClick = { /* submit form */ }
        )
    )
)
```

---

## 7. Working with Assets

### Material Icons (2,235 available)

```kotlin
// Search for icons
val heartIcons = MaterialIconsLibrary.searchIcons("heart")
// Returns: favorite, favorite_border, heart_broken, etc.

// Use icon
IconComponent(name = "favorite")
IconComponent(name = "star")
IconComponent(name = "home")
IconComponent(name = "settings")
IconComponent(name = "search")
```

### Popular Icon Categories

**Actions:**
- favorite, delete, search, home, settings, menu, close, add, remove

**Communication:**
- phone, email, chat, message, call, contact_phone

**Content:**
- save, edit, copy, paste, link, flag, undo, redo

**Navigation:**
- arrow_back, arrow_forward, menu, more_vert, refresh, expand_more

**Social:**
- share, thumb_up, thumb_down, person, group, star_rate

### Loading Images

```kotlin
// From assets
ImageComponent(
    source = "my_image.png",
    contentDescription = "Description"
)

// From URL (requires network permission)
ImageComponent(
    source = "https://example.com/image.jpg",
    contentDescription = "Remote image"
)
```

---

## 8. Common Patterns

### List of Items

```kotlin
val items = listOf("Apple", "Banana", "Cherry")

ListComponent(
    items = items.map { fruit ->
        RowComponent(
            children = listOf(
                IconComponent(name = "check"),
                TextComponent(text = fruit)
            )
        )
    },
    divider = true
)
```

### Loading State

```kotlin
var isLoading by remember { mutableStateOf(true) }
var data by remember { mutableStateOf<List<String>>(emptyList()) }

LaunchedEffect(Unit) {
    delay(2000)  // Simulate network call
    data = listOf("Item 1", "Item 2", "Item 3")
    isLoading = false
}

if (isLoading) {
    ProgressBarComponent(indeterminate = true)
} else {
    ListComponent(items = data.map { TextComponent(text = it) })
}
```

### Conditional UI

```kotlin
var isLoggedIn by remember { mutableStateOf(false) }

ColumnComponent(
    children = if (isLoggedIn) {
        listOf(
            TextComponent(text = "Welcome back!"),
            ButtonComponent(text = "Logout", onClick = { isLoggedIn = false })
        )
    } else {
        listOf(
            TextComponent(text = "Please log in"),
            ButtonComponent(text = "Login", onClick = { isLoggedIn = true })
        )
    }
)
```

### Master-Detail Layout

```kotlin
var selectedItem by remember { mutableStateOf<String?>(null) }

RowComponent(
    children = listOf(
        // Master list
        ListComponent(
            items = listOf("Item 1", "Item 2", "Item 3").map { item ->
                ButtonComponent(
                    text = item,
                    onClick = { selectedItem = item }
                )
            },
            modifiers = listOf(Modifier.weight(0.3f))
        ),

        // Detail view
        ContainerComponent(
            child = selectedItem?.let { item ->
                ColumnComponent(
                    children = listOf(
                        TextComponent(text = "Details for $item"),
                        TextComponent(text = "Description here...")
                    )
                )
            } ?: TextComponent(text = "Select an item"),
            modifiers = listOf(Modifier.weight(0.7f))
        )
    )
)
```

---

## 9. Troubleshooting

### Common Issues

**Issue:** Component not appearing
```kotlin
// ❌ Wrong - missing renderer
val ui = TextComponent(text = "Hello")

// ✅ Correct
val renderer = ComposeRenderer()
renderer.render(ui)()
```

**Issue:** Click handler not working
```kotlin
// ❌ Wrong - forgetting to invoke
ButtonComponent(text = "Click", onClick = println("Clicked"))

// ✅ Correct
ButtonComponent(text = "Click", onClick = { println("Clicked") })
```

**Issue:** State not updating UI
```kotlin
// ❌ Wrong - not using remember
var count = 0
ButtonComponent(text = "Count: $count", onClick = { count++ })

// ✅ Correct
var count by remember { mutableStateOf(0) }
ButtonComponent(text = "Count: $count", onClick = { count++ })
```

**Issue:** Icons not showing
```kotlin
// ❌ Wrong - typo in icon name
IconComponent(name = "favrite")

// ✅ Correct - use searchIcons to find correct name
val icons = MaterialIconsLibrary.searchIcons("favorite")
IconComponent(name = "favorite")
```

---

## 10. FAQ

**Q: Can I use AvaElements with XML layouts?**
A: No, AvaElements is a pure Compose-based framework. It replaces XML layouts entirely.

**Q: How do I migrate from XML to AvaElements?**
A: Start by converting one screen at a time. Each XML element has a AvaElements equivalent (TextView → TextComponent, Button → ButtonComponent, etc.).

**Q: Does AvaElements work on iOS?**
A: iOS support is planned! Currently Android-only, but the component definitions are platform-agnostic.

**Q: How many icons are available?**
A: 2,235 Material Design icons are included. Font Awesome support coming soon (+1,500 icons).

**Q: Can I create custom components?**
A: Yes! See the Developer Manual for component creation guidelines.

**Q: Is hot reload supported?**
A: Yes! Changes to themes and some component properties apply immediately during development.

**Q: How does AvaElements compare to Jetpack Compose?**
A: AvaElements uses Compose under the hood but provides a simpler, cross-platform API similar to Flutter.

**Q: Can I mix AvaElements with native Compose?**
A: Yes, you can embed native Compose within AvaElements components.

**Q: What's the minimum Android version?**
A: Android 8.0 (API 26+)

**Q: Is AvaElements production-ready?**
A: Phase 1 components are stable and tested. Phase 2/3 components are in development.

---

## Next Steps

✅ Read the [Developer Manual](./MAGICELEMENTS-DEVELOPER-MANUAL.md) for advanced topics
✅ Browse the [Component Gallery Examples](./examples/)
✅ Join the community on Discord
✅ Check out sample apps in `/apps/`

---

## Support & Resources

- 📧 **Email:** manoj@ideahq.net
- 📚 **Documentation:** `/docs/`
- 🐛 **Bug Reports:** GitHub Issues
- 💬 **Community:** Discord Server

---

**Happy Coding! 🚀**

*Version 2.0.0 | Last Updated: 2025-11-13 | Author: Manoj Jhawar*
