# AVA Overlay AvanueUI v5.1 Migration Plan

**Document:** AVA-Plan-OverlayMigration-260219-V1.md
**Date:** 2026-02-19
**Branch:** Cockpit-Development
**Mode:** .yolo .cot

---

## Overview

Migrate the AVA Overlay module from its bespoke `OceanGlassColors` / `OceanGlass v2.3` design tokens to
AvanueUI v5.1 unified tokens. Concurrently move the source tree from the legacy plain-Android layout
(`src/main/java/`) to the standard KMP layout (`src/androidMain/kotlin/`) and add the AvanueUI module
dependency so the module is ready for future KMP expansion.

**Scope:** Theming and directory structure only. Zero functional changes.
**No new voice commands.** (Overlay is infrastructure — AVID semantics are added where missing.)
**KMP Score stays at ~60%** — this migration does not add commonMain targets.

---

## Context: Current Overlay State

Module path: `Modules/AVA/Overlay/`
Source root: `src/main/java/com/augmentalis/overlay/` (plain Android library, not KMP)
Build file: `Modules/AVA/Overlay/build.gradle.kts` — `android.library` + `kotlin.android` plugins

### Verified Token Usage (from source)

The module defines its own `OceanGlassColors` object inside `GlassEffects.kt`:

```kotlin
object OceanGlassColors {
    val OceanDarker = Color(0xFF0F172A)
    val OceanDark   = Color(0xFF1E293B)
    val OceanMedium = Color(0xFF334155)
    val CoralBlue   = Color(0xFF3B82F6)   // accent — maps to AvanueTheme.colors.primary
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFCBD5E1)
    val Border      = Color(0xFF475569)
}
```

Every UI file imports from this local object. No external deprecated AvanueUI tokens (`OceanGlass.*`,
`SunsetGlass.*`, etc.) are in use — the problem is the opposite: the module has never been wired to
AvanueUI at all.

---

## File Inventory

### Files Requiring Changes

| File | Current Issue | Required Change |
|------|--------------|-----------------|
| `build.gradle.kts` | Missing AvanueUI dependency; no KMP source set config | Add `implementation(project(":Modules:AvanueUI"))` |
| `theme/GlassEffects.kt` | `OceanGlassColors` hardcoded palette; standalone modifier functions | Replace `OceanGlassColors` with `AvanueTheme.colors.*` + `AvanueTheme.glass.*` parameters; keep modifier API intact |
| `ui/GlassMorphicPanel.kt` | `OceanGlassColors.TextPrimary/Secondary/CoralBlue`; unused `MaterialTheme` import | Replace color refs; remove `MaterialTheme` import |
| `ui/VoiceOrb.kt` | `OceanGlassColors.TextPrimary/CoralBlue` in all four state composables | Replace with `AvanueTheme` reads; add AVID semantic on orb |
| `ui/SuggestionChips.kt` | `AssistChip` with `OceanGlassColors` color overrides | Replace color params with `AvanueTheme`; add AVID semantic per chip |

### Files Moved Only (No Content Changes)

