# AvaElements Developer Manual

**Version:** 2.1.0
**Last Updated:** 2025-01-14
**Framework Status:** Phase 1 Complete, Phase 2 50% Complete, Universal Theming Complete

---

## Table of Contents

1. [Introduction](#introduction)
2. [Architecture Overview](#architecture-overview)
3. [Getting Started](#getting-started)
4. [Component Development](#component-development)
5. [Renderer Development](#renderer-development)
6. [Asset Management](#asset-management)
7. [Cross-Platform SQLite Storage](#cross-platform-sqlite-storage)
8. [Universal Theming System](#universal-theming-system)
9. [Testing Guidelines](#testing-guidelines)
10. [API Reference](#api-reference)
11. [Best Practices](#best-practices)

---

## 1. Introduction

AvaElements is a Kotlin Multiplatform declarative UI framework designed as a Flutter/SwiftUI alternative with 3D capabilities.

### Current Status

**Components:**
- **Phase 1 (Complete):** 13 core components
- **Phase 2 (50% Complete):** Advanced input & navigation components
- **Phase 3 (Planned):** 35 specialized components
- **Total Target:** 48+ components

**Renderers:**
- **Android:** 39 mappers (Material3)
- **iOS:** Planned (SwiftUI)
- **Web:** Planned (React)
- **Desktop:** Planned (Compose Desktop)

**Assets:**
- **Material Icons:** 2,235 icons
- **Font Awesome:** Planned (1,500+ icons)

### Key Features

✅ **Kotlin Multiplatform** - Share UI code across Android, iOS, Web, Desktop
✅ **Declarative DSL** - Flutter/SwiftUI-like syntax
✅ **Type-Safe** - Full Kotlin type safety
✅ **Hot Reload** - Fast development iteration
✅ **Theme Inheritance** - Dynamic theme system
✅ **3D Support** - OpenGL/Metal integration (planned)
✅ **Asset Management** - Unified icon/image/font system

---

## 2. Architecture Overview

### Module Structure

```
AvaElements/
├── Core/                           # Core types, plugin system
│   ├── src/commonMain/kotlin/
│   │   ├── core/
│   │   │   ├── Plugin.kt          # Plugin interface
│   │   │   ├── Registry.kt        # Component registry
│   │   │   └── api/Renderer.kt    # Renderer interface
│   │   ├── types/                 # Base types
│   │   └── runtime/               # Plugin loader, security
│   ├── src/androidMain/
│   ├── src/iosMain/
│   └── src/desktopMain/
│
├── components/
│   ├── phase1/                    # 13 basic components
│   │   ├── display/               # Text, Icon, Image
│   │   ├── form/                  # Button, TextField, Checkbox, Switch
│   │   ├── layout/                # Card, Column, Row, Container
│   │   ├── navigation/            # ScrollView
│   │   └── data/                  # List
│   └── phase3/                    # 35 advanced components (planned)
│
├── Renderers/
│   ├── Android/                   # 39 Jetpack Compose mappers
│   │   └── src/androidMain/kotlin/
│   │       └── mappers/           # Component → Compose
│   ├── iOS/                       # SwiftUI (planned)
│   └── Web/                       # React (planned)
│
└── AssetManager/                  # Icons, images, fonts
    └── MaterialIconsLibrary.kt    # 2,235 Material Design icons
```

### Data Flow

```
┌─────────────────┐
│  AvaElements  │  DSL syntax
│      DSL        │  ui { column { text("Hello") } }
└────────┬────────┘
         │
         v
┌─────────────────┐
│   Component     │  Platform-agnostic
│     Tree        │  Component objects
└────────┬────────┘
         │
         v
┌─────────────────┐
│    Renderer     │  Platform-specific
│   (Android)    │  Jetpack Compose
└────────┬────────┘
         │
         v
┌─────────────────┐
│  Native UI      │  Material3 widgets
│   Widgets       │
└─────────────────┘
```

---

## 3. Getting Started

### Prerequisites

- Kotlin 1.9.24+
- Android Studio Arctic Fox or later
- JDK 17
- Gradle 8.10+

### Add AvaElements to Your Project

**Step 1:** Add to `settings.gradle.kts`

```kotlin
include(":modules:MagicIdea:UI:Core")
include(":modules:MagicIdea:Components:Renderers:Android")
include(":modules:MagicIdea:Components:AssetManager:AssetManager")
```

**Step 2:** Add dependencies in `build.gradle.kts`

```kotlin
kotlin {
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":modules:MagicIdea:UI:Core"))
                implementation(project(":modules:MagicIdea:Components:AssetManager:AssetManager"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":modules:MagicIdea:Components:Renderers:Android"))
                implementation("androidx.compose.ui:ui:1.6.8")
                implementation("androidx.compose.material3:material3:1.2.1")
            }
        }
    }
}
```

### Hello World Example

```kotlin
import com.augmentalis.avamagic.ui.core.layout.ColumnComponent
import com.augmentalis.avamagic.ui.core.display.TextComponent
import com.augmentalis.avamagic.ui.core.form.ButtonComponent
import com.augmentalis.avaelements.renderer.android.ComposeRenderer

@Composable
fun HelloWorldScreen() {
    val renderer = ComposeRenderer()

    val screen = ColumnComponent(
        children = listOf(
            TextComponent(
                text = "Hello AvaElements!",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            ),
            ButtonComponent(
                text = "Click Me",
                onClick = { println("Button clicked!") }
            )
        )
    )

    renderer.render(screen)()
}
```

---

## 4. Component Development

### Creating a New Component

**Step 1:** Define component data class in `UI:Core`

```kotlin
// File: modules/MagicIdea/UI/Core/src/commonMain/kotlin/.../display/CustomComponent.kt
package com.augmentalis.avamagic.ui.core.display

import com.augmentalis.avamagic.ui.core.base.Component
import com.augmentalis.avamagic.ui.core.base.Modifier

/**
 * CustomComponent - Displays custom content with special formatting
 *
 * @param title The title text
 * @param subtitle Optional subtitle text
 * @param icon Optional icon name from MaterialIcons
 * @param modifiers Visual modifiers (padding, margin, etc.)
 * @param onClick Optional click handler
 */
data class CustomComponent(
    val title: String,
    val subtitle: String? = null,
    val icon: String? = null,
    val modifiers: List<Modifier> = emptyList(),
    val onClick: (() -> Unit)? = null
) : Component {
    override val type: String = "Custom"
}
```

**Step 2:** Create Android mapper

```kotlin
// File: modules/MagicIdea/Components/Renderers/Android/src/androidMain/.../mappers/CustomMapper.kt
package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.avamagic.ui.core.display.CustomComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * CustomMapper - Maps CustomComponent to Material3 UI
 */
class CustomMapper : ComponentMapper<CustomComponent> {
    private val modifierConverter = ModifierConverter()

    @Composable
    override fun map(
        component: CustomComponent,
        renderer: ComposeRenderer
    ): @Composable () -> Unit {
        return {
            val modifier = modifierConverter.convert(component.modifiers)
                .let { if (component.onClick != null) it.clickable { component.onClick.invoke() } else it }

            Row(modifier = modifier) {
                // Icon (if provided)
                component.icon?.let { iconName ->
                    Icon(
                        imageVector = getIconByName(iconName),
                        contentDescription = iconName,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Text content
                Column {
                    Text(
                        text = component.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    component.subtitle?.let { sub ->
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    private fun getIconByName(name: String) = when(name) {
        "star" -> Icons.Default.Star
        "favorite" -> Icons.Default.Favorite
        else -> Icons.Default.Info
    }
}
```

**Step 3:** Register mapper in ComposeRenderer

```kotlin
// In ComposeRenderer.kt, add to mappers map:
private val mappers = mapOf(
    "Custom" to CustomMapper(),
    // ... other mappers
)
```

### Component Testing

```kotlin
// File: src/commonTest/kotlin/.../CustomComponentTest.kt
class CustomComponentTest {
    @Test
    fun `CustomComponent should have correct type`() {
        val component = CustomComponent(title = "Test")
        assertEquals("Custom", component.type)
    }

    @Test
    fun `CustomComponent should handle optional fields`() {
        val component = CustomComponent(
            title = "Title",
            subtitle = "Subtitle",
            icon = "star"
        )
        assertEquals("Subtitle", component.subtitle)
        assertEquals("star", component.icon)
    }

    @Test
    fun `CustomComponent click handler should be invoked`() {
        var clicked = false
        val component = CustomComponent(
            title = "Clickable",
            onClick = { clicked = true }
        )
        component.onClick?.invoke()
        assertTrue(clicked)
    }
}
```

---

## 5. Renderer Development

### Implementing a Custom Renderer

```kotlin
interface Renderer {
    val platform: Platform

    @Composable
    fun render(component: Component): @Composable () -> Unit

    enum class Platform {
        Android, iOS, Web, Desktop
    }
}

class CustomRenderer : Renderer {
    override val platform = Renderer.Platform.Android

    private val mappers = mutableMapOf<String, ComponentMapper<*>>()

    @Composable
    override fun render(component: Component): @Composable () -> Unit {
        val mapper = mappers[component.type]
            ?: throw IllegalArgumentException("No mapper for type: ${component.type}")

        @Suppress("UNCHECKED_CAST")
        return (mapper as ComponentMapper<Component>).map(component, this)
    }

    fun <T : Component> registerMapper(type: String, mapper: ComponentMapper<T>) {
        mappers[type] = mapper
    }
}
```

### ComponentMapper Interface

```kotlin
interface ComponentMapper<T : Component> {
    @Composable
    fun map(component: T, renderer: ComposeRenderer): @Composable () -> Unit
}
```

---

## 6. Asset Management

### Using MaterialIcons (2,235 icons available)

```kotlin
import com.augmentalis.universal.assetmanager.MaterialIconsLibrary

// Search for icons
val icons = MaterialIconsLibrary.searchIcons("heart")
// Returns: [IconSpec("favorite", "favorite", "Action", ...)]

// Get icon by name
val icon = MaterialIconsLibrary.getIconByName("favorite")

// Get all icons in a category
val actionIcons = MaterialIconsLibrary.getIconsByCategory("Action")

// Use in component
IconComponent(
    name = "favorite",
    size = 24.dp,
    tint = Color.Red
)
```

### Icon Categories (Available)

- **Action** - Common actions (favorite, delete, search, etc.)
- **Alert** - Warnings and notifications
- **AV** - Audio/video controls
- **Communication** - Phone, email, chat
- **Content** - Content editing icons
- **Device** - Device-specific icons
- **Editor** - Text editing icons
- **File** - File operations
- **Hardware** - Hardware icons
- **Image** - Image manipulation
- **Maps** - Location and maps
- **Navigation** - Navigation controls
- **Places** - Places and locations
- **Social** - Social media icons
- **Toggle** - Toggle buttons

### Loading Custom Assets

```kotlin
// Register custom image
AssetManager.registerImage(
    name = "logo",
    path = "assets/images/logo.png",
    width = 200,
    height = 100
)

// Use in component
ImageComponent(
    source = "logo",
    contentDescription = "Company Logo"
)
```

---

## 7. Cross-Platform SQLite Storage

AvaElements uses **SQLDelight 2.0.1** for cross-platform persistent storage with type-safe queries.

### Why SQLDelight?

✅ **Cross-platform** - Works on Android, iOS, Desktop, Web
✅ **Type-safe** - SQL queries validated at compile-time
✅ **Fast** - SQLite FTS5 full-text search
✅ **Small** - ~100 KB binary size
✅ **KMP-native** - Designed for Kotlin Multiplatform

See **[ADR-008](/Volumes/M-Drive/Coding/Avanues/docs/adr/ADR-008-SQLDelight-Cross-Platform-Storage.md)** for architecture decision details.

### Database Schema

AssetManager stores ~3,900 icons with full-text search:

```sql
-- File: AssetManager/src/commonMain/sqldelight/com/augmentalis/avaelements/assets/db/AssetDatabase.sq

CREATE TABLE Icon (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    svg TEXT,
    png_data TEXT,
    tags TEXT NOT NULL,
    library TEXT,
    category TEXT,
    aliases TEXT NOT NULL
);

-- Full-text search index (SQLite FTS5)
CREATE VIRTUAL TABLE IconSearch USING fts5(
    id UNINDEXED,
    name,
    tags,
    aliases,
    content=Icon,
    content_rowid=rowid
);

-- Queries
insertIcon:
INSERT OR REPLACE INTO Icon (id, name, svg, png_data, tags, library, category, aliases)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

getIconById:
SELECT * FROM Icon WHERE id = ?;

searchIcons:
SELECT Icon.* FROM Icon
JOIN IconSearch ON Icon.rowid = IconSearch.rowid
WHERE IconSearch MATCH ?
ORDER BY rank
LIMIT ? OFFSET ?;

getAllIcons:
SELECT * FROM Icon ORDER BY name;

getIconsByLibrary:
SELECT * FROM Icon WHERE library = ? ORDER BY name;

deleteAllIcons:
DELETE FROM Icon;
```

### Platform-Specific Drivers

SQLDelight uses the **expect/actual** pattern for platform drivers:

#### Common Interface

```kotlin
// File: AssetManager/src/commonMain/kotlin/.../db/DatabaseDriverFactory.kt
package com.augmentalis.avaelements.assets.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Database driver factory (expect/actual pattern)
 *
 * Provides platform-specific SQLite drivers
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

#### Android Driver

```kotlin
// File: AssetManager/src/androidMain/kotlin/.../db/DatabaseDriverFactory.android.kt
package com.augmentalis.avaelements.assets.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android database driver factory
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AssetDatabase.Schema,
            context = context,
            name = "avaelements_assets.db"
        )
    }
}
```

#### iOS Driver

```kotlin
// File: AssetManager/src/iosMain/kotlin/.../db/DatabaseDriverFactory.ios.kt
package com.augmentalis.avaelements.assets.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS database driver factory
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AssetDatabase.Schema,
            name = "avaelements_assets.db"
        )
    }
}
```

### Gradle Configuration

```kotlin
// File: AssetManager/build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.1"  // Add SQLDelight plugin
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // SQLDelight runtime
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android SQLite driver
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }

        val iosMain by creating {
            dependencies {
                // iOS SQLite driver (native)
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
        }
    }
}

// SQLDelight configuration
sqldelight {
    databases {
        create("AssetDatabase") {
            packageName.set("com.augmentalis.avaelements.assets.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
```

### Using SQLDelight in Your Code

#### Initialize Database

```kotlin
import com.augmentalis.avaelements.assets.db.DatabaseDriverFactory
import com.augmentalis.avaelements.assets.db.AssetDatabase

// Android
val driverFactory = DatabaseDriverFactory(context = applicationContext)
val database = AssetDatabase(driverFactory.createDriver())

// iOS
val driverFactory = DatabaseDriverFactory()
val database = AssetDatabase(driverFactory.createDriver())
```

#### Insert Data

```kotlin
// Insert single icon
database.iconQueries.insertIcon(
    id = "favorite",
    name = "Favorite",
    svg = "<svg>...</svg>",
    png_data = null,
    tags = "heart,love,like",
    library = "material",
    category = "Action",
    aliases = "heart,love"
)

// Batch insert (transaction)
database.transaction {
    icons.forEach { icon ->
        database.iconQueries.insertIcon(
            id = icon.id,
            name = icon.name,
            svg = icon.svg,
            png_data = icon.pngData,
            tags = icon.tags.joinToString(","),
            library = icon.library,
            category = icon.category,
            aliases = icon.aliases.joinToString(",")
        )
    }
}
```

#### Query Data

```kotlin
// Get icon by ID
val icon = database.iconQueries.getIconById(id = "favorite").executeAsOneOrNull()

// Get all icons from library
val materialIcons = database.iconQueries.getIconsByLibrary(library = "material").executeAsList()

// Full-text search
val searchResults = database.iconQueries.searchIcons(
    query = "heart",
    limit = 100,
    offset = 0
).executeAsList()

// Get all icons
val allIcons = database.iconQueries.getAllIcons().executeAsList()
```

#### Async Queries with Coroutines

```kotlin
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow

// Observe query as Flow
val iconsFlow: Flow<List<Icon>> = database.iconQueries
    .getAllIcons()
    .asFlow()
    .mapToList(Dispatchers.IO)

// Use in ViewModel
class IconViewModel {
    val icons: StateFlow<List<Icon>> = database.iconQueries
        .getAllIcons()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

### Performance Benchmarks

| Operation | Time (ms) | Notes |
|-----------|-----------|-------|
| Insert 1,000 icons | 120 | Batched transaction |
| Search by name | 5 | FTS5 index |
| Search by tags | 8 | FTS5 index |
| Get icon by ID | 1 | Primary key index |
| Get library icons | 12 | Library index |

### Binary Size Impact

- SQLDelight runtime: ~80 KB
- Android driver: ~20 KB
- iOS driver: ~30 KB (native)
- **Total: ~100 KB**

### Migration Support

SQLDelight supports schema migrations for evolving your database:

```sql
-- File: AssetDatabase.sq (Migration 1 → 2)
ALTER TABLE Icon ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0;

-- Create migration directory
-- src/commonMain/sqldelight/databases/migrations/
-- 1.sqm, 2.sqm, etc.
```

### Testing with In-Memory Database

```kotlin
// Use in-memory driver for tests
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

class AssetStorageTest {
    private lateinit var database: AssetDatabase

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AssetDatabase.Schema.create(driver)
        database = AssetDatabase(driver)
    }

    @Test
    fun testInsertAndQuery() {
        database.iconQueries.insertIcon(
            id = "test",
            name = "Test Icon",
            // ...
        )

        val icon = database.iconQueries.getIconById("test").executeAsOne()
        assertEquals("Test Icon", icon.name)
    }
}
```

---

## 8. Universal Theming System

AvaElements provides a **universal theming system** that works across iOS, Android, macOS, Windows, Web, and XR/AR/VR platforms.

See **[ADR-009](/Volumes/M-Drive/Coding/Avanues/docs/adr/ADR-009-Universal-Theming-System.md)** for architecture decision details.

### Key Concepts

✅ **Design Tokens** - Atomic design values (single source of truth)
✅ **Theme Modes** - Light, Dark, XR, Auto
✅ **Visual Styles** - ModernUI, LiquidGlass, SpatialGlass, FrostGlass
✅ **Cross-Platform** - One theme definition for all platforms
✅ **Platform Overrides** - Optional platform-specific customization

### What are Design Tokens?

Design tokens are the **atomic values** that define your design system:

```kotlin
// ❌ Hard-coded values (bad)
backgroundColor = Color(0xFF6200EE)
padding = 16.dp
fontSize = 24.sp

// ✅ Design tokens (good)
backgroundColor = theme.tokens.color.primary.main
padding = theme.tokens.spacing.md.dp
fontSize = theme.tokens.typography.h1.fontSize
```

**Benefits:**
- Single source of truth
- Semantic naming
- Platform consistency
- Instant theme switching
- Accessibility built-in

### Design Token Structure

```kotlin
data class DesignTokens(
    val color: ColorTokens,          // Color scales (11 shades per color)
    val spacing: SpacingTokens,      // 8dp base unit system
    val typography: TypographyTokens, // Font families, sizes, weights
    val radius: RadiusTokens,        // Border radius values
    val elevation: ElevationTokens,  // Shadow/depth system
    val motion: MotionTokens,        // Animation durations/easings
    val breakpoints: BreakpointTokens, // Responsive breakpoints
    val zIndex: ZIndexTokens         // Stacking order
)
```

### Four Theme Modes

```kotlin
enum class ThemeMode {
    LIGHT,  // Traditional light theme
    DARK,   // Traditional dark theme
    XR,     // Spatial/AR/VR/MR for see-through displays
    AUTO    // System preference
}
```

### Four Preset Themes

#### 1. ModernUITheme (Material3-inspired)

**Best for:** Android apps, modern web apps, enterprise software

```kotlin
import com.augmentalis.avaelements.core.theme.presets.ModernUITheme

// Use light theme
val theme = ModernUITheme.Light

// Use dark theme
val theme = ModernUITheme.Dark

// Access theme pair
val (light, dark) = ModernUITheme.ThemePair
```

**Characteristics:**
- Vibrant primary colors (#6750A4 purple)
- 12dp rounded corners
- Subtle elevation
- 300ms animations
- 100% opaque surfaces

#### 2. LiquidGlassTheme (iOS-inspired)

**Best for:** iOS apps, modern mobile interfaces

```kotlin
import com.augmentalis.avaelements.core.theme.presets.LiquidGlassTheme

// Use light theme
val theme = LiquidGlassTheme.Light

// Use dark theme
val theme = LiquidGlassTheme.Dark
```

**Characteristics:**
- System blues (#007AFF)
- 12dp rounded corners (iOS-style)
- Minimal shadows
- Spring animations (700ms)
- 85% opacity (frosted glass)

#### 3. SpatialGlassTheme (XR/Spatial)

**Best for:** AR/VR/MR apps, visionOS, spatial computing

```kotlin
import com.augmentalis.avaelements.core.theme.presets.SpatialGlassTheme

// Use XR theme
val theme = SpatialGlassTheme.XR
```

**Characteristics:**
- High-contrast vibrants (#0A84FF blue)
- 24dp rounded corners (depth-friendly)
- Large shadows (spatial depth)
- Slow animations (500ms)
- **15-25% opacity (see-through)**
- 12dp base spacing (larger hit targets)
- Depth separation (8cm Z-axis)
- Spatial audio integration

**XR-specific features:**
```kotlin
theme.platformOverrides.xr?.let { xr ->
    glassOpacity = xr.glassOpacity        // 0.25f (25%)
    depthSeparation = xr.depthSeparation  // 0.08f (8cm)
    spatialAudio = xr.spatialAudio        // true
    depthBlur = xr.depthBlur             // true
}
```

#### 4. FrostGlassTheme (Glassmorphism)

**Best for:** Premium apps, creative tools, modern dashboards

```kotlin
import com.augmentalis.avaelements.core.theme.presets.FrostGlassTheme

// Use light theme
val theme = FrostGlassTheme.Light

// Use dark theme
val theme = FrostGlassTheme.Dark
```

**Characteristics:**
- Vivid colors (#7C3AED purple, #06B6D4 cyan)
- 16dp rounded corners
- Purple-tinted shadows
- 400ms smooth transitions
- **30-60% opacity (frosted glass)**
- Subtle vivid borders

### Using Themes in Components

#### Access Design Tokens

```kotlin
@Composable
fun MyButton(theme: UniversalTheme = ModernUITheme.Light) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.tokens.color.primary.main,
            contentColor = theme.tokens.color.primary.contrastText
        ),
        modifier = Modifier
            .padding(theme.tokens.spacing.md.dp)
            .clip(RoundedCornerShape(theme.tokens.radius.medium.dp))
    ) {
        Text(
            text = "Click Me",
            fontSize = theme.tokens.typography.button.fontSize,
            fontWeight = theme.tokens.typography.button.fontWeight
        )
    }
}
```

#### Theme Switching

```kotlin
@Composable
fun App() {
    var currentTheme by remember { mutableStateOf(ModernUITheme.Light) }
    var isDark by remember { mutableStateOf(false) }

    // Switch light/dark
    Button(onClick = {
        isDark = !isDark
        currentTheme = if (isDark) ModernUITheme.Dark else ModernUITheme.Light
    }) {
        Text("Toggle Theme")
    }

    // Switch visual style
    Button(onClick = {
        currentTheme = LiquidGlassTheme.Light
    }) {
        Text("Switch to Liquid Glass")
    }

    ProvideTheme(currentTheme) {
        AppContent()
    }
}
```

### Creating Custom Themes

#### Simple Custom Theme

```kotlin
import com.augmentalis.avaelements.core.theme.UniversalTheme
import com.augmentalis.avaelements.core.theme.ThemeMode
import com.augmentalis.avaelements.core.theme.VisualStyle
import com.augmentalis.avaelements.core.tokens.DesignTokens
import com.augmentalis.avaelements.core.tokens.ColorTokens
import com.augmentalis.avaelements.core.tokens.ColorScale

val myBrandTheme = UniversalTheme(
    id = "my-brand",
    name = "My Brand Theme",
    mode = ThemeMode.LIGHT,
    visualStyle = VisualStyle.CUSTOM,
    tokens = DesignTokens(
        color = ColorTokens(
            primary = ColorScale(
                shade500 = Color(0xFF1976D2),  // Your brand color
                // Other shades auto-interpolated
            ),
            secondary = ColorScale(
                shade500 = Color(0xFFDC004E)
            )
            // ... other colors use defaults
        ),
        spacing = SpacingTokens(unit = 8f),    // 8dp base
        typography = TypographyTokens(
            fontFamily = FontFamily("Roboto")
        ),
        // ... other tokens use defaults
    )
)
```

#### Advanced Custom Theme with Light/Dark Pair

```kotlin
object MyBrandTheme {
    val ThemePair: com.augmentalis.avaelements.core.theme.ThemePair = createMyBrandPair()
    val Light: UniversalTheme = ThemePair.light
    val Dark: UniversalTheme = ThemePair.dark

    private fun createMyBrandPair(): com.augmentalis.avaelements.core.theme.ThemePair {
        return UniversalTheme.createPair(
            id = "my-brand",
            name = "My Brand",
            visualStyle = VisualStyle.CUSTOM,
            lightTokens = createLightTokens(),
            darkTokens = createDarkTokens()
        )
    }

    private fun createLightTokens(): DesignTokens {
        return DesignTokens(
            color = ColorTokens(
                primary = ColorScale(/* light mode colors */),
                surface = SurfaceColors(
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA)
                )
            ),
            // ... other tokens
        )
    }

    private fun createDarkTokens(): DesignTokens {
        return DesignTokens(
            color = ColorTokens(
                primary = ColorScale(/* dark mode colors */),
                surface = SurfaceColors(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            ),
            // ... other tokens
        )
    }
}

// Usage
val theme = MyBrandTheme.Light
val darkTheme = MyBrandTheme.Dark
```

### Platform Overrides

#### iOS-specific Overrides

```kotlin
val themeWithIOSOverrides = ModernUITheme.Light.copy(
    platformOverrides = PlatformOverrides(
        ios = IosOverrides(
            useSafeArea = true,
            safeAreaInsets = EdgeInsets.system,
            preferLargeTitle = true
        )
    )
)
```

#### Android-specific Overrides

```kotlin
val themeWithAndroidOverrides = ModernUITheme.Light.copy(
    platformOverrides = PlatformOverrides(
        android = AndroidOverrides(
            useSystemBars = true,
            navigationBarColor = Color(0xFF6750A4),
            statusBarColor = Color.Transparent
        )
    )
)
```

#### XR-specific Overrides

```kotlin
val themeWithXROverrides = SpatialGlassTheme.XR.copy(
    platformOverrides = PlatformOverrides(
        xr = XrOverrides(
            glassOpacity = 0.3f,          // 30% opacity
            depthSeparation = 0.1f,       // 10cm depth
            spatialAudio = true,
            depthBlur = true,
            materialType = XrMaterialType.GLASS
        )
    )
)
```

### Color Scale System

Each semantic color has **11 shades** (50-950):

```kotlin
val primaryColor = theme.tokens.color.primary

// Access specific shades
val lightest = primaryColor.shade50    // Lightest
val light = primaryColor.shade300      // Light variant
val main = primaryColor.shade500       // Main color
val dark = primaryColor.shade700       // Dark variant
val darkest = primaryColor.shade900    // Darkest

// Convenience accessors
val mainColor = primaryColor.main           // = shade500
val lightVariant = primaryColor.light       // = shade300
val darkVariant = primaryColor.dark         // = shade700
val onPrimary = primaryColor.contrastText   // Auto-calculated
```

### Theme Comparison Guide

| Feature | ModernUI | LiquidGlass | SpatialGlass | FrostGlass |
|---------|----------|-------------|--------------|------------|
| **Target** | Android/Web | iOS | XR/AR/VR | Premium Apps |
| **Opacity** | 100% | 85% | 15-25% | 30-60% |
| **Corners** | 12dp | 12dp | 24dp | 16dp |
| **Spacing** | 8dp | 8dp | 12dp | 8dp |
| **Animation** | 300ms | 700ms spring | 500ms | 400ms |
| **Elevation** | Tonal | Minimal | Spatial | Vivid |

### Accessibility

All preset themes meet **WCAG 2.1 Level AA**:

✅ Color contrast ratios ≥ 4.5:1 (text)
✅ Color contrast ratios ≥ 3:1 (UI components)
✅ Focus indicators clearly visible
✅ Touch targets ≥ 44x44dp (iOS), 48x48dp (Android)

### Performance

- **Binary size:** +120 KB (all 4 presets, tree-shakeable)
- **Runtime overhead:** 0ms (compile-time token resolution)
- **Theme switch time:** <1ms (token set swap)
- **Memory footprint:** ~40 KB per theme instance

---

## 9. Testing Guidelines

### Unit Testing Components

```kotlin
class ButtonComponentTest {
    @Test
    fun `ButtonComponent should have correct default values`() {
        val button = ButtonComponent(text = "Click")
        assertEquals("Click", button.text)
        assertNull(button.icon)
        assertFalse(button.disabled)
    }

    @Test
    fun `ButtonComponent onClick should be invoked`() {
        var clicked = false
        val button = ButtonComponent(
            text = "Test",
            onClick = { clicked = true }
        )
        button.onClick?.invoke()
        assertTrue(clicked)
    }
}
```

### UI Testing (Android)

```kotlin
@RunWith(AndroidJUnit4::class)
class CustomMapperTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun customComponentRendersCorrectly() {
        val component = CustomComponent(
            title = "Test Title",
            subtitle = "Test Subtitle"
        )

        composeTestRule.setContent {
            val renderer = ComposeRenderer()
            renderer.render(component)()
        }

        composeTestRule.onNodeWithText("Test Title").assertExists()
        composeTestRule.onNodeWithText("Test Subtitle").assertExists()
    }

    @Test
    fun customComponentHandlesClick() {
        var clicked = false
        val component = CustomComponent(
            title = "Clickable",
            onClick = { clicked = true }
        )

        composeTestRule.setContent {
            val renderer = ComposeRenderer()
            renderer.render(component)()
        }

        composeTestRule.onNodeWithText("Clickable").performClick()
        assertTrue(clicked)
    }
}
```

---

## 10. API Reference

### Core Components (Phase 1 - Complete)

#### Display Components

**TextComponent**
```kotlin
data class TextComponent(
    val text: String,
    val style: TextStyle? = null,
    val color: Color? = null,
    val maxLines: Int? = null,
    val overflow: TextOverflow = TextOverflow.Clip,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

**IconComponent**
```kotlin
data class IconComponent(
    val name: String,                    // Material icon name
    val size: Dp = 24.dp,
    val tint: Color? = null,
    val contentDescription: String? = null,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

**ImageComponent**
```kotlin
data class ImageComponent(
    val source: String,                  // Asset name or URL
    val contentDescription: String? = null,
    val contentScale: ContentScale = ContentScale.Fit,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

#### Form Components

**ButtonComponent**
```kotlin
data class ButtonComponent(
    val text: String,
    val onClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
    val icon: String? = null,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

**TextFieldComponent**
```kotlin
data class TextFieldComponent(
    val value: String,
    val onValueChange: ((String) -> Unit)? = null,
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val maxLines: Int = 1,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

**CheckboxComponent**
```kotlin
data class CheckboxComponent(
    val checked: Boolean,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    val enabled: Boolean = true,
    val label: String? = null,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

**SwitchComponent**
```kotlin
data class SwitchComponent(
    val checked: Boolean,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    val enabled: Boolean = true,
    val label: String? = null,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

#### Layout Components

**ColumnComponent**
```kotlin
data class ColumnComponent(
    val children: List<Component>,
    val spacing: Dp = 0.dp,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

**RowComponent**
```kotlin
data class RowComponent(
    val children: List<Component>,
    val spacing: Dp = 0.dp,
    val verticalAlignment: Alignment.Vertical = Alignment.Top,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

**CardComponent**
```kotlin
data class CardComponent(
    val children: List<Component>,
    val elevation: Dp = 4.dp,
    val shape: Shape = RoundedCornerShape(8.dp),
    val backgroundColor: Color? = null,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

**ContainerComponent**
```kotlin
data class ContainerComponent(
    val child: Component,
    val padding: Dp = 0.dp,
    val backgroundColor: Color? = null,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

#### Navigation Components

**ScrollViewComponent**
```kotlin
data class ScrollViewComponent(
    val child: Component,
    val direction: ScrollDirection = ScrollDirection.Vertical,
    val modifiers: List<Modifier> = emptyList()
) : Component

enum class ScrollDirection {
    Vertical, Horizontal
}
```

#### Data Components

**ListComponent**
```kotlin
data class ListComponent(
    val items: List<Component>,
    val divider: Boolean = false,
    val modifiers: List<Modifier> = emptyList()
) : Component
```

### Advanced Components (Phase 2/3 - In Progress)

#### Slider, Radio, Dropdown, Dialog, Toast, Avatar, Badge, Chip, ProgressBar
(See respective mapper files for full API)

---

## 11. Best Practices

### Component Design

✅ **DO:**
- Keep components immutable (use `data class`)
- Provide sensible defaults
- Use nullable types for optional fields
- Document all parameters with KDoc
- Include example usage in documentation

❌ **DON'T:**
- Store mutable state in components
- Use platform-specific types in common code
- Create deep component hierarchies (keep it flat)
- Couple components to specific renderers

### Renderer Design

✅ **DO:**
- Use Material Design guidelines for Android
- Support all component modifiers
- Handle null/optional fields gracefully
- Provide accessibility labels
- Test on multiple screen sizes

❌ **DON'T:**
- Hardcode colors/dimensions (use theme)
- Ignore error states
- Skip accessibility support
- Create tightly coupled mappers

### Performance

✅ **DO:**
- Use `remember` for expensive computations
- Implement `key` for lists
- Use `LaunchedEffect` for side effects
- Profile with Compose Layout Inspector
- Minimize recomposition scope

❌ **DON'T:**
- Create new objects in `@Composable` functions
- Use unstable types in parameters
- Call suspend functions directly
- Ignore composition warnings

### Testing

✅ **DO:**
- Write unit tests for all components
- Test UI rendering with Compose Test
- Test edge cases (null values, empty lists)
- Mock external dependencies
- Achieve 80%+ code coverage

❌ **DON'T:**
- Skip testing for "simple" components
- Test implementation details
- Create flaky tests
- Ignore test failures

---

## Resources

- **GitHub:** [AvaElements Repository](#)
- **API Docs:** [KDoc Documentation](#)
- **Examples:** `/Universal/Libraries/AvaElements/examples/`
- **Issue Tracker:** [GitHub Issues](#)
- **Community:** [Discord Server](#)

## Support

- **Email:** manoj@ideahq.net
- **Documentation:** `/docs/`
- **Tutorials:** Coming soon

---

**Version:** 2.1.0
**Last Updated:** 2025-01-14
**Author:** Manoj Jhawar

