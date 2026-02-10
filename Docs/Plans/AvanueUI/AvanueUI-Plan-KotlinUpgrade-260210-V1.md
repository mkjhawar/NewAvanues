# MaterialTheme Fixes + Kotlin 2.1.0 / Compose 1.7.3 Upgrade

## Context

Two tasks on `VoiceOSCore-KotlinUpdate` branch (based on `060226-1-consolidation-framework`):
1. **Fix 10 MaterialTheme.colorScheme violations** in 4 files in `apps/avanues` (quick mechanical fixes)
2. **Upgrade from Kotlin 1.9.24 → 2.1.0** and Compose Multiplatform 1.6.11 → 1.7.3 (major toolchain upgrade)

### Why Kotlin 2.1.0 + Compose 1.7.3 (not 2.0.21 + 1.7.1)
- K2 compiler alignment — Compose 1.7.3 has K2 mode in IntelliJ plugin
- kotlinx.serialization 1.8.0+ requires Kotlin 2.1.0+
- Native/web klibs in Compose 1.7.3 require Kotlin 2.1.0+
- Coroutines 1.8.1 built with Kotlin 2.1.0
- Better future-proofing (active development branch)
- ObjectBox (previous blocker) confirmed removed from project
- Gradle 8.14.3 compatible with both; AGP 8.2.0 compatible

---

## Part 1: MaterialTheme Violations (10 fixes in 4 files)

Do this FIRST — independent of the Kotlin upgrade, verifiable immediately.

### File 1: `apps/avanues/.../ui/settings/GlassesSettingsLayout.kt` — 7 violations

| Line | Current | Replacement |
|------|---------|-------------|
| 185 | `MaterialTheme.colorScheme.surfaceContainerHigh` | `AvanueTheme.colors.surfaceElevated` |
| 198 | `MaterialTheme.colorScheme.primary` | `AvanueTheme.colors.primary` |
| 224 | `MaterialTheme.colorScheme.onSurfaceVariant` | `AvanueTheme.colors.textSecondary` |
| 277 | `MaterialTheme.colorScheme.onSurfaceVariant` | `AvanueTheme.colors.textSecondary` |
| 358 | `MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)` | `AvanueTheme.colors.surface.copy(alpha = 0.7f)` |
| 372 | `MaterialTheme.colorScheme.primary` | `AvanueTheme.colors.primary` |
| 434-435 | `MaterialTheme.colorScheme.primary` / `.onSurfaceVariant` | `AvanueTheme.colors.primary` / `.textSecondary` |

Add import: `import com.augmentalis.avanueui.theme.AvanueTheme`

### File 2: `apps/avanues/.../ui/developer/DeveloperConsoleScreen.kt` — 1 violation

| Line | Current | Replacement |
|------|---------|-------------|
| 226 | `MaterialTheme.colorScheme.onSurfaceVariant` | `AvanueTheme.colors.textSecondary` |

Add import: `import com.augmentalis.avanueui.theme.AvanueTheme`

### File 3: `apps/avanues/.../ui/about/AboutScreen.kt` — 3 violations

| Line | Current | Replacement |
|------|---------|-------------|
| 343 | `MaterialTheme.colorScheme.primary` | `AvanueTheme.colors.primary` |
| 349 | `MaterialTheme.colorScheme.primary` | `AvanueTheme.colors.primary` |
| 377 | `MaterialTheme.colorScheme.tertiary` | `AvanueTheme.colors.tertiary` |

Add import: `import com.augmentalis.avanueui.theme.AvanueTheme`

### File 4: `apps/avanues/.../MainActivity.kt` — 1 violation

| Line | Current | Replacement |
|------|---------|-------------|
| 88 | `MaterialTheme.colorScheme.background` | `AvanueTheme.colors.background` |

Import likely already present.

### Color Mapping Reference
- `MaterialTheme.colorScheme.surfaceContainerHigh` → `AvanueTheme.colors.surfaceElevated` (confirmed: AvanueTheme.kt:131 maps surfaceContainerHigh = surfaceElevated)
- `.onSurfaceVariant` → `.textSecondary`
- `.primary` → `.primary` (direct)
- `.tertiary` → `.tertiary` (direct)
- `.surface` → `.surface` (direct)
- `.background` → `.background` (direct)

---

## Part 2: Kotlin 2.1.0 + Compose 1.7.3 Upgrade

### Target Versions

```toml
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
compose = "1.7.3"
# compose-compiler version removed — handled by kotlin.plugin.compose
```

### Phase 2A: Version Catalog Update (`gradle/libs.versions.toml`)

**Version changes:**
```
kotlin = "1.9.24" → "2.1.0"
ksp = "1.9.24-1.0.20" → "2.1.0-1.0.29"
compose = "1.6.11" → "1.7.3"
compose-compiler = "1.5.14" → DELETE (no longer needed)
```

**Plugin changes:**
- Uncomment line 271: `kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }`

### Phase 2B: KAPT → KSP Migration (4 modules)

Must do BEFORE the Kotlin version bump (kapt deprecated in 2.0+).