| File | Current Path | New Path |
|------|-------------|----------|
| `theme/GlassEffects.kt` | `src/main/java/com/augmentalis/overlay/theme/` | `src/androidMain/kotlin/com/augmentalis/overlay/theme/` |
| `theme/AnimationSpecs.kt` | `src/main/java/com/augmentalis/overlay/theme/` | `src/androidMain/kotlin/com/augmentalis/overlay/theme/` |
| `ui/GlassMorphicPanel.kt` | `src/main/java/com/augmentalis/overlay/ui/` | `src/androidMain/kotlin/com/augmentalis/overlay/ui/` |
| `ui/OverlayComposables.kt` | `src/main/java/com/augmentalis/overlay/ui/` | `src/androidMain/kotlin/com/augmentalis/overlay/ui/` |
| `ui/SuggestionChips.kt` | `src/main/java/com/augmentalis/overlay/ui/` | `src/androidMain/kotlin/com/augmentalis/overlay/ui/` |
| `ui/VoiceOrb.kt` | `src/main/java/com/augmentalis/overlay/ui/` | `src/androidMain/kotlin/com/augmentalis/overlay/ui/` |
| `service/OverlayService.kt` | `src/main/java/com/augmentalis/overlay/service/` | `src/androidMain/kotlin/com/augmentalis/overlay/service/` |
| `service/OverlayPermissionActivity.kt` | `src/main/java/com/augmentalis/overlay/service/` | `src/androidMain/kotlin/com/augmentalis/overlay/service/` |
| `service/DialogQueueManager.kt` | `src/main/java/com/augmentalis/overlay/service/` | `src/androidMain/kotlin/com/augmentalis/overlay/service/` |
| `service/OverlayZIndexManager.kt` | `src/main/java/com/augmentalis/overlay/service/` | `src/androidMain/kotlin/com/augmentalis/overlay/service/` |
| `controller/OverlayController.kt` | `src/main/java/com/augmentalis/overlay/controller/` | `src/androidMain/kotlin/com/augmentalis/overlay/controller/` |
| `controller/VoiceRecognizer.kt` | `src/main/java/com/augmentalis/overlay/controller/` | `src/androidMain/kotlin/com/augmentalis/overlay/controller/` |
| `context/ContextEngine.kt` | `src/main/java/com/augmentalis/overlay/context/` | `src/androidMain/kotlin/com/augmentalis/overlay/context/` |
| `integration/AvaIntegrationBridge.kt` | `src/main/java/com/augmentalis/overlay/integration/` | `src/androidMain/kotlin/com/augmentalis/overlay/integration/` |
| `integration/ChatConnector.kt` | `src/main/java/com/augmentalis/overlay/integration/` | `src/androidMain/kotlin/com/augmentalis/overlay/integration/` |
| `integration/NluConnector.kt` | `src/main/java/com/augmentalis/overlay/integration/` | `src/androidMain/kotlin/com/augmentalis/overlay/integration/` |

### Files With No Changes

| File | Reason |
|------|--------|
| `service/OverlayService.kt` | Service lifecycle only — no color or theme references |
| `controller/OverlayController.kt` | Pure StateFlow state management — no UI tokens |
| `controller/VoiceRecognizer.kt` | Speech recognition logic — no UI tokens |
| `theme/AnimationSpecs.kt` | Animation constants only — no color references |
| `ui/OverlayComposables.kt` | Controller wiring only — delegates to VoiceOrb + GlassMorphicPanel |
| `integration/AvaIntegrationBridge.kt` | Business logic bridge — no UI tokens |
| `integration/ChatConnector.kt` | Network logic — no UI tokens |
| `integration/NluConnector.kt` | NLU logic — no UI tokens |
| `context/ContextEngine.kt` | Context analysis logic — no UI tokens |

---

## Phase 1: Directory Restructure

### Step 1.1 — Create KMP Source Tree

```bash
mkdir -p Modules/AVA/Overlay/src/androidMain/kotlin/com/augmentalis/overlay/theme
mkdir -p Modules/AVA/Overlay/src/androidMain/kotlin/com/augmentalis/overlay/ui
mkdir -p Modules/AVA/Overlay/src/androidMain/kotlin/com/augmentalis/overlay/service
mkdir -p Modules/AVA/Overlay/src/androidMain/kotlin/com/augmentalis/overlay/controller
mkdir -p Modules/AVA/Overlay/src/androidMain/kotlin/com/augmentalis/overlay/context
mkdir -p Modules/AVA/Overlay/src/androidMain/kotlin/com/augmentalis/overlay/integration
```

### Step 1.2 — Move All Source Files

Move each file listed in "Files Moved Only" using `git mv` to preserve history:

```bash
cd Modules/AVA/Overlay

git mv src/main/java/com/augmentalis/overlay/theme/GlassEffects.kt \
       src/androidMain/kotlin/com/augmentalis/overlay/theme/GlassEffects.kt

git mv src/main/java/com/augmentalis/overlay/theme/AnimationSpecs.kt \
       src/androidMain/kotlin/com/augmentalis/overlay/theme/AnimationSpecs.kt

# Repeat for all 16 files — ui/, service/, controller/, context/, integration/
```

