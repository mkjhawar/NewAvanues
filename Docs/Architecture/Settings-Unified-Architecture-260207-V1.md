# Unified Settings Architecture Protocol

**Applies to:** All NewAvanues modules with settings screens
**Status:** Mandatory for new settings, migration for existing

## Problem

Settings are scattered across modules with no responsive layout, no foldable/multi-window support, and no way for modules to register settings dynamically.

## Architecture: Module Providers with Composable Slots + Adaptive Scaffold

### Foundation (commonMain)

```kotlin
// In shared/commonMain
interface ModuleSettingsProvider {
    val moduleId: String
    val displayName: String
    val icon: ImageVector
    val sections: List<SettingsSection>
    val searchableItems: List<SearchableSettingItem>
}

data class SettingsSection(
    val title: String,
    val content: @Composable () -> Unit  // Module controls its own UI
)

data class SearchableSettingItem(
    val label: String,
    val keywords: List<String>,
    val sectionTitle: String
)
```

### App Layer (androidMain)

```kotlin
// UnifiedSettingsScreen using Material3 Adaptive
@Composable
fun UnifiedSettingsScreen(providers: Set<ModuleSettingsProvider>) {
    ListDetailPaneScaffold(
        // Left pane: module list + section list
        // Right pane: section content (from module's composable)
        // Search bar filters across all modules
        // WindowSizeClass drives compact/medium/expanded
        // FoldingFeature detection for hinge-aware split
    )
}
```

### Module Registration (Hilt)

```kotlin
// Each module provides its settings via @IntoSet
@Module
@InstallIn(SingletonComponent::class)
object WebAvanueSettingsModule {
    @Provides
    @IntoSet
    fun provideSettings(): ModuleSettingsProvider = WebAvanueSettingsProvider()
}
```

### Shared SettingsComponents (AvanueUI)

Standard composables for visual consistency:

| Component | Purpose |
|-----------|---------|
| `SettingsSwitchRow` | Toggle setting with label and description |
| `SettingsSliderRow` | Numeric range setting |
| `SettingsNavRow` | Navigation to sub-screen |
| `SettingsSectionHeader` | Section divider with title |
| `SettingsPickerDialog` | Selection from list of options |
| `SettingsTextFieldRow` | Text input setting |
| `SettingsInfoRow` | Read-only information display |

## Responsive Layout

| Device State | Layout |
|-------------|--------|
| Phone portrait | Single pane: module list → section list → section content (stack navigation) |
| Phone landscape | Two-pane: section nav (narrow) + content |
| Tablet / desktop | Two-pane: module+section nav + content (always visible) |
| Foldable unfolded | Two-pane split at hinge |
| Foldable folded | Single pane (phone behavior) |
| Multi-window | Adapts via WindowSizeClass - narrow = single, wide = two-pane |

## Dependencies

```kotlin
// Material3 Adaptive
implementation("androidx.compose.material3.adaptive:adaptive-layout:1.1.0")
implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.1.0")

// Window size and foldable detection
implementation("androidx.window:window:1.3.0")
implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
```

## Migration Checklist

For each module with existing settings:

1. Create `{Module}SettingsProvider` implementing `ModuleSettingsProvider`
2. Move existing settings UI into section composables
3. Use shared `SettingsComponents` for standard rows
4. Register via Hilt `@IntoSet` multibinding
5. Add searchable items for cross-module search
6. Remove old `SettingsScreen` navigation entry
7. Test on phone, tablet, and foldable emulators

## Current State & Migration Priority

| Module | Current | Priority |
|--------|---------|----------|
| App-level | SettingsScreen.kt (LazyColumn, M3 ListItem) | P1 - Host framework |
| WebAvanue | Own SettingsScreen.kt (11 sections) | P1 - Most complex |
| VoiceCursor | 3 items in app Settings | P2 - Simple migration |
| Gaze | No settings | P3 - New provider when needed |
| AVID | No settings | P3 - New provider when needed |
| AVU | No settings | P3 - New provider when needed |

## Rules

1. ALL new settings screens MUST use this architecture
2. Modules MUST NOT create standalone SettingsScreen composables
3. Shared `SettingsComponents` MUST be used for standard UI patterns
4. Custom composables allowed only for complex/unique settings (e.g., XR section)
5. All settings MUST be searchable via `searchableItems`
6. Responsive layout MUST work on all device states listed above