#### Module 1: `Modules/Voice/WakeWord/build.gradle.kts`
- Remove: `kotlin("kapt")` plugin
- Add: `alias(libs.plugins.ksp)` plugin
- Replace: `kapt(libs.hilt.compiler)` → `ksp(libs.hilt.compiler)`

#### Module 2: `Modules/AI/Chat/build.gradle.kts`
- Remove: `apply(plugin = "kotlin-kapt")` (line 178)
- Add: `alias(libs.plugins.ksp)` to plugins block
- Replace: `"kapt"(libs.hilt.compiler)` → `"ksp"(libs.hilt.compiler)` in android dependencies
- Replace: `"kaptAndroidTest"(...)` → `"kspAndroidTest"(...)` if present

#### Module 3: `Modules/AI/Teach/build.gradle.kts`
- Same pattern as Chat: remove kapt plugin, add KSP, replace kapt() with ksp()

#### Module 4: `android/apps/VoiceUI/build.gradle.kts` (legacy app)
- Same pattern: kapt → KSP

### Phase 2C: Compose Compiler Plugin Migration (~24 files)

Every file with `composeOptions { kotlinCompilerExtensionVersion = ... }` needs:

1. **Add** `alias(libs.plugins.kotlin.compose)` to the `plugins { }` block
2. **Remove** the entire `composeOptions { }` block from `android { }`

**Files to modify:**
- `apps/avanues/build.gradle.kts`
- `Modules/VoiceOSCore/build.gradle.kts`
- `Modules/WebAvanue/build.gradle.kts`
- `Modules/VoiceAvanue/build.gradle.kts`
- `Modules/AI/Chat/build.gradle.kts`
- `Modules/AI/RAG/build.gradle.kts`
- `Modules/AI/Teach/build.gradle.kts`
- `Modules/SpeechRecognition/build.gradle.kts`
- `Modules/DeviceManager/build.gradle.kts`
- All legacy apps in `android/apps/` (VoiceUI, etc.)
- Any other modules with `composeOptions`

**Pattern:**
```kotlin
// BEFORE
android {
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

// AFTER — block deleted entirely, plugin handles it
```

### Phase 2D: Root build.gradle.kts Update

Uncomment or add the compose compiler plugin registration:
```kotlin
plugins {
    alias(libs.plugins.kotlin.compose) apply false  // UNCOMMENT THIS
}
```

### Phase 2E: Kotlin Version Bump

After all the above infrastructure changes are in place, update `libs.versions.toml` with the new versions. This is when the actual Kotlin 2.1.0 takes effect.

---

## Pre-Implementation

0a. **Save plan doc** — `docs/plans/AvanueUI/AvanueUI-Plan-KotlinUpgrade-260210-V1.md` (naming convention)
0b. **Create branch** — `git checkout -b VoiceOSCore-KotlinUpdate` from `060226-1-consolidation-framework` HEAD (`dba31538`). Consolidation branch stays untouched as fallback.

## Execution Order

1. **Part 1** — Fix 10 MaterialTheme violations (4 files)
2. **Build verify** — `./gradlew :apps:avanues:assembleDebug`
3. **Commit** — MaterialTheme fixes
4. **Part 2A** — Update version catalog (1 file)
5. **Part 2B** — KAPT → KSP in 4 modules
6. **Part 2C** — Compose compiler plugin migration (~24 files)
7. **Part 2D** — Root build.gradle.kts update
8. **Part 2E** — Apply version bumps
9. **Build verify** — Full build: `:Modules:AvanueUI:compileDebugKotlinAndroid`, `:apps:avanues:assembleDebug`, `:Modules:AvanueUI:compileKotlinDesktop`
10. **Fix any build errors** — Iterate until clean
11. **Commit** — Kotlin/Compose upgrade

## Verification

1. `./gradlew :apps:avanues:assembleDebug` — Main app builds
2. `./gradlew :Modules:AvanueUI:compileDebugKotlinAndroid` — AvanueUI module builds
3. `./gradlew :Modules:AvanueUI:compileKotlinDesktop` — Desktop target builds
4. `./gradlew :Modules:AI:Chat:compileDebugKotlinAndroid` — Chat module (was KAPT) builds
5. `./gradlew :Modules:Voice:WakeWord:compileDebugKotlinAndroid` — WakeWord (was KAPT) builds
6. Grep for remaining `MaterialTheme.colorScheme` in `apps/avanues/` — should be zero
7. Grep for remaining `kapt` in any active build.gradle.kts — should be zero
8. Grep for remaining `composeOptions` in any active build.gradle.kts — should be zero

## Risk Mitigation

- **If a module fails to build after KAPT→KSP**: Check Hilt version supports KSP, verify `@HiltAndroidApp`/`@AndroidEntryPoint` annotations are intact
- **If Compose compiler fails**: Verify `kotlin.plugin.compose` is applied AFTER `kotlin.multiplatform` or `kotlin.android`
- **If desktop build fails**: Compose 1.7.3 may have API changes in Material3 adaptive — check deprecation warnings
- **Rollback**: `git stash` or `git checkout .` to revert all changes; version catalog is the single point of version control
