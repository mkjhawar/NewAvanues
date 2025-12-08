# Universal Theme Manager

Centralized theme management system for all Avanues applications.

## Overview

The Universal Theme Manager provides a single source of truth for theming across all Avanues apps, with support for:

- **Global Avanues Theme**: One unified theme for the entire ecosystem
- **Per-App Overrides**: Apps can customize their appearance while maintaining consistency
- **Full & Partial Overrides**: Choose between complete theme replacement or selective property overrides
- **Theme Inheritance**: Apps inherit from the universal theme and override only what they need
- **Persistent Storage**: Themes are saved locally and optionally synced to the cloud
- **Real-time Updates**: Apps automatically receive theme changes via reactive Flow APIs

## Architecture

```
ThemeManager (Singleton)
├── Universal Theme (Global)
├── App Overrides (Per-App)
│   ├── Full Override (Complete replacement)
│   └── Partial Override (Selective properties)
├── ThemeRepository (Persistence)
│   ├── LocalThemeRepository (File storage)
│   ├── CloudThemeRepository (Cloud sync)
│   └── InMemoryThemeRepository (Testing)
└── ThemeSync (Synchronization)
    ├── Local ↔ Cloud sync
    ├── Conflict resolution
    └── Version tracking
```

## Installation

### 1. Add to settings.gradle.kts

```kotlin
include(":Universal:Core:ThemeManager")
```

### 2. Add dependency to your app module

```kotlin
// In your app's build.gradle.kts
dependencies {
    implementation(project(":Universal:Core:ThemeManager"))
    implementation(project(":Universal:Libraries:AvaElements:Core"))
}
```

## Quick Start

### Basic Setup

```kotlin
import com.augmentalis.universal.thememanager.ThemeManager
import com.augmentalis.universal.thememanager.LocalThemeRepository
import com.augmentalis.avaelements.core.Themes

// 1. Initialize on app startup
suspend fun initializeThemes() {
    ThemeManager.initialize(
        repository = LocalThemeRepository(),
        syncManager = null  // Optional: Add cloud sync later
    )

    // 2. Load saved themes
    ThemeManager.loadThemes()

    // 3. Set default universal theme (first time only)
    if (ThemeManager.getUniversalTheme().name.isEmpty()) {
        ThemeManager.setUniversalTheme(Themes.Material3Light)
    }
}

// 4. Get theme for your app
val appId = "com.augmentalis.myapp"
val theme = ThemeManager.getTheme(appId)

// 5. Observe theme changes
ThemeManager.observeTheme(appId).collect { theme ->
    // Update your UI with the new theme
    applyTheme(theme)
}
```

## Usage Examples

### Setting the Universal Theme

All apps without overrides will use this theme:

```kotlin
// Set Material Design 3 as the universal theme
ThemeManager.setUniversalTheme(Themes.Material3Light)

// Or iOS 26 Liquid Glass
ThemeManager.setUniversalTheme(Themes.iOS26LiquidGlass)

// Or Windows 11 Fluent 2
ThemeManager.setUniversalTheme(Themes.Windows11Fluent2)
```

### Full App Override

Completely replace the universal theme for a specific app:

```kotlin
val appId = "com.augmentalis.voiceos"

// VoiceOS uses iOS 26 Liquid Glass theme exclusively
ThemeManager.setAppTheme(appId, Themes.iOS26LiquidGlass)

// Now VoiceOS will use iOS theme regardless of universal theme
val voiceOSTheme = ThemeManager.getTheme(appId)
println(voiceOSTheme.name) // "iOS 26 Liquid Glass"
```

### Partial App Override

Inherit most properties from universal theme, override specific ones:

```kotlin
val appId = "com.augmentalis.noteavanue"

// Create custom theme with different colors but same typography
val customTheme = Themes.Material3Light.copy(
    name = "NoteAvanue Custom",
    colorScheme = Themes.Windows11Fluent2.colorScheme
)

// Set partial override - inherit everything except colors
ThemeManager.setPartialAppTheme(
    appId = appId,
    theme = customTheme,
    inheritedProperties = listOf(
        ThemeProperties.TYPOGRAPHY,
        ThemeProperties.SHAPES,
        ThemeProperties.SPACING,
        ThemeProperties.ELEVATION,
        ThemeProperties.ANIMATION
    )
)
```

### Using the Builder Pattern

