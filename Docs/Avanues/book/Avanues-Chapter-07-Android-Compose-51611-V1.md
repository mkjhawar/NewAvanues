# Chapter 7: Android Jetpack Compose Bridge

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~6,000 words

---

## Overview

The Android platform bridge renders AvaUI components as Jetpack Compose Material3 UI. This chapter documents the implementation of the Compose renderer.

## Architecture

```
AvaUI AST                  Compose Bridge              Android UI
ComponentNode         →      ComposeUIImplementation   →  @Composable
  ├─ type: BUTTON            ├─ renderButton()             Button()
  ├─ properties              ├─ renderText()               Text()
  └─ eventHandlers           └─ renderTextField()          TextField()
```

## ComposeUIImplementation

**Location:** `Universal/IDEAMagic/Components/Adapters/src/androidMain/kotlin/ComposeUIImplementation.kt`

### Key Methods

```kotlin
class ComposeUIImplementation : UIImplementation {

    @Composable
    override fun renderComponent(component: ComponentNode) {
        when (component.type) {
            ComponentType.BUTTON -> renderButton(component)
            ComponentType.TEXT -> renderText(component)
            ComponentType.TEXT_FIELD -> renderTextField(component)
            ComponentType.CARD -> renderCard(component)
            ComponentType.COLUMN -> renderColumn(component)
            ComponentType.ROW -> renderRow(component)
            else -> renderGeneric(component)
        }
    }

    @Composable
    private fun renderButton(component: ComponentNode) {
        val text = component.properties["text"] as? String ?: "Button"
        val onClick = component.eventHandlers["onClick"]

        Button(
            onClick = { onClick?.invoke() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
        }
    }

    @Composable
    private fun renderTextField(component: ComponentNode) {
        val value = component.properties["value"] as? String ?: ""
        val label = component.properties["label"] as? String
        val onValueChange = component.eventHandlers["onValueChange"]

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange?.invoke(it) },
            label = label?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun renderColumn(component: ComponentNode) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            component.children.forEach { child ->
                renderComponent(child)
            }
        }
    }
}
```

## Material3 Integration

### Theme Mapping

```kotlin
@Composable
fun MagicTheme(
    theme: ThemeNode,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = Color(parseColor(theme.colors["primary"]!!)),
        secondary = Color(parseColor(theme.colors["secondary"]!!)),
        background = Color(parseColor(theme.colors["background"]!!)),
        surface = Color(parseColor(theme.colors["surface"]!!))
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = createTypography(theme.typography),
        shapes = createShapes(theme.shapes),
        content = content
    )
}
```

## State Management

```kotlin
@Composable
fun MagicScreen(screen: ScreenNode) {
    // Initialize state variables
    val stateValues = remember {
        mutableStateMapOf<String, Any?>().apply {
            screen.stateVariables.forEach { stateVar ->
                put(stateVar.name, stateVar.initialValue)
            }
        }
    }

    // Render screen
    renderComponent(screen.root)
}
```

## Generated Code Example

**Input JSON:**
```json
{
  "type": "COLUMN",
  "children": [
    {
      "type": "TEXT",
      "properties": { "content": "Login", "variant": "H1" }
    },
    {
      "type": "TEXT_FIELD",
      "properties": { "label": "Username" },
      "eventHandlers": { "onValueChange": "{ username = it }" }
    },
    {
      "type": "BUTTON",
      "properties": { "text": "Submit" },
      "eventHandlers": { "onClick": "{ handleLogin() }" }
    }
  ]
}
```

**Generated Compose Code:**
```kotlin
@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineLarge
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { handleLogin() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }
    }
}
```

## Platform-Specific Features

### Android-Only Components
- **FloatingActionButton** - Material floating action button
- **Scaffold** - Material design layout structure
- **TopAppBar** - Material top app bar
- **NavigationBar** - Material navigation bar
- **ModalBottomSheet** - Bottom sheet dialog

### Modifiers
Compose modifiers are applied based on component properties:
- `padding` → `Modifier.padding(dp)`
- `width` → `Modifier.width(dp)`
- `backgroundColor` → `Modifier.background(color)`
- `cornerRadius` → `Modifier.clip(RoundedCornerShape(dp))`

## Performance Optimizations

1. **Remember scoping** - Use `remember` for expensive computations
2. **Derived state** - Use `derivedStateOf` for computed values
3. **Key composition** - Use `key()` for stable list items
4. **Lazy layouts** - Use `LazyColumn`/`LazyRow` for lists

## Summary

The Android Compose bridge provides native Material3 rendering with:
- Full Material3 component support
- Theme customization
- State management integration
- Performance optimizations
- Type-safe property binding

**Next:** Chapter 8 covers iOS SwiftUI + Kotlin/Native integration.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