### Step 1.3 — Update build.gradle.kts

Change from plain Android library to KMP-compatible layout. The module stays Android-only for now but
uses the KMP source set naming so it is forward-compatible.

**Before** (current):
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    // ...
    // No sourceSets override — defaults to src/main/java
}
```

**After:**
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.augmentalis.overlay"
    compileSdk = 35

    sourceSets {
        getByName("main") {
            java.srcDirs("src/androidMain/kotlin")
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res")
        }
        getByName("test") {
            java.srcDirs("src/androidTest/kotlin")
        }
    }

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // AvanueUI — NEW
    implementation(project(":Modules:AvanueUI"))

    // Core modules (unchanged)
    implementation(project(":Modules:AVA:core:Domain"))
    implementation(project(":Modules:AVA:core:Data"))
    implementation(project(":Modules:AVA:core:Utils"))
    implementation(project(":Modules:AI:Chat"))
    implementation(project(":Modules:AI:LLM"))
    implementation(project(":Modules:AI:NLU"))

    // Kotlin Coroutines (updated to match repo version)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Compose (via BOM — unchanged)
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Android Lifecycle (unchanged)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## Phase 2: AvanueUI v5.1 Token Migration

### 2.1 `theme/GlassEffects.kt`

**Problem:** `OceanGlassColors` is a local hardcoded palette. The `glassBackground` modifier takes a
`Color` parameter defaulting to `Color(0x1E, 0x1E, 0x20)` — the old Ocean Dark shade.

**Solution:** Remove `OceanGlassColors`. The modifier functions accept an optional `color` parameter;
callers that were relying on the default now pass `AvanueTheme.colors.surface` explicitly at call site.
The `orbSolidEffect` and `panelSolidEffect` convenience modifiers are updated to use `AvanueTheme` reads.

**Key token mapping:**

| Old `OceanGlassColors.*` | New `AvanueTheme.*` |
|--------------------------|---------------------|
| `OceanDarker` (panel bg) | `AvanueTheme.colors.background` |
| `OceanDark` (orb bg) | `AvanueTheme.colors.surface` |
| `OceanMedium` (chip bg) | `AvanueTheme.colors.surfaceVariant` |
| `CoralBlue` (accent) | `AvanueTheme.colors.primary` |
| `TextPrimary` | `AvanueTheme.colors.onSurface` |
| `TextSecondary` | `AvanueTheme.colors.onSurface.copy(alpha = 0.7f)` |
| `Border` | `AvanueTheme.colors.outline` |

**Modifier API stays the same.** Only the default `color` parameter changes; no callers need updating.

Because these modifiers are used inside `@Composable` call sites (`orbSolidEffect()`,
`panelSolidEffect()`), the color reads must happen at the composable level. The convenience modifiers
are converted to `@Composable` extension functions, or the color values are passed in from the composable
that calls them — both are valid. The simpler approach is to remove the one-liner convenience modifiers
and inline the modifier chain directly inside the composable, reading `AvanueTheme` there.

### 2.2 `ui/GlassMorphicPanel.kt`

**Problem:** `OceanGlassColors.TextPrimary/Secondary/CoralBlue` in all `Text` color params.
Unused `MaterialTheme` import.

**Changes:**
- Remove `import com.augmentalis.overlay.theme.OceanGlassColors`
- Remove unused `import androidx.compose.material3.MaterialTheme`
- Add `import com.augmentalis.avanueui.theme.AvanueTheme`
- Replace all `OceanGlassColors.*` color refs:

```kotlin
// Before
Text(text = "AVA Assistant", color = OceanGlassColors.TextPrimary, ...)
Text(text = "You said:", color = OceanGlassColors.TextSecondary, ...)
Text(text = "AVA:", color = OceanGlassColors.CoralBlue, ...)