```kotlin
val appId = "com.augmentalis.browseravanue"

val override = appId.createThemeOverride()
    .partialOverride(Themes.Windows11Fluent2)
    .inheritTypography()
    .inheritSpacing()
    .inheritShapes()
    .withDescription("Windows 11 colors with universal typography")
    .build()

ThemeManager.setPartialAppTheme(
    appId = override.appId,
    theme = override.theme,
    inheritedProperties = override.inheritedProperties
)
```

### Removing an Override

Fall back to the universal theme:

```kotlin
ThemeManager.removeAppTheme("com.augmentalis.myapp")

// App now uses universal theme
```

### Listing Apps with Overrides

```kotlin
val appsWithOverrides = ThemeManager.getAppsWithOverrides()

appsWithOverrides.forEach { appId ->
    val override = ThemeManager.getAppOverride(appId)
    println("$appId: ${override?.overrideType} - ${override?.theme?.name}")
}
```

### Observing Theme Changes

```kotlin
// In your UI layer (e.g., MainActivity, App composable)
lifecycleScope.launch {
    ThemeManager.observeTheme(appId).collect { theme ->
        // Update MaterialTheme or similar
        updateAppTheme(theme)
    }
}

// Or in Compose
@Composable
fun MyApp() {
    val theme by ThemeManager.observeTheme(appId).collectAsState()

    MaterialTheme(
        colorScheme = theme.colorScheme,
        typography = theme.typography,
        shapes = theme.shapes
    ) {
        // Your app content
    }
}
```

### Exporting and Importing Themes

Useful for backups, migrations, or sharing themes:

```kotlin
// Export all themes
val export = ThemeManager.exportThemes()

// Save to file or send to server
saveToFile(export)

// Later, import themes
val export = loadFromFile()
ThemeManager.importThemes(export)
```

## Cloud Synchronization

### Setup Cloud Sync

```kotlin
// Create a cloud repository (example with custom provider)
val cloudRepo = CloudThemeRepository(myCloudProvider)

// Create sync manager
val syncManager = ThemeSync(
    localRepository = LocalThemeRepository(),
    cloudRepository = cloudRepo,
    conflictResolver = LastWriteWinsResolver()
)

// Initialize ThemeManager with sync
ThemeManager.initialize(
    repository = LocalThemeRepository(),
    syncManager = syncManager
)
```

### Sync Operations

```kotlin
// Sync from cloud to local
syncManager.syncFromCloud()

// Sync from local to cloud
syncManager.syncToCloud()

// Bidirectional sync
syncManager.sync()

// Observe sync state
syncManager.syncState.collect { state ->
    when (state) {
        is SyncState.Idle -> hideProgress()
        is SyncState.Syncing -> showProgress()
        is SyncState.Success -> showSuccess()
        is SyncState.Error -> showError(state.message)
    }
}
```

### Conflict Resolution Strategies

```kotlin
// Last-write-wins (default)
val resolver = LastWriteWinsResolver()

// Local-first (always prefer local changes)
val resolver = LocalFirstResolver()

// Cloud-first (always prefer cloud changes)
val resolver = CloudFirstResolver()

// Manual (prompt user to choose)
val resolver = ManualConflictResolver { local, cloud ->
    // Show UI to user
    askUserToResolve(local, cloud)
}
```

## Theme Resolution Flow

1. **App requests theme**: `ThemeManager.getTheme(appId)`
2. **Check for override**: Does this app have an override?
   - **No**: Return universal theme
   - **Yes, Full**: Return override theme completely
   - **Yes, Partial**: Merge universal theme with override properties

## File Storage

Themes are stored in `Universal/Core/ThemeManager/themes/`:

```
themes/
├── universal.json              # Global Avanues theme
└── apps/
    ├── com_augmentalis_voiceos.json         # App theme data
    ├── com_augmentalis_voiceos.override.json # Override config
    ├── com_augmentalis_noteavanue.json
    └── com_augmentalis_noteavanue.override.json
```

### Example universal.json

```json
{
  "name": "Avanues Universal Theme",
  "platform": "Material3_Expressive",
  "colorScheme": { ... },
  "typography": { ... },
  "shapes": { ... },
  "spacing": { ... },
  "elevation": { ... },
  "animation": { ... }
}
```

### Example override configuration

```json
{
  "appId": "com.augmentalis.voiceos",
  "overrideType": "FULL",
  "theme": { ... },
  "inheritedProperties": [],
  "description": "VoiceOS uses iOS Liquid Glass theme",
  "createdAt": 1730236800000,
  "modifiedAt": 1730236800000
}
```

## Integration with AvaElements

ThemeManager works seamlessly with AvaElements Core themes:

