# AvaElements Android Renderer - Deep Dive

**Version:** 2.0.0
**Last Updated:** 2025-11-13
**Target:** Android SDK 24+ (Android 7.0 Nougat and later)
**Framework:** Jetpack Compose with Material3

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [ComposeRenderer Class](#composerenderer-class)
3. [Mapper Functions](#mapper-functions)
4. [Theme Integration](#theme-integration)
5. [Material3 Components](#material3-components)
6. [State Management](#state-management)
7. [Icon System](#icon-system)
8. [Error Handling](#error-handling)
9. [Performance Optimization](#performance-optimization)
10. [Testing Strategies](#testing-strategies)

---

## Architecture Overview

The Android renderer translates platform-agnostic AvaElements components into Jetpack Compose UI.

### Rendering Flow

```
┌────────────────────────────────────┐
│   AvaElements Component          │
│   (Platform-agnostic data class)   │
└──────────────┬─────────────────────┘
               │
               │ component.render(renderer)
               ▼
┌────────────────────────────────────┐
│   ComposeRenderer                  │
│   (Android-specific renderer)      │
└──────────────┬─────────────────────┘
               │
               │ renderer.render(component)
               ▼
┌────────────────────────────────────┐
│   Mapper Function                  │
│   Render{Component}(c, theme)      │
└──────────────┬─────────────────────┘
               │
               │ Returns @Composable
               ▼
┌────────────────────────────────────┐
│   Jetpack Compose UI               │
│   (Material3 components)           │
└────────────────────────────────────┘
```

### Key Files

**Core Renderer:**
```
Universal/Libraries/AvaElements/renderers/android/
├── src/androidMain/kotlin/com/augmentalis/avaelements/renderers/android/
│   ├── ComposeRenderer.kt                    # Main renderer class
│   ├── ThemeConverter.kt                     # Theme conversion utilities
│   ├── ModifierConverter.kt                  # Modifier conversion
│   └── mappers/
│       ├── Phase1Mappers.kt                  # 13 foundation mappers
│       ├── Phase3InputMappers.kt             # 12 input/form mappers
│       ├── Phase3DisplayMappers.kt           # 8 display mappers
│       ├── Phase3NavigationMappers.kt        # 4 navigation mappers
│       ├── Phase3LayoutMappers.kt            # 5 layout mappers
│       └── Phase3FeedbackMappers.kt          # 7 feedback mappers (pending)
└── build.gradle.kts                          # Dependencies
```

---

## ComposeRenderer Class

### Implementation

```kotlin
package com.augmentalis.avaelements.renderers.android

import androidx.compose.runtime.Composable
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.phase1.*
import com.augmentalis.avaelements.components.phase3.*
import com.augmentalis.avaelements.renderers.android.mappers.*

class ComposeRenderer(
    override val theme: Theme = ThemeProvider.getCurrentTheme()
) : Renderer {

    @Composable
    override fun render(component: Component): @Composable (() -> Unit) {
        return {
            when (component) {
                // Phase 1: Foundation components
                is com.augmentalis.avaelements.components.phase1.basic.Text ->
                    RenderText(component, theme)
                is com.augmentalis.avaelements.components.phase1.form.Button ->
                    RenderButton(component, theme)
                is com.augmentalis.avaelements.components.phase1.form.TextField ->
                    RenderTextField(component, theme)

                // Phase 3: Input components
                is com.augmentalis.avaelements.components.phase3.form.Slider ->
                    RenderSlider(component, theme)
                is com.augmentalis.avaelements.components.phase3.form.DatePicker ->
                    RenderDatePicker(component, theme)

                // Phase 3: Display components
                is com.augmentalis.avaelements.components.phase3.display.Badge ->
                    RenderBadge(component, theme)
                is com.augmentalis.avaelements.components.phase3.display.Skeleton ->
                    RenderSkeleton(component, theme)

                // ... more component mappings

                else -> {
                    // Fallback for unmapped components
                    androidx.compose.material3.Text(
                        text = "Unsupported component: ${component::class.simpleName}"
                    )
                }
            }
        }
    }
}
```

### Usage in App

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.augmentalis.avaelements.renderers.android.ComposeRenderer
import com.augmentalis.avaelements.components.phase1.form.Button

@Composable
fun MyScreen() {
    MaterialTheme {
        val renderer = ComposeRenderer()

        val button = Button(
            text = "Click Me",
            onClick = { println("Clicked!") }
        )

        // Render the component
        button.render(renderer)()
    }
}
```

---

## Mapper Functions

Mapper functions convert AvaElements components to Compose UI.

### Mapper Signature

All mappers follow this pattern:

```kotlin
@Composable
fun Render{ComponentName}(c: {ComponentType}, theme: Theme) {
    // Compose UI implementation
}
```

### Example: Button Mapper

```kotlin
@Composable
fun RenderButton(c: Button, theme: Theme) = androidx.compose.material3.Button(
    onClick = c.onClick ?: {},
    enabled = c.enabled,
    colors = ButtonDefaults.buttonColors(
        containerColor = c.style?.backgroundColor?.toCompose()
            ?: theme.colorScheme.primary.toCompose(),
        contentColor = theme.colorScheme.onPrimary.toCompose(),
        disabledContainerColor = theme.colorScheme.surfaceVariant.toCompose(),
        disabledContentColor = theme.colorScheme.onSurfaceVariant.toCompose()
    )
) {
    Text(text = c.text)
}
```

**Key Patterns:**
1. **Null-safe callbacks:** `c.onClick ?: {}`
2. **Style fallback:** `c.style?.backgroundColor?.toCompose() ?: theme.colorScheme.primary.toCompose()`
3. **Theme integration:** Always use `theme.colorScheme.*` for colors
4. **Material3 defaults:** Use `ButtonDefaults`, `TextFieldDefaults`, etc.

---

## Theme Integration

### Theme Structure

```kotlin
data class Theme(
    val colorScheme: ColorScheme,
    val typography: Typography,
    val spacing: Spacing,
    val shapes: Shapes
)

data class ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val surface: Color,
    val onSurface: Color,
    val error: Color,
    val onError: Color,
    // ... more colors
)
```

### Color Conversion

```kotlin
// Extension function in ThemeConverter.kt
fun Color.toCompose(): androidx.compose.ui.graphics.Color {
    return androidx.compose.ui.graphics.Color(parseHex(this.value))
}

private fun parseHex(hex: String): Long {
    val cleanHex = hex.removePrefix("#")
    return when (cleanHex.length) {
        6 -> "FF$cleanHex".toLong(16) // RGB -> ARGB
        8 -> cleanHex.toLong(16)      // ARGB
        else -> 0xFF000000 // Fallback to black
    }
}
```

### Using Theme in Mappers

```kotlin
@Composable
fun RenderCard(c: Card, theme: Theme) = androidx.compose.material3.Card(
    colors = CardDefaults.cardColors(
        containerColor = c.style?.backgroundColor?.toCompose()
            ?: theme.colorScheme.surface.toCompose(),
        contentColor = theme.colorScheme.onSurface.toCompose()
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = (c.style?.elevation ?: 2f).dp
    )
) {
    Column(modifier = Modifier.padding(16.dp)) {
        c.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = theme.colorScheme.onSurface.toCompose()
            )
        }
        Text(
            text = c.content,
            color = theme.colorScheme.onSurface.toCompose()
        )
    }
}
```

---

## Material3 Components

### Material3 Best Practices

**1. Use Material3 APIs (not Material2)**

```kotlin
// ✅ CORRECT - Material3
import androidx.compose.material3.*

@Composable
fun RenderButton(c: Button, theme: Theme) = androidx.compose.material3.Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = theme.colorScheme.primary.toCompose()
    )
) { Text(text = c.text) }

// ❌ WRONG - Material2
import androidx.compose.material.*

@Composable
fun RenderButton(c: Button, theme: Theme) = androidx.compose.material.Button(
    colors = ButtonDefaults.buttonColors(
        backgroundColor = theme.colorScheme.primary.toCompose() // Wrong API
    )
) { Text(text = c.text) }
```

**2. Opt-in to Experimental APIs**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDatePicker(c: DatePicker, theme: Theme) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // DatePicker requires ExperimentalMaterial3Api
    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "OK")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}
```

**3. Use Component Defaults**

```kotlin
@Composable
fun RenderTextField(c: TextField, theme: Theme) = OutlinedTextField(
    value = c.value,
    onValueChange = c.onChange ?: {},
    label = c.label?.let { { Text(text = it) } },
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = theme.colorScheme.primary.toCompose(),
        unfocusedBorderColor = theme.colorScheme.outline.toCompose(),
        focusedLabelColor = theme.colorScheme.primary.toCompose(),
        unfocusedLabelColor = theme.colorScheme.onSurfaceVariant.toCompose()
    )
)
```

### Common Material3 Components

| AvaElements | Material3 Component | Key Properties |
|---------------|---------------------|----------------|
| Button | `Button` | `colors`, `enabled`, `onClick` |
| TextField | `OutlinedTextField` | `value`, `onValueChange`, `colors` |
| Card | `Card` | `colors`, `elevation`, `onClick` |
| Checkbox | `Checkbox` | `checked`, `onCheckedChange`, `colors` |
| Switch | `Switch` | `checked`, `onCheckedChange`, `colors` |
| Slider | `Slider` | `value`, `onValueChange`, `valueRange`, `steps` |
| Dropdown | `ExposedDropdownMenuBox` | `expanded`, `onExpandedChange` |
| AppBar | `TopAppBar` | `title`, `navigationIcon`, `colors` |
| BottomNav | `NavigationBar` | `NavigationBarItem` with `selected` |
| Modal | `Dialog` or `ModalBottomSheet` | `onDismissRequest`, `content` |

---

## State Management

### Using remember and mutableStateOf

**Problem:** Some Material3 components require state management.

**Solution:** Use `remember` and `mutableStateOf` in mappers.

### Example: Dropdown with Expanded State

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDropdown(c: Dropdown, theme: Theme) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(c.selectedValue ?: c.placeholder) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = c.placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            c.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        selectedText = option
                        expanded = false
                        c.onSelectionChange?.invoke(option)
                    }
                )
            }
        }
    }
}
```

### Example: Drawer with State Synchronization

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDrawer(c: Drawer, theme: Theme) {
    val drawerState = rememberDrawerState(
        initialValue = if (c.open) DrawerValue.Open else DrawerValue.Closed
    )

    // Synchronize external `c.open` with internal `drawerState`
    LaunchedEffect(c.open) {
        if (c.open && drawerState.isClosed) {
            drawerState.open()
        } else if (!c.open && drawerState.isOpen) {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Drawer content
            }
        }
    ) {
        // Main content
    }
}
```