// After
Text(text = "AVA Assistant", color = AvanueTheme.colors.onSurface, ...)
Text(text = "You said:", color = AvanueTheme.colors.onSurface.copy(alpha = 0.7f), ...)
Text(text = "AVA:", color = AvanueTheme.colors.primary, ...)
```

- Replace `panelSolidEffect()` on the Column with the AvanueUI-backed equivalent (solid surface
  background using `AvanueTheme.colors.background`, `AvanueTheme.colors.outline` border,
  and `RoundedCornerShape(24.dp)` clip — same visual, different token source).

### 2.3 `ui/VoiceOrb.kt`

**Problem:** `OceanGlassColors.TextPrimary` and `OceanGlassColors.CoralBlue` referenced in four private
composables (`IdleMicIcon`, `ListeningWaveform`, `ProcessingSpinner`, `SpeakingGlow`).
AVID voice semantic is missing from the orb's `clickable` modifier.

**Changes:**
- Remove `import com.augmentalis.overlay.theme.OceanGlassColors`
- Add `import com.augmentalis.avanueui.theme.AvanueTheme`
- Replace `orbSolidEffect()` on the outer `Box` with the AvanueUI-backed version
- Replace color refs in each private composable:

```kotlin
// IdleMicIcon — before
tint = OceanGlassColors.TextPrimary

// IdleMicIcon — after
tint = AvanueTheme.colors.onSurface

// ListeningWaveform — before
.background(OceanGlassColors.CoralBlue)

// ListeningWaveform — after
.background(AvanueTheme.colors.primary)

// ProcessingSpinner — before
tint = OceanGlassColors.CoralBlue

// ProcessingSpinner — after
tint = AvanueTheme.colors.primary

// SpeakingGlow — before
.background(OceanGlassColors.CoralBlue.copy(alpha * 0.3f))
tint = OceanGlassColors.TextPrimary

// SpeakingGlow — after
.background(AvanueTheme.colors.primary.copy(alpha * 0.3f))
tint = AvanueTheme.colors.onSurface
```

- Add AVID semantic to the orb's outer `Box`:

```kotlin
// Add to the Box modifier chain
.semantics { contentDescription = "Voice: click voice orb" }
```

### 2.4 `ui/SuggestionChips.kt`

**Problem:** `Material3 AssistChip` with `AssistChipDefaults.assistChipColors(...)` overrides using
`OceanGlassColors.*`. The chip itself is functional but not themed through AvanueUI. AVID is missing.

**Option A (Recommended):** Keep `AssistChip` but replace color overrides with `AvanueTheme` tokens and
add AVID semantics. This is minimal change, zero visual regression risk.

**Option B:** Replace with `AvanueChip` from `com.augmentalis.avanueui.components.*`. This is the
long-term correct approach but requires verifying `AvanueChip` API matches the current usage
(label content + press animation + FlowRow layout).

**This plan recommends Option A for the migration commit. Option B can follow as a separate
improvement.** The goal of this migration is token correctness, not component system migration.

**Changes (Option A):**
- Remove `import com.augmentalis.overlay.theme.OceanGlassColors`
- Add `import com.augmentalis.avanueui.theme.AvanueTheme`
- Replace chip color overrides:

```kotlin
// Before
colors = AssistChipDefaults.assistChipColors(
    containerColor = OceanGlassColors.OceanMedium,
    labelColor = OceanGlassColors.TextPrimary,
    leadingIconContentColor = OceanGlassColors.TextSecondary
),
border = BorderStroke(1.dp, OceanGlassColors.Border),

// After
colors = AssistChipDefaults.assistChipColors(
    containerColor = AvanueTheme.colors.surfaceVariant,
    labelColor = AvanueTheme.colors.onSurface,
    leadingIconContentColor = AvanueTheme.colors.onSurface.copy(alpha = 0.7f)
),
border = BorderStroke(1.dp, AvanueTheme.colors.outline),
```

- Add AVID semantic inside `SuggestionChip` composable:

```kotlin
modifier = Modifier
    .scale(scale)
    .semantics { contentDescription = "Voice: click ${suggestion.label}" }