```kotlin
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.core.Themes

// All AvaElements predefined themes are available
ThemeManager.setUniversalTheme(Themes.Material3Light)
ThemeManager.setAppTheme("myapp", Themes.iOS26LiquidGlass)
ThemeManager.setAppTheme("myapp", Themes.Windows11Fluent2)
ThemeManager.setAppTheme("myapp", Themes.visionOS2SpatialGlass)
```

## Best Practices

### 1. Initialize Early

Initialize ThemeManager in your Application class or main activity:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            initializeThemes()
        }
    }
}
```

### 2. Use Partial Overrides When Possible

Partial overrides maintain consistency while allowing customization:

```kotlin
// Good: Inherit most properties, override only colors
ThemeManager.setPartialAppTheme(appId, customTheme, inheritedProperties)

// Avoid: Full override unless truly necessary
ThemeManager.setAppTheme(appId, customTheme)
```

### 3. Observe Theme Changes

Always use `observeTheme()` instead of `getTheme()` in UI code:

```kotlin
// Good: Reactive updates
ThemeManager.observeTheme(appId).collect { theme -> updateUI(theme) }

// Avoid: One-time read (won't update if theme changes)
val theme = ThemeManager.getTheme(appId)
```

### 4. Provide User Control

Allow users to choose themes:

```kotlin
// Settings screen
fun onThemeSelected(theme: Theme) {
    lifecycleScope.launch {
        ThemeManager.setUniversalTheme(theme)
        // All apps update automatically
    }
}
```

## API Reference

### ThemeManager

| Method | Description |
|--------|-------------|
| `initialize(repository, syncManager)` | Initialize with storage backend |
| `loadThemes()` | Load themes from persistent storage |
| `setUniversalTheme(theme)` | Set global Avanues theme |
| `getUniversalTheme()` | Get global theme |
| `setAppTheme(appId, theme)` | Set full app override |
| `setPartialAppTheme(appId, theme, inheritedProperties)` | Set partial override |
| `removeAppTheme(appId)` | Remove app override |
| `getTheme(appId)` | Get effective theme for app |
| `observeTheme(appId)` | Observe theme changes |
| `getAppsWithOverrides()` | List apps with overrides |
| `exportThemes()` | Export all themes |
| `importThemes(export)` | Import themes |

### ThemeRepository

Implement this interface for custom storage backends:

```kotlin
interface ThemeRepository {
    suspend fun saveUniversalTheme(theme: Theme)
    suspend fun loadUniversalTheme(): Theme?
    suspend fun saveAppTheme(appId: String, theme: Theme)
    suspend fun loadAppTheme(appId: String): Theme?
    suspend fun deleteAppTheme(appId: String)
    suspend fun saveAppOverride(appId: String, override: ThemeOverride)
    suspend fun loadAppOverride(appId: String): ThemeOverride?
    suspend fun deleteAppOverride(appId: String)
    suspend fun loadAllAppThemes(): Map<String, Theme>
    suspend fun clearAll()
}
```

### ThemeOverride

```kotlin
data class ThemeOverride(
    val appId: String,
    val overrideType: OverrideType,  // FULL or PARTIAL
    val theme: Theme,
    val inheritedProperties: List<String>,
    val description: String? = null,
    val createdAt: Long,
    val modifiedAt: Long
)
```

## Troubleshooting

### Theme not updating in UI

Ensure you're using `observeTheme()` not `getTheme()`:

```kotlin
// This won't auto-update
val theme = ThemeManager.getTheme(appId)

// This will auto-update
ThemeManager.observeTheme(appId).collect { theme -> ... }
```

### Themes not persisting

Check that you're calling `initialize()` and `loadThemes()`:

```kotlin
ThemeManager.initialize(LocalThemeRepository())
ThemeManager.loadThemes()
```

### Override not working

Verify the override type:

```kotlin
val override = ThemeManager.getAppOverride(appId)
println("Override type: ${override?.overrideType}")
println("Theme name: ${override?.theme?.name}")
```

## Examples

See `ThemeManagerExample.kt` for comprehensive examples covering:

1. Setting universal theme
2. Full app overrides
3. Partial app overrides
4. Builder pattern
5. Theme observation
6. Export/import
7. Cloud sync
8. Custom themes with inheritance

## Version History

### Version 1.0.0
- Initial release
- Universal theme management
- Full and partial app overrides
- Local file storage
- Cloud sync framework
- Theme import/export
- Real-time theme observation

## License

Copyright 2025 Augmentalis. All rights reserved.
