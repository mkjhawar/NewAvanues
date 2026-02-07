# Developer Manual - Chapter 90: Unified Adaptive Settings Architecture

**Version:** 1.0.0
**Date:** 2026-02-08
**Author:** AVA Development Team
**Status:** Reference Documentation
**Module:** apps/avanues/ (settings subsystem)
**Branch:** 060226-1-consolidation-framework
**Commit:** 321786ba

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Foundation Layer — ModuleSettingsProvider](#3-foundation-layer)
4. [AvanueUI Layer — Shared Components](#4-avanueui-layer)
5. [App Layer — ComposableSettingsProvider](#5-app-layer)
6. [Provider Implementations](#6-provider-implementations)
7. [Hilt Multibinding](#7-hilt-multibinding)
8. [Responsive Layout — ListDetailPaneScaffold](#8-responsive-layout)
9. [Smart Glasses Adaptation](#9-smart-glasses-adaptation)
10. [Voice Navigation](#10-voice-navigation)
11. [How to Add a New Module's Settings](#11-how-to-add-new-settings)
12. [File Locations](#12-file-locations)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. Overview

The Unified Adaptive Settings system replaces the monolithic `SettingsScreen.kt` with a **module-provider architecture** that dynamically discovers and renders settings from any module. It adapts to phone, tablet, foldable, multi-window, and smart glasses (monocular + binocular) form factors.

### Core Principles

- **Modular**: Each module registers its own settings via a provider — no central screen knows about individual settings
- **Adaptive**: Material3 `ListDetailPaneScaffold` auto-adapts phone/tablet/foldable; custom layouts for smart glasses
- **Discoverable**: Hilt `@IntoSet` multibinding — add a provider, it shows up automatically
- **Searchable**: Each provider declares searchable entries with keywords for cross-module search
- **Voice-First on Glasses**: VoiceOSCore handles all voice navigation (WearHF/WearML are disabled)

### What It Replaced

| Before | After |
|--------|-------|
| `SettingsScreen.kt` — monolithic, 5 hardcoded sections | `UnifiedSettingsScreen.kt` — discovers providers dynamically |
| Inline `SettingsViewModel` inside the screen file | `UnifiedSettingsViewModel` — Hilt ViewModel with Set injection |
| No responsive layout (phone-only) | `ListDetailPaneScaffold` — phone/tablet/foldable/multi-window |
| No smart glasses support | `GlassesSettingsLayout` — monocular + binocular modes |
| Separate browser settings route | `WebAvanueSettingsProvider` — integrated into unified screen |

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Foundation (commonMain) — Pure KMP, no Compose             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ ModuleSettingsProvider interface                      │    │
│  │ SettingsSection data class                           │    │
│  │ SearchableSettingEntry data class                    │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  AvanueUI (commonMain) — Shared Compose Components          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ SettingsSwitchRow, SettingsSliderRow                  │    │
│  │ SettingsNavigationRow, SettingsDropdownRow<T>         │    │
│  │ SettingsSectionHeader, SettingsGroupCard              │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  App Layer (androidMain) — Providers + Unified Screen       │
│  ┌──────────────────┐  ┌──────────────────────────────┐    │
│  │ ComposableSettings│  │ UnifiedSettingsScreen         │    │
│  │ Provider interface│  │ ├─ StandardSettingsScreen     │    │
│  ├──────────────────┤  │ ├─ GlassesSettingsLayout      │    │
│  │ Providers:        │  │ │  ├─ MonocularSettingsScreen │    │
│  │ ├─ Permissions    │  │ │  └─ BinocularSettingsScreen │    │
│  │ ├─ VoiceCursor    │  │ └─ SettingsDisplayMode enum   │    │
│  │ ├─ VoiceControl   │  └──────────────────────────────┘    │
│  │ ├─ WebAvanue      │                                      │
│  │ └─ System         │  ┌──────────────────────────────┐    │
│  └──────────────────┘  │ Hilt SettingsModule            │    │
│                         │ @Provides @IntoSet per provider│    │
│                         └──────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

```
Hilt @IntoSet → Set<ComposableSettingsProvider> → UnifiedSettingsViewModel
    → UnifiedSettingsScreen detects SettingsDisplayMode
        → STANDARD: ListDetailPaneScaffold (phone/tablet/foldable)
        → GLASS_MONOCULAR: Paginated single-setting view
        → GLASS_BINOCULAR: Simplified single-pane list
```

---

## 3. Foundation Layer — ModuleSettingsProvider

**File:** `Modules/Foundation/src/commonMain/kotlin/com/augmentalis/foundation/settings/ModuleSettingsProvider.kt`

Pure KMP interface — no Compose dependency. Any module on any platform can implement this.

```kotlin
interface ModuleSettingsProvider {
    val moduleId: String          // Unique ID, e.g., "voicecursor"
    val displayName: String       // Human label, e.g., "VoiceCursor"
    val iconName: String          // Material icon name for cross-platform
    val sortOrder: Int            // Lower = higher. Use multiples of 100.
    val isEnabled: Boolean        // Default true. Set false to hide.
    val sections: List<SettingsSection>
    val searchableEntries: List<SearchableSettingEntry>
}

data class SettingsSection(
    val id: String,               // e.g., "cursor", "general"
    val title: String,            // Display title
    val sortOrder: Int = 0        // Sort within provider
)

data class SearchableSettingEntry(
    val key: String,              // Unique key, e.g., "dwell_click_enabled"
    val displayName: String,      // What the user sees in search results
    val sectionId: String,        // Which section this belongs to
    val keywords: List<String>    // Search keywords, e.g., ["click", "auto"]
)
```

### Sort Order Convention

| Range | Category | Example |
|-------|----------|---------|
| 100-199 | Permissions / Security | PermissionsSettingsProvider (100) |
| 200-299 | Core Accessibility | VoiceCursorSettingsProvider (200) |
| 300-399 | Voice / Input | VoiceControlSettingsProvider (300) |
| 400-499 | Apps / Modules | WebAvanueSettingsProvider (400) |
| 500-599 | System | SystemSettingsProvider (500) |
| 600-699 | Developer / Debug | Future providers |
| 700+ | Third-party / Extensions | Future AVU plugin settings |

---

## 4. AvanueUI Layer — Shared Components

**File:** `Modules/AvanueUI/src/commonMain/kotlin/com/avanueui/settings/SettingsComponents.kt`

Six reusable components consolidating duplicates from app and WebAvanue:

| Component | Purpose | Key Parameters |
|-----------|---------|----------------|
| `SettingsSwitchRow` | Toggle setting with switch | title, subtitle?, icon?, checked, onCheckedChange |
| `SettingsSliderRow` | Numeric setting with slider | title, value, valueRange, steps?, valueLabel?, onValueChange |
| `SettingsNavigationRow` | Clickable row that navigates | title, subtitle?, icon?, currentValue?, onClick |
| `SettingsDropdownRow<T>` | Dropdown selection | title, selected, options, optionLabel, onSelected |
| `SettingsSectionHeader` | Section title | title |
| `SettingsGroupCard` | iOS-style rounded group card | content: @Composable ColumnScope.() -> Unit |

All components accept optional `icon: ImageVector?` — app settings use icons, WebAvanue settings don't. Uses Material3 `ListItem` internally for consistent spacing and Material3 Adaptive compliance.

### Important: JetBrains Compose Multiplatform Compatibility

The project uses JetBrains Compose Multiplatform 1.6.11, which uses an older Material3 API:
- `ExposedDropdownMenuBox.menuAnchor()` takes **no parameters** (no `MenuAnchorType`)
- Always test AvanueUI changes against the Compose BOM version in `gradle/libs.versions.toml`

---

## 5. App Layer — ComposableSettingsProvider

**File:** `apps/avanues/.../ui/settings/ComposableSettingsProvider.kt`

Extends the KMP interface with Compose rendering. Lives at app level where both Hilt and Compose are available.

```kotlin
interface ComposableSettingsProvider : ModuleSettingsProvider {
    @Composable
    fun SectionContent(sectionId: String)  // Renders section UI

    @Composable
    fun ModuleIcon(): ImageVector           // Returns icon for list
}
```

The `SectionContent()` renders identically regardless of display mode — only the scaffold/chrome adapts. This means providers don't need to know about smart glasses vs phone vs tablet.

---

## 6. Provider Implementations

**Directory:** `apps/avanues/.../ui/settings/providers/`

### PermissionsSettingsProvider (sortOrder: 100)

- **Sections:** permissions
- **Settings:** Accessibility Service status + launch, Overlay Permission status + launch
- **Repository:** None (uses system intents)
- **Key:** Uses `VoiceAvanueAccessibilityService.isEnabled()` and `Settings.canDrawOverlays()`

### VoiceCursorSettingsProvider (sortOrder: 200)

- **Sections:** cursor
- **Settings:** Dwell Click toggle, Dwell Delay slider (500-3000ms), Cursor Smoothing toggle
- **Repository:** `AvanuesSettingsRepository` (DataStore)
- **Key:** Uses `rememberCoroutineScope()` for DataStore updates

### VoiceControlSettingsProvider (sortOrder: 300)

- **Sections:** voice
- **Settings:** Voice Feedback toggle, Wake Word navigation row
- **Repository:** `AvanuesSettingsRepository` (DataStore)

### WebAvanueSettingsProvider (sortOrder: 400)

- **Sections:** general, appearance, privacy, more (4 sections → tabbed layout)
- **Settings:** 60+ settings from WebAvanue's full SettingsScreen
- **Repository:** `BrowserRepository` (SQLDelight)
- **Key:** Wraps existing WebAvanue `SettingsScreen` composable via `BrowserSettingsViewModel(repository)`. Phase-1 migration — no storage layer changes.
- **Search entries:** 9 searchable entries (search engine, homepage, JavaScript, etc.)

### SystemSettingsProvider (sortOrder: 500)

- **Sections:** system
- **Settings:** Start on Boot toggle
- **Repository:** `AvanuesSettingsRepository` (DataStore)

---

## 7. Hilt Multibinding

**File:** `apps/avanues/.../di/SettingsModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides @IntoSet
    fun providePermissionsSettings(): ComposableSettingsProvider = ...

    @Provides @IntoSet
    fun provideVoiceCursorSettings(repo: AvanuesSettingsRepository): ComposableSettingsProvider = ...
    // ... one per provider
}
```

### Critical: @JvmSuppressWildcards

The ViewModel MUST use `@JvmSuppressWildcards`:

```kotlin
@HiltViewModel
class UnifiedSettingsViewModel @Inject constructor(
    val providers: Set<@JvmSuppressWildcards ComposableSettingsProvider>
) : ViewModel()
```

Without this annotation, Hilt generates wildcard types (`Set<? extends ComposableSettingsProvider>`) that fail to match `@IntoSet` bindings. This will cause a runtime crash with "missing binding" error.

---

## 8. Responsive Layout — ListDetailPaneScaffold

**Dependency:** `androidx.compose.material3.adaptive` v1.0.0-beta01

### Layout Behavior

| Device State | Behavior |
|---|---|
| Phone portrait | Single pane: module list → detail (stack navigation) |
| Phone landscape | Two-pane: nav (40%) \| content (60%) |
| Tablet / desktop | Two-pane: nav (30%) \| content (70%) always visible |
| Foldable unfolded | Two-pane split at hinge |
| Multi-window | Adapts via WindowSizeClass |

### List Pane — iOS-style Grouped Cards

The module list uses `SettingsGroupCard` to group related providers:
- **Core** (sortOrder < 400): Permissions, VoiceCursor, Voice Control
- **Browser** (moduleId == "webavanue"): WebAvanue
- **System** (moduleId == "system"): System settings

The **About footer** (Version + Licenses) is NOT a provider — it's fixed at the bottom of the list pane. The Version row has a 7-tap easter egg that opens the Developer Console.

### Detail Pane

- **Single section:** Renders content directly
- **Multiple sections:** `ScrollableTabRow` for section tabs + content below
- **Empty state:** "Select a module" text (visible on tablet when nothing selected)

---

## 9. Smart Glasses Adaptation

**File:** `apps/avanues/.../ui/settings/GlassesSettingsLayout.kt`

### Detection

At screen entry, `detectSettingsDisplayMode()` queries DeviceManager:

```kotlin
enum class SettingsDisplayMode {
    STANDARD,          // Phone/tablet — ListDetailPaneScaffold
    GLASS_MONOCULAR,   // Single setting at a time, voice nav
    GLASS_BINOCULAR    // Simplified single pane, GlassAvanue theme
}
```

Detection chain:
1. `SmartGlassDetection(context).getSmartGlassType()` → if `UNKNOWN`, return `STANDARD`
2. `DeviceDetection(context).detectARGlasses()` → check `isMonocular` / `isBinocular`
3. Fallback by known glass type (RealWear/Google Glass → monocular; XREAL/Rokid/Epson → binocular)

### Monocular Mode (RealWear, Vuzix M400, Google Glass, Even G1)

- **Resolution:** 640x200 to 1280x720, FOV: 16-20 degrees
- **Layout:** Paginated — one module/setting per screen, extra-large text (20sp+)
- **Navigation:** Module chooser with PaginationDots → single setting detail
- **Voice hints:** "NEXT", "PREVIOUS", "SELECT", "TOGGLE", "BACK"

### Binocular Mode (XREAL, Rokid, Virture, Almer, Epson Moverio)

- **Resolution:** 854x480 to 1920x1080, FOV: 28-50 degrees
- **Layout:** Simplified single-pane scrollable list, 3-5 settings visible at once
- **Theme:** Semi-transparent glass cards (70% opacity), GlassAvanue styling
- **Navigation:** Module list → tap/voice → section content (stack navigation)

### Manufacturer Accent Colors

```kotlin
SmartGlassType.REALWEAR       → #FF6B00 (orange)
SmartGlassType.VUZIX          → #0066CC (blue)
SmartGlassType.ROKID          → #00BCD4 (cyan)
SmartGlassType.EPSON          → #003399 (blue)
SmartGlassType.XREAL          → #FF0050 (red/pink)
SmartGlassType.GOOGLE_GLASS   → #4285F4 (Google blue)
SmartGlassType.MAGIC_LEAP     → #6C3CFF (purple)
SmartGlassType.MICROSOFT_HOLOLENS → #0078D4 (MS blue)
SmartGlassType.TCL            → #E60012 (red)
SmartGlassType.PICO           → #00B4D8 (blue)
SmartGlassType.HTC            → #69BE28 (green)
Default                        → ColorTokens.Primary
```

---

## 10. Voice Navigation

**VoiceOSCore handles ALL voice navigation.** WearHF/WearML are DISABLED across the project.

- Settings screen registers contextual commands via `StaticCommandRegistry`
- Manufacturer native commands (e.g., RealWear "SELECT ITEM", "NAVIGATE BACK") are added as synonyms
- Vuzix Speech SDK: If `com.vuzix.speech.sdk` package is detected, use their Vivoka/Cerence backend instead of downloading the 100+MB library

### Future Work

- Register dynamic voice commands per visible setting (e.g., "toggle dwell click")
- Head-gesture scroll on monocular devices that support it
- Touchpad/hand-tracking navigation for binocular devices

---

## 11. How to Add a New Module's Settings

This is the most important section for future development. Follow these steps:

### Step 1: Create a Provider

```kotlin
// apps/avanues/.../ui/settings/providers/MyModuleSettingsProvider.kt

class MyModuleSettingsProvider @Inject constructor(
    private val repository: MyRepository  // If needed
) : ComposableSettingsProvider {

    override val moduleId = "mymodule"
    override val displayName = "My Module"
    override val iconName = "Extension"
    override val sortOrder = 350  // See sort order table in Section 3

    override val sections = listOf(
        SettingsSection(id = "general", title = "General"),
        SettingsSection(id = "advanced", title = "Advanced", sortOrder = 1)
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "my_setting",
            displayName = "My Setting",
            sectionId = "general",
            keywords = listOf("setting", "config", "option")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        when (sectionId) {
            "general" -> GeneralSection()
            "advanced" -> AdvancedSection()
        }
    }

    @Composable
    private fun GeneralSection() {
        val settings by repository.settings.collectAsState(initial = MyDefaults())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsSwitchRow(
                title = "My Setting",
                subtitle = "Description of what it does",
                icon = Icons.Default.Extension,
                checked = settings.mySetting,
                onCheckedChange = { scope.launch { repository.updateMySetting(it) } }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Extension
}
```

### Step 2: Register in Hilt

```kotlin
// In apps/avanues/.../di/SettingsModule.kt

@Provides
@IntoSet
fun provideMyModuleSettings(
    repository: MyRepository
): ComposableSettingsProvider =
    MyModuleSettingsProvider(repository)
```

### Step 3: That's It

The unified screen picks it up automatically. No changes needed to `UnifiedSettingsScreen`, `MainActivity`, or any navigation code. The provider will:
- Appear in the module list sorted by `sortOrder`
- Be searchable via `searchableEntries`
- Render with tabs if it has multiple sections
- Work on all form factors (phone, tablet, foldable, smart glasses)

### Grouping

To add the module to a specific group in the list pane, update `SettingsModuleList()` in `UnifiedSettingsScreen.kt` with a new filter. Currently:
- sortOrder < 400 → "Core" group
- moduleId == "webavanue" → "Browser" group
- moduleId == "system" → "System" group

---

## 12. File Locations

### New Files (created in this architecture)

| File | Purpose |
|------|---------|
| `Modules/Foundation/.../settings/ModuleSettingsProvider.kt` | KMP interface |
| `Modules/AvanueUI/.../settings/SettingsComponents.kt` | 6 shared Compose components |
| `apps/avanues/.../settings/ComposableSettingsProvider.kt` | Compose-aware interface |
| `apps/avanues/.../settings/UnifiedSettingsViewModel.kt` | Hilt ViewModel with Set injection |
| `apps/avanues/.../settings/UnifiedSettingsScreen.kt` | Adaptive screen + standard layout |
| `apps/avanues/.../settings/GlassesSettingsLayout.kt` | Monocular + binocular layouts |
| `apps/avanues/.../settings/providers/PermissionsSettingsProvider.kt` | Permissions provider |
| `apps/avanues/.../settings/providers/VoiceCursorSettingsProvider.kt` | VoiceCursor provider |
| `apps/avanues/.../settings/providers/VoiceControlSettingsProvider.kt` | VoiceControl provider |
| `apps/avanues/.../settings/providers/WebAvanueSettingsProvider.kt` | WebAvanue provider |
| `apps/avanues/.../settings/providers/SystemSettingsProvider.kt` | System provider |
| `apps/avanues/.../di/SettingsModule.kt` | Hilt multibinding |

### Modified Files

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Added material3-adaptive v1.0.0-beta01 |
| `apps/avanues/build.gradle.kts` | Added adaptive + DeviceManager deps |
| `apps/avanues/.../MainActivity.kt` | Rewired routes to UnifiedSettingsScreen |

### Deleted Files

| File | Replaced By |
|------|-------------|
| `apps/avanues/.../settings/SettingsScreen.kt` | UnifiedSettingsScreen + providers |

### Key Reference Files (read-only)

| File | What It Provides |
|------|-----------------|
| `Modules/DeviceManager/.../SmartGlassDetection.kt` | SmartGlassType enum (14 types) |
| `Modules/DeviceManager/.../DeviceDetection.kt` | ARDisplayInfo (isMonocular/isBinocular) |
| `Modules/AvanueUI/.../DesignTokens.kt` | ColorTokens, TypographyTokens, etc. |

---

## 13. Troubleshooting

### "Missing binding for Set<ComposableSettingsProvider>"

**Cause:** Missing `@JvmSuppressWildcards` on the Set injection in the ViewModel.
**Fix:** Ensure `Set<@JvmSuppressWildcards ComposableSettingsProvider>` in constructor.

### "Unresolved reference: MenuAnchorType"

**Cause:** JetBrains Compose Multiplatform 1.6.11 uses older Material3 API.
**Fix:** Use `.menuAnchor()` without parameters. Do NOT use `MenuAnchorType.PrimaryNotEditable`.

### "Unresolved reference: OceanDesignTokens"

**Cause:** `OceanDesignTokens` is in `com.augmentalis.avamagic.ui.foundation` (AvanueUI Foundation submodule), not `com.avanueui`.
**Fix:** Use `com.avanueui.ColorTokens.Primary` instead for the default accent color.

### Settings not appearing in unified screen

**Checklist:**
1. Provider class implements `ComposableSettingsProvider`
2. `@Provides @IntoSet` method exists in `SettingsModule.kt`
3. All constructor dependencies are available in Hilt
4. `isEnabled` returns `true` (default)

### Smart glasses mode not activating

**Checklist:**
1. DeviceManager module is a dependency in `apps/avanues/build.gradle.kts`
2. `SmartGlassDetection` correctly identifies the device (check `getSmartGlassType()`)
3. `DeviceDetection.detectARGlasses()` returns non-null ARDisplayInfo
4. Falls back to `STANDARD` if detection fails (by design)

---

## Cross-References

- **Chapter 88:** Avanues Consolidated App — Activity-alias routing, AvanueMode enum
- **Chapter 89:** AvaUI Design System — DesignTokens, GlassmorphicComponents, themes
- **Plan:** `docs/plans/Avanues/Avanues-Plan-UnifiedAdaptiveSettings-260208-V1.md`
- **Smart Glasses:** DeviceManager module documentation
- **Voice Navigation:** VoiceOSCore — StaticCommandRegistry, VoiceAvanueAccessibilityService

---

*End of Chapter 90*