---

## Icon System

### Material Icons

AvaElements Android renderer uses Material Icons from Jetpack Compose.

**Import Strategy:**

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
```

**Wildcard imports** are used to access all available icons.

### Using Icons in Mappers

```kotlin
@Composable
fun RenderRating(c: Rating, theme: Theme) {
    Row(modifier = Modifier.fillMaxWidth()) {
        repeat(c.maxRating) { index ->
            val filled = index < c.rating.toInt()
            IconButton(onClick = { c.onRatingChange?.invoke((index + 1).toFloat()) }) {
                Icon(
                    imageVector = if (filled)
                        Icons.Filled.Star
                    else
                        Icons.Outlined.Star,
                    contentDescription = "Star ${index + 1}",
                    tint = if (filled)
                        theme.colorScheme.primary.toCompose()
                    else
                        theme.colorScheme.outline.toCompose()
                )
            }
        }
    }
}
```

### Common Icon Pitfalls

**1. Icon doesn't exist**

```kotlin
// ❌ WRONG - StarBorder doesn't exist in Material Icons
Icon(imageVector = Icons.Filled.StarBorder, contentDescription = "Empty star")

// ✅ CORRECT - Use Icons.Outlined.Star instead
Icon(imageVector = Icons.Outlined.Star, contentDescription = "Empty star")
```

**2. Icon needs AutoMirrored**

```kotlin
// ❌ WRONG - ArrowBack should be auto-mirrored for RTL layouts
Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")

