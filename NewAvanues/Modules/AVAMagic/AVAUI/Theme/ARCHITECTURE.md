# Universal Theme Manager Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      Avanues Apps                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ VoiceOS  │  │NoteAvanue│  │BrowserAv.│  │ AIAvanue │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
│       │             │              │             │              │
└───────┼─────────────┼──────────────┼─────────────┼──────────────┘
        │             │              │             │
        └─────────────┴──────────────┴─────────────┘
                          │
                          ▼
        ┌─────────────────────────────────────────┐
        │       ThemeManager (Singleton)          │
        │                                         │
        │  ┌────────────────────────────────┐    │
        │  │  Universal Theme (Global)      │    │
        │  │  - Material3Light (default)    │    │
        │  │  - iOS26LiquidGlass           │    │
        │  │  - Windows11Fluent2           │    │
        │  └────────────────────────────────┘    │
        │                                         │
        │  ┌────────────────────────────────┐    │
        │  │  App Overrides (Per-App)       │    │
        │  │  ┌──────────────────────────┐  │    │
        │  │  │ VoiceOS: FULL            │  │    │
        │  │  │ Theme: iOS26LiquidGlass  │  │    │
        │  │  └──────────────────────────┘  │    │
        │  │  ┌──────────────────────────┐  │    │
        │  │  │ NoteAvanue: PARTIAL      │  │    │
        │  │  │ Inherit: Typography, etc.│  │    │
        │  │  │ Override: ColorScheme    │  │    │
        │  │  └──────────────────────────┘  │    │
        │  └────────────────────────────────┘    │
        └─────────────┬───────────────────────────┘
                      │
        ┌─────────────┴────────────────────┐
        │                                  │
        ▼                                  ▼
┌──────────────────┐           ┌──────────────────┐
│ ThemeRepository  │           │   ThemeSync      │
│                  │           │                  │
│ ┌──────────────┐ │           │ ┌──────────────┐ │
│ │ Local Storage│ │           │ │Cloud Storage │ │
│ │ (JSON Files) │ │◄─────────►│ │(Firebase, etc)│ │
│ └──────────────┘ │           │ └──────────────┘ │
│                  │           │                  │
│ ┌──────────────┐ │           │ ┌──────────────┐ │
│ │  In-Memory   │ │           │ │ Conflict     │ │
│ │  (Testing)   │ │           │ │ Resolution   │ │
│ └──────────────┘ │           │ └──────────────┘ │
└──────────────────┘           └──────────────────┘
```

## Component Responsibilities

### ThemeManager
**Role**: Central coordinator and single source of truth

**Responsibilities**:
- Store universal theme (singleton)
- Manage per-app overrides (map)
- Resolve effective theme for each app
- Emit events when themes change (Flow)
- Coordinate persistence and sync

**Key APIs**:
```kotlin
setUniversalTheme(theme)
setAppTheme(appId, theme)
setPartialAppTheme(appId, theme, inheritedProperties)
getTheme(appId)
observeTheme(appId): StateFlow<Theme>
```

### ThemeRepository
**Role**: Abstract persistence layer

**Implementations**:
- **LocalThemeRepository**: File-based JSON storage
- **CloudThemeRepository**: Cloud sync (placeholder)
- **InMemoryThemeRepository**: Testing

**Responsibilities**:
- Save/load universal theme
- Save/load app themes
- Save/load override configurations
- Platform-agnostic interface

**Storage Location**:
```
Universal/Core/ThemeManager/themes/
├── universal.json
└── apps/
    ├── com_augmentalis_voiceos.json
    ├── com_augmentalis_voiceos.override.json
    ├── com_augmentalis_noteavanue.json
    └── com_augmentalis_noteavanue.override.json
```

### ThemeOverride
**Role**: Configure per-app theme overrides

**Types**:
- **FULL**: Complete theme replacement
- **PARTIAL**: Selective property override with inheritance

**Data Structure**:
```kotlin
ThemeOverride(
    appId: String,
    overrideType: OverrideType,
    theme: Theme,
    inheritedProperties: List<String>,
    description: String?,
    createdAt: Long,
    modifiedAt: Long
)
```

**Supported Properties**:
- colorScheme
- typography
- shapes
- spacing
- elevation
- material
- animation

### ThemeSync
**Role**: Cloud synchronization manager

**Features**:
- Bidirectional sync (local ↔ cloud)
- Conflict resolution strategies
- Sync state monitoring (Flow)
- Version tracking

**Conflict Resolution**:
- **LastWriteWinsResolver**: Use newest timestamp
- **LocalFirstResolver**: Prefer local changes
- **CloudFirstResolver**: Prefer cloud changes
- **ManualConflictResolver**: User chooses

## Data Flow

### 1. App Requests Theme

```
App calls getTheme(appId)
        ↓
ThemeManager checks for override
        ↓
    ┌───────┴────────┐
    │                │
    ▼                ▼
No Override     Has Override
    ▼                ▼
Return          Check Type
Universal           ↓
Theme         ┌─────┴─────┐
              ▼           ▼
           FULL        PARTIAL
              ▼           ▼
          Return      Merge with
          Override    Universal
          Theme       Theme
```

### 2. Theme Change Propagation

```
setUniversalTheme(newTheme)
        ↓
Save to Repository
        ↓
Update _universalTheme StateFlow
        ↓
Notify all apps
        ↓
    ┌───────┴────────────────┐
    │                        │
    ▼                        ▼
Apps without         Apps with PARTIAL
Override             Override
    ▼                        ▼
Receive new          Recalculate
Universal            Effective
Theme                Theme
```

### 3. Cloud Sync Flow

```
Local Change
    ↓
Save to Local Repository
    ↓
