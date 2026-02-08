# Avanues Unified Adaptive Settings - Implementation Plan

**Date:** 2026-02-08
**Branch:** 060226-1-consolidation-framework
**Commit:** 321786ba
**Status:** COMPLETED
**Mode:** .yolo (auto-chain from /i.implement)
**Scope:** 4 phases, 16 files created/modified, 1976 insertions

---

## Overview

Replaced monolithic `SettingsScreen.kt` with a module-provider architecture that dynamically discovers and renders settings from any module. Adapts to all form factors including smart glasses.

**Platforms:** Android (app layer); Foundation + AvanueUI are KMP
**Developer Manual:** Chapter 90

---

## Architecture Summary

```
Foundation (KMP)      → ModuleSettingsProvider interface
AvanueUI (KMP+Compose)→ 6 shared SettingsComponents
App providers/         → 5 ComposableSettingsProvider implementations
App settings/          → UnifiedSettingsScreen + GlassesSettingsLayout
App di/                → Hilt @IntoSet multibinding
```

### Key Design Decisions

1. **Pure KMP interface in Foundation** — no Compose dependency, can be implemented by any module on any platform
2. **Hilt @IntoSet multibinding** — providers auto-discovered, no central registry to maintain
3. **@JvmSuppressWildcards mandatory** — without it, Hilt generates wildcard types that break Set injection
4. **Material3 Adaptive ListDetailPaneScaffold** — auto-adapts phone/tablet/foldable/desktop
5. **SettingsDisplayMode enum** — detected at screen entry via DeviceManager, falls back to STANDARD
6. **About footer is NOT a provider** — fixed at bottom of list pane (Version 7-tap + Licenses)
7. **WebAvanue settings wrapped as-is** — Phase-1 migration, no storage layer changes

---

## Phase 1: Foundation + AvanueUI + Dependencies (COMPLETED)

### Tasks

| # | Task | File | Status |
|---|------|------|--------|
| 1.1 | Create ModuleSettingsProvider interface | Foundation/.../settings/ModuleSettingsProvider.kt | Done |
| 1.2 | Create shared SettingsComponents | AvanueUI/.../settings/SettingsComponents.kt | Done |
| 1.3 | Add Material3 Adaptive to version catalog | gradle/libs.versions.toml | Done |
| 1.4 | Add deps to app build.gradle | apps/avanues/build.gradle.kts | Done |

### Issues Encountered

- **MenuAnchorType unresolved**: JetBrains Compose 1.6.11 uses older Material3 API. Fixed by using `.menuAnchor()` without parameters.

---

## Phase 2: Provider Infrastructure (COMPLETED)

### Tasks

| # | Task | File | Status |
|---|------|------|--------|
| 2.1 | Create ComposableSettingsProvider interface | settings/ComposableSettingsProvider.kt | Done |
| 2.2 | Create UnifiedSettingsViewModel | settings/UnifiedSettingsViewModel.kt | Done |
| 2.3 | Create PermissionsSettingsProvider | settings/providers/PermissionsSettingsProvider.kt | Done |
| 2.4 | Create VoiceCursorSettingsProvider | settings/providers/VoiceCursorSettingsProvider.kt | Done |
| 2.5 | Create VoiceControlSettingsProvider | settings/providers/VoiceControlSettingsProvider.kt | Done |
| 2.6 | Create WebAvanueSettingsProvider | settings/providers/WebAvanueSettingsProvider.kt | Done |
| 2.7 | Create SystemSettingsProvider | settings/providers/SystemSettingsProvider.kt | Done |
| 2.8 | Create Hilt SettingsModule | di/SettingsModule.kt | Done |

---

## Phase 3: Unified Settings Screen (COMPLETED)

### Tasks

| # | Task | File | Status |
|---|------|------|--------|
| 3.1 | Create UnifiedSettingsScreen with ListDetailPaneScaffold | settings/UnifiedSettingsScreen.kt | Done |
| 3.2 | Create GlassesSettingsLayout (monocular + binocular) | settings/GlassesSettingsLayout.kt | Done |

### Issues Encountered

- **OceanDesignTokens.accent doesn't exist**: Package is `com.augmentalis.avamagic.ui.foundation`, not `com.avanueui`. Fixed by using `ColorTokens.Primary`.

---

## Phase 4: Integration (COMPLETED)

### Tasks

| # | Task | File | Status |
|---|------|------|--------|
| 4.1 | Rewire MainActivity routes | MainActivity.kt | Done |
| 4.2 | Remove BROWSER_SETTINGS from AvanueMode | MainActivity.kt | Done |
| 4.3 | Delete old SettingsScreen.kt | settings/SettingsScreen.kt | Deleted |

---

## Dependencies Added

```toml
# gradle/libs.versions.toml
material3-adaptive = "1.0.0-beta01"
material3-adaptive = { module = "androidx.compose.material3.adaptive:adaptive" }
material3-adaptive-layout = { module = "...adaptive-layout" }
material3-adaptive-navigation = { module = "...adaptive-navigation" }
```

```kotlin
// apps/avanues/build.gradle.kts
implementation(project(":Modules:DeviceManager"))
implementation(libs.material3.adaptive)
implementation(libs.material3.adaptive.layout)
implementation(libs.material3.adaptive.navigation)
```

---

## Provider Sort Order Reference

| sortOrder | Provider | Repository |
|-----------|----------|------------|
| 100 | PermissionsSettingsProvider | None (system intents) |
| 200 | VoiceCursorSettingsProvider | AvanuesSettingsRepository |
| 300 | VoiceControlSettingsProvider | AvanuesSettingsRepository |
| 400 | WebAvanueSettingsProvider | BrowserRepository |
| 500 | SystemSettingsProvider | AvanuesSettingsRepository |

---

## Smart Glasses Support

| Mode | Devices | Layout |
|------|---------|--------|
| STANDARD | Phone, Tablet, Foldable | ListDetailPaneScaffold |
| GLASS_MONOCULAR | RealWear, Vuzix M400, Google Glass | Paginated single-setting |
| GLASS_BINOCULAR | XREAL, Rokid, Epson Moverio | Simplified single-pane list |

Detection via: `SmartGlassDetection.getSmartGlassType()` + `DeviceDetection.detectARGlasses()`
Fallback: Always `STANDARD` on detection failure.

---

## Verification

```bash
./gradlew :apps:avanues:compileDebugKotlin  # Kotlin compilation
./gradlew :apps:avanues:assembleDebug         # Full APK build
```

Both passed successfully. APK built in 3m 19s.

---

## Future Work

1. **Voice command registration** — Register dynamic voice commands per visible setting
2. **Deep search** — Navigate directly to specific setting from search results
3. **AVU plugin settings** — Allow .avp plugins to register settings via DSL
4. **GlassAvanue theme integration** — Apply full glassmorphic theme on binocular devices
5. **Manufacturer accent color propagation** — Thread accent through entire settings UI on glass devices
6. **Phase-2 WebAvanue migration** — Move individual WebAvanue settings to native composables instead of wrapping entire SettingsScreen

---

## Cross-References

- Developer Manual Chapter 90: Unified Adaptive Settings Architecture
- Plan (approved): `/Users/manoj_mbpm14/.claude/plans/precious-finding-patterson.md`
- Chapter 88: Avanues Consolidated App
- Chapter 89: AvaUI Design System