// ✅ CORRECT - Use Icons.AutoMirrored.Filled.ArrowBack
Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
```

### Available Icon Families

- `Icons.Filled.*` - Filled icons (default)
- `Icons.Outlined.*` - Outlined icons
- `Icons.Rounded.*` - Rounded icons
- `Icons.Sharp.*` - Sharp icons
- `Icons.TwoTone.*` - Two-tone icons
- `Icons.AutoMirrored.Filled.*` - Auto-mirrored filled icons (for RTL support)

---

## Error Handling

### Null Safety

Always handle nullable properties safely:

```kotlin
@Composable
fun RenderButton(c: Button, theme: Theme) = androidx.compose.material3.Button(
    onClick = c.onClick ?: {},  // Provide empty lambda if null
    enabled = c.enabled
) {
    Text(text = c.text)
}
```

### Unsupported Components

Handle unmapped components gracefully:

```kotlin
class ComposeRenderer(override val theme: Theme) : Renderer {
    @Composable
    override fun render(component: Component): @Composable (() -> Unit) {
        return {
            when (component) {
                is Button -> RenderButton(component, theme)
                // ... other mappings

                else -> {
                    // Fallback for unmapped components
                    Text(
                        text = "⚠️ Unsupported: ${component::class.simpleName}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
```

### API Version Checks

Some Material3 APIs are experimental:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderSearchBar(c: SearchBar, theme: Theme) {
    // SearchBar requires ExperimentalMaterial3Api
    var query by remember { mutableStateOf(c.query) }

    androidx.compose.material3.SearchBar(
        query = query,
        onQueryChange = { newQuery ->
            query = newQuery
            c.onQueryChange?.invoke(newQuery)
        },
        onSearch = { c.onQueryChange?.invoke(it) },
        active = false,
        onActiveChange = {},
        placeholder = { Text(text = c.placeholder) }
    ) {
        // Search results
    }
}
```

---

## Performance Optimization

### 1. Avoid Recomposition

Use `remember` to cache expensive computations:

```kotlin
@Composable
fun RenderSkeleton(c: Skeleton, theme: Theme) {
    // Cache the shape calculation
    val shape = remember(c.variant) {
        when (c.variant) {
            "circular" -> CircleShape
            "rounded" -> RoundedCornerShape(8.dp)
            else -> RectangleShape
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-alpha"
    )

    Box(
        modifier = Modifier
            .size(c.width.dp, c.height.dp)
            .clip(shape)
            .background(theme.colorScheme.surfaceVariant.toCompose().copy(alpha = alpha))
    )
}
```

### 2. Use LazyColumn/LazyRow for Lists

For large lists, use lazy components:

```kotlin
@Composable
fun RenderList(items: List<String>, theme: Theme) {
    LazyColumn {
        items(items) { item ->
            Text(
                text = item,
                modifier = Modifier.padding(8.dp),
                color = theme.colorScheme.onSurface.toCompose()
            )
        }
    }
}
```

### 3. Minimize Modifier Chains

Keep modifier chains concise:

```kotlin
// ✅ GOOD - Concise modifier chain
Box(
    modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(theme.colorScheme.primary.toCompose())
)

// ❌ BAD - Redundant modifiers
Box(
    modifier = Modifier
        .size(48.dp)
        .then(Modifier.clip(CircleShape))
        .then(Modifier.background(theme.colorScheme.primary.toCompose()))
)
```

---

## Testing Strategies

### Unit Testing Mappers

Test mapper functions in isolation:

```kotlin
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ButtonMapperTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testButtonRenderingWithText() {
        val button = Button(text = "Click Me", onClick = {})
        val theme = ThemeProvider.getCurrentTheme()

        composeTestRule.setContent {
            RenderButton(button, theme)
        }

        composeTestRule.onNodeWithText("Click Me").assertExists()
    }

    @Test
    fun testButtonClick() {
        var clicked = false
        val button = Button(text = "Click Me", onClick = { clicked = true })
        val theme = ThemeProvider.getCurrentTheme()

        composeTestRule.setContent {
            RenderButton(button, theme)
        }

        composeTestRule.onNodeWithText("Click Me").performClick()
        assert(clicked)
    }
}
```

### Integration Testing

Test full component rendering flow:

```kotlin
@Test
fun testFullRenderingFlow() {
    val renderer = ComposeRenderer()
    val button = Button(text = "Submit", onClick = {})

    composeTestRule.setContent {
        button.render(renderer)()
    }

    composeTestRule.onNodeWithText("Submit").assertExists()
}
```

### Screenshot Testing

Use Paparazzi or similar tools for visual regression testing:

```kotlin
@Test
fun testButtonAppearance() {
    val button = Button(text = "Click Me", enabled = true)
    val theme = ThemeProvider.getCurrentTheme()

    paparazzi.snapshot {
        RenderButton(button, theme)
    }
}
```

---

## Common Pitfalls

### 1. RectangleShape Import

```kotlin
// ❌ WRONG
import androidx.compose.foundation.shape.RectangleShape

// ✅ CORRECT
import androidx.compose.ui.graphics.RectangleShape
```

### 2. Text Overload Resolution

```kotlin
// ❌ WRONG - Ambiguous
Text(c.placeholder)

// ✅ CORRECT - Explicit
Text(text = c.placeholder)
```

### 3. Component Property Assumptions

```kotlin
// ❌ WRONG - Assuming property exists
Slider(
    value = c.value,
    enabled = c.enabled // Property doesn't exist!
)

// ✅ CORRECT - Read component definition first
Slider(
    value = c.value,
    onValueChange = c.onValueChange ?: {}
)
```

### 4. tabIndicatorOffset Not Found

```kotlin
// ❌ WRONG
TabRowDefaults.SecondaryIndicator(
    modifier = Modifier.tabIndicatorOffset(tabPositions[c.selectedIndex])
)

// ✅ CORRECT
TabRowDefaults.SecondaryIndicator(
    modifier = Modifier.fillMaxWidth(),
    color = theme.colorScheme.primary.toCompose()
)
```

---

## Build Configuration

### build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()

    sourceSets {
        val androidMain by getting {
            dependencies {
                // AvaElements Core
                implementation(project(":Universal:Libraries:AvaElements:Core"))
                implementation(project(":Universal:Libraries:AvaElements:components:phase1"))
                implementation(project(":Universal:Libraries:AvaElements:components:phase3"))

                // Jetpack Compose
                implementation("androidx.compose.ui:ui:1.5.4")
                implementation("androidx.compose.material3:material3:1.2.0")
                implementation("androidx.compose.foundation:foundation:1.5.4")
                implementation("androidx.compose.runtime:runtime:1.5.4")

                // Material Icons
                implementation("androidx.compose.material:material-icons-core:1.5.4")
                implementation("androidx.compose.material:material-icons-extended:1.5.4")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avaelements.renderers.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
}
```

---

## Next Steps

📖 **Continue to:**
- [Chapter 04 - Theme System](./04-Theme-System.md) - Customize colors and styles
- [Chapter 05 - Building Custom Components](./05-Custom-Components.md) - Extend the framework

📚 **Tutorials:**
- [Tutorial 01 - Building a Login Screen](../tutorials/01-Login-Screen.md)
- [Tutorial 03 - Navigation Patterns](../tutorials/03-Navigation-Patterns.md)

---

**Version:** 2.0.0
**Status:** 29/49 Android mappers complete (59%)
**Framework:** AvaElements on Kotlin Multiplatform
**Methodology:** IDEACODE 7.2.0
**Author:** Manoj Jhawar (manoj@ideahq.net)