Trigger syncToCloud()
    ↓
Upload to Cloud Repository
    ↓
Cloud Change Detected
    ↓
Trigger syncFromCloud()
    ↓
Download from Cloud
    ↓
Conflict Resolution
    ↓
Update Local Repository
    ↓
Notify ThemeManager
    ↓
Propagate to Apps
```

## Theme Resolution Algorithm

```kotlin
fun resolveTheme(appId: String): Theme {
    // 1. Check for override
    val override = appOverrides[appId]
    if (override == null) {
        return universalTheme
    }

    // 2. Handle full override
    if (override.overrideType == FULL) {
        return override.theme
    }

    // 3. Handle partial override
    // (Current implementation returns override.theme)
    // (Future: Merge properties based on inheritedProperties)
    return mergeThemes(universalTheme, override)
}
```

## Integration Points

### With AvaElements Core

```kotlin
// ThemeManager uses Theme data classes from AvaElements Core
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.core.Themes
import com.augmentalis.avaelements.core.ColorScheme
import com.augmentalis.avaelements.core.Typography
// etc.
```

### With App UI Layer

```kotlin
// Compose integration
@Composable
fun MyApp() {
    val theme by ThemeManager
        .observeTheme("com.augmentalis.myapp")
        .collectAsState()

    MaterialTheme(
        colorScheme = theme.colorScheme.toCompose(),
        typography = theme.typography.toCompose(),
        shapes = theme.shapes.toCompose()
    ) {
        AppContent()
    }
}
```

### With Settings UI

```kotlin
// Settings screen for theme selection
fun onUserSelectsTheme(theme: Theme) {
    lifecycleScope.launch {
        ThemeManager.setUniversalTheme(theme)
        // All apps update automatically via observeTheme()
    }
}
```

## State Management

### ThemeManager State

```kotlin
// Universal theme (single source of truth)
private val _universalTheme = MutableStateFlow<Theme>(default)
val universalTheme: StateFlow<Theme>

// Per-app overrides
private val appOverrides = mutableMapOf<String, ThemeOverride>()

// Per-app theme flows (for observation)
private val themeFlows = mutableMapOf<String, MutableStateFlow<Theme>>()
```

### Reactive Updates

```
User changes theme
        ↓
ThemeManager._universalTheme.value = newTheme
        ↓
All collectors receive update
        ↓
    ┌───────┴────────┐
    │                │
    ▼                ▼
observeUniversalTheme()  observeTheme(appId)
    ▼                        ▼
Settings UI             App UI
Updates                 Updates
```

## Performance Considerations

### Caching
- ThemeManager holds themes in memory
- No repeated disk reads for same theme
- StateFlow ensures single instance per app

### Lazy Loading
- Theme flows created on-demand
- Only load app themes when requested
- Batch operations for import/export

### Conflict Resolution
- Last-write-wins by default (fastest)
- Manual resolution only when needed
- Timestamps tracked for all changes

## Security Considerations

### Local Storage
- JSON files in app-private directory
- No sensitive data (themes are UI config)
- Validated during deserialization

### Cloud Sync
- Repository interface allows encryption
- Authentication handled by cloud provider
- Conflict resolution prevents data loss

## Testing Strategy

### Unit Tests
```kotlin
// Use InMemoryThemeRepository for tests
val repository = InMemoryThemeRepository()
ThemeManager.initialize(repository)

// Test theme resolution
ThemeManager.setUniversalTheme(Themes.Material3Light)
assertEquals(Themes.Material3Light, ThemeManager.getTheme("app1"))

ThemeManager.setAppTheme("app1", Themes.iOS26LiquidGlass)
assertEquals(Themes.iOS26LiquidGlass, ThemeManager.getTheme("app1"))
```

### Integration Tests
```kotlin
// Test with LocalThemeRepository
val repository = LocalThemeRepository()
ThemeManager.initialize(repository)

// Set themes
ThemeManager.setUniversalTheme(Themes.Material3Light)
ThemeManager.setAppTheme("app1", Themes.iOS26LiquidGlass)

// Restart (clear memory)
ThemeManager.loadThemes()

// Verify persistence
assertEquals(Themes.Material3Light, ThemeManager.getUniversalTheme())
assertEquals(Themes.iOS26LiquidGlass, ThemeManager.getTheme("app1"))
```

## Future Enhancements

### 1. Property-Level Merging
Implement true partial overrides at property level:
```kotlin
// Merge specific color properties
val merged = ColorScheme(
    primary = override.colorScheme.primary,
    secondary = universal.colorScheme.secondary, // inherited
    // ...
)
```

### 2. Theme Templates
Predefined override templates:
```kotlin
ThemeTemplates.DarkMode
ThemeTemplates.HighContrast
ThemeTemplates.LargeText
```

### 3. Theme Validation
Validate themes for accessibility:
```kotlin
fun validateTheme(theme: Theme): ValidationResult {
    // Check contrast ratios
    // Check font sizes
    // Check touch targets
}
```

### 4. Theme Preview
Generate preview images:
```kotlin
fun generatePreview(theme: Theme): Bitmap {
    // Render sample UI with theme
}
```

### 5. Theme History
Track theme changes:
```kotlin
fun getThemeHistory(appId: String): List<ThemeChange> {
    // Return timeline of changes
}
```

## Versioning and Migration

### Current Version: 1.0.0

### Future Versions
- **1.1.0**: Property-level merging
- **1.2.0**: Theme templates
- **1.3.0**: Cloud sync implementation
- **2.0.0**: Breaking changes if needed

### Migration Strategy
```kotlin
// Export themes before update
val backup = ThemeManager.exportThemes()

// Update ThemeManager

// Import themes after update
ThemeManager.importThemes(backup)
```