```

---

## Phase 3: Verification Checklist

### Build Verification

| Check | Pass Condition |
|-------|---------------|
| Module compiles after directory move | Zero compile errors |
| Module compiles after token migration | Zero compile errors |
| AvanueUI import resolves | `AvanueTheme.colors.*` accessible without errors |
| No `OceanGlassColors` references remain | `grep -r "OceanGlassColors"` returns no results |
| No `MaterialTheme.colorScheme` references remain | `grep -r "MaterialTheme.colorScheme"` returns no results |

### Manual Verification (On-Device / Emulator)

| Test | Expected Result |
|------|----------------|
| OverlayService starts | Overlay appears, orb visible on screen |
| VoiceOrb idle state | Gentle pulse animation, mic icon visible |
| VoiceOrb tap to expand | Panel slides in, GlassMorphicPanel visible |
| Transcript display | White text on dark surface |
| AVA response display | Primary-colored "AVA:" label |
| Suggestion chips render | Chips visible in FlowRow, correct colors |
| Chip tap press scale | 0.95 scale animation on press |
| VoiceOrb drag | Orb follows drag gesture, no jump |
| Panel collapse on second tap | Panel slides out, orb returns to idle |
| Dark/Light theme switch | Colors adapt correctly via AvanueTheme |

### AVID Verification

| Element | Required `contentDescription` |
|---------|-------------------------------|
| VoiceOrb outer Box | `"Voice: click voice orb"` |
| Each SuggestionChip | `"Voice: click {suggestion.label}"` |

---

## Estimated Scope

| Category | Count |
|----------|-------|
| Files moved (git mv) | 16 |
| Files with token changes | 4 (`GlassEffects`, `GlassMorphicPanel`, `VoiceOrb`, `SuggestionChips`) |
| Files with AVID additions | 2 (`VoiceOrb`, `SuggestionChips`) |
| `build.gradle.kts` changes | 1 |
| New files created | 0 |
| Files deleted | 0 |
| New voice commands | 0 |

---

## Risk Assessment

| Risk | Level | Mitigation |
|------|-------|------------|
| Directory move breaks Gradle source resolution | LOW | `sourceSets` block in build.gradle.kts explicitly maps `androidMain/kotlin` to `main` |
| `AvanueTheme` not accessible inside modifier functions | MEDIUM | Modifier functions that need theme reads must be called from `@Composable` scope — inline the modifier chain at the composable call site or accept color parameters |
| Visual regression in overlay rendering | LOW | Color mapping is 1:1 — same shade intent, different token source |
| `OceanGlassColors.CoralBlue` ≠ `AvanueTheme.colors.primary` | LOW | HYDRA default primary is #1A5CFF (royal sapphire) vs old #3B82F6 (blue). Visual difference is minor and expected — overlay now follows the active palette |
| Functional regression in service lifecycle | NONE | `OverlayService.kt` and `OverlayController.kt` are not modified |

---

## Commit Strategy

Single commit covering all three phases together. Mixing directory moves with content changes in
one commit is correct here because the move is the prerequisite for the changes and the changes are
small — splitting into two commits would leave the build broken mid-way.

**Commit message:**
```
refactor(AVA/Overlay): Migrate to AvanueUI v5.1 tokens + KMP source layout

- Move src/main/java/ -> src/androidMain/kotlin/ (KMP-ready layout)
- Replace OceanGlassColors local palette with AvanueTheme.colors.* tokens
- Update orbSolidEffect/panelSolidEffect to use AvanueTheme surface+outline
- Remove unused MaterialTheme import from GlassMorphicPanel
- Add AVID semantics to VoiceOrb and SuggestionChips
- Add AvanueUI module dependency to build.gradle.kts
- Update lifecycle + coroutines dependencies to current repo versions
```

---

## Dependencies

- `Modules/AvanueUI` — already exists in repo, new dependency for Overlay module
- No external library additions
- No database schema changes
- No new VOS commands

---

## Related Documentation

- MANDATORY RULE #3 (AvanueUI v5.1): `NewAvanues/CLAUDE.md`
- AvanueUI token reference: `Modules/AvanueUI/DesignSystem/`
- Chapter 91 (AvanueUI DesignSystem): `Docs/MasterDocs/AvanueUI/`
- Chapter 92 (Unified Components): `Docs/MasterDocs/AvanueUI/`
- Unified AVID System: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-UnifiedAVIDAndScrollReset-260215-V1.md`

---

Author: Manoj Jhawar | 2026-02-19
