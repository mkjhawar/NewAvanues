# Cockpit Branch Strategy: MagicUI vs Compose Parallel Development

**Date:** 2025-12-09
**Status:** Planning
**Purpose:** Parallel UI implementation strategy for Cockpit MVP

---

## Branch Structure

### Primary Branches

| Branch | UI Framework | Target | Purpose |
|--------|--------------|--------|---------|
| **`Cockpit-Development`** | Jetpack Compose + Material3 | Android phones/tablets | Standard Android UI |
| **`Cockpit-MagicUI`** | Avanues MagicUI + MagicCode | All platforms (KMP) | Cross-platform spatial UI |

**Naming:** Case-insensitive (Git treats as same: `Cockpit-MagicUI` = `cockpit-magicui`)

---

## Current State Analysis

### Existing Implementation (Cockpit-Development)

**Location:** `/Volumes/M-Drive/Coding/NewAvanues/android/apps/cockpit-mvp/`

**UI Stack:**
- Jetpack Compose (Android-only)
- Material3 components
- Ocean Theme tokens (pre-MagicUI pattern)
- GlassmorphicSurface wrappers (migration-ready)

**Components:**
```
src/main/java/com/augmentalis/cockpit/mvp/
├── components/
│   └── GlassmorphicSurface.kt          (Compose wrapper)
├── theme/
│   └── ThemeProvider.kt                 (Ocean/AvaMagic/MagicCode)
├── OceanTheme.kt                        (Compose tokens)
├── WindowCard.kt                        (Compose Card)
├── TopNavigationBar.kt                  (Compose Surface)
├── ControlPanel.kt                      (Compose Surface)
├── WorkspaceView.kt                     (Compose BoxWithConstraints)
├── HeadCursorOverlay.kt                 (Compose Canvas)
└── MainActivity.kt                      (ComponentActivity)
```

**Advantages:**
- ✅ Mature Android framework
- ✅ Material3 design system
- ✅ Rich component library
- ✅ Excellent tooling (Android Studio)

**Limitations:**
- ❌ Android-only (not KMP)
- ❌ Cannot run on iOS, desktop, web without rewrite
- ❌ Tight coupling to Android APIs

---

## Cockpit-MagicUI Branch Architecture

### MagicUI Stack

**Framework:** Avanues MagicUI (KMP cross-platform)

**Platforms Supported:**
- Android (Compose)
- iOS (SwiftUI)
- Desktop (Compose Desktop)
- Web (React + Tailwind via MagicUI Web)
- visionOS (SwiftUI + RealityKit)
- AndroidXR (Compose XR)

**Core Components:**

```kotlin
// MagicUI component system (when ready)
MagicSurface { }        // Replaces GlassmorphicSurface
MagicButton { }         // Replaces OceanButton
MagicTextField { }      // Replaces OceanTextField
MagicCard { }           // Replaces WindowCard
MagicWindow { }         // Spatial window component
MagicWorkspace { }      // 3D workspace container
```

---

### File Structure Comparison

#### Cockpit-Development (Compose)
```
android/apps/cockpit-mvp/
├── src/main/java/com/augmentalis/cockpit/mvp/
│   ├── components/          (Compose wrappers)
│   ├── theme/               (Compose theme)
│   ├── rendering/           (Android Canvas)
│   └── *.kt                 (Compose UI)
├── build.gradle.kts         (Android app)
└── AndroidManifest.xml
```

#### Cockpit-MagicUI (MagicUI)
```
Common/Cockpit/
├── src/
│   ├── commonMain/kotlin/com/avanues/cockpit/
│   │   ├── ui/
│   │   │   ├── components/      (MagicUI components)
│   │   │   ├── theme/           (MagicUI theme)
│   │   │   ├── workspace/       (MagicWorkspace)
│   │   │   └── windows/         (MagicWindow)
│   │   ├── core/
│   │   │   ├── window/          (Shared domain models)
│   │   │   ├── workspace/       (Shared workspace logic)
│   │   │   └── layout/          (Shared layout presets)
│   │   └── rendering/
│   │       ├── SpatialRenderer.kt  (KMP spatial math)
│   │       └── CurvedProjection.kt (KMP projection)
│   ├── androidMain/kotlin/      (Android-specific)
│   ├── iosMain/kotlin/          (iOS-specific)
│   ├── desktopMain/kotlin/      (Desktop-specific)
│   └── jsMain/kotlin/           (Web-specific)
└── build.gradle.kts             (KMP library)
```

---

## Migration Strategy

### Phase 1: Prepare Cockpit-Development (Current State)

**Status:** ✅ Complete

**Achievements:**
- Wrapper components (GlassmorphicSurface) ready for MagicUI swap
- Ocean Theme tokens follow MagicUI patterns
- Business logic separated from UI (WorkspaceViewModel)
- Spatial rendering math in standalone classes (CurvedProjection)

**Ready for MagicUI:**
```kotlin
// TODAY (Compose):
GlassmorphicSurface(modifier) { content() }

// TOMORROW (MagicUI) - Single line change:
MagicSurface(modifier) { content() }
```

---

### Phase 2: Create Cockpit-MagicUI Branch

**Timeline:** When MagicUI framework is ready

**Steps:**

#### 2.1 Branch Creation
```bash
# From Cockpit-Development
git checkout Cockpit-Development
git pull origin Cockpit-Development
git checkout -b Cockpit-MagicUI
git push -u origin Cockpit-MagicUI
```

#### 2.2 Move to KMP Structure
```bash
# Move android/apps/cockpit-mvp → Common/Cockpit/src/
mkdir -p Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/ui
mv android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/components \
   Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/ui/
```

#### 2.3 Replace Compose with MagicUI

**File-by-file conversion:**

| Compose File | MagicUI File | Changes |
|--------------|--------------|---------|
| `GlassmorphicSurface.kt` | DELETE (use MagicSurface) | Remove wrapper, use MagicUI directly |
| `WindowCard.kt` | `MagicWindowCard.kt` | Replace Compose Card → MagicCard |
| `WorkspaceView.kt` | `MagicWorkspaceView.kt` | Replace Compose Box → MagicWorkspace |
| `OceanTheme.kt` | KEEP (MagicUI uses same tokens) | No change needed |
| `ThemeProvider.kt` | UPDATE | Add MagicUI theme provider |

**Example Conversion:**

```kotlin
// BEFORE (Compose - Cockpit-Development)
@Composable
fun WindowCard(window: AppWindow, ...) {
    GlassmorphicSurface(
        modifier = Modifier.width(OceanTheme.windowWidthDefault)
    ) {
        Column {
            Text(window.title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// AFTER (MagicUI - Cockpit-MagicUI)
@Composable  // Cross-platform composable
fun MagicWindowCard(window: AppWindow, ...) {
    MagicSurface(
        modifier = Modifier.width(OceanTheme.windowWidthDefault),
        style = MagicSurfaceStyle.Glassmorphic
    ) {
        MagicColumn {
            MagicText(window.title, style = MagicTextStyle.TitleMedium)
        }
    }
}
```

---

### Phase 3: Spatial Rendering Without HUDManager

**Goal:** Implement curved window rendering using Cockpit's own Vector3D (no HUDManager dependency)

**Architecture:**

```kotlin
// Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/rendering/

CockpitSpatialRenderer.kt
├── Uses: Vector3D (existing in Cockpit)
├── Uses: Quaternion (existing in Cockpit)
├── Uses: CurvedProjection (Phase 2 component)
├── Uses: ArcFrontLayout (Phase 2 component)
├── Uses: TheaterLayout (Phase 2 component)
└── NO HUDManager dependency

Functions:
- calculateProjectionMatrix(fov, aspect, near, far)
- worldToScreen(position: Vector3D, viewMatrix, projMatrix)
- screenToWorld(screenX, screenY, depth)
- sortByDepth(windows: List<AppWindow>)
- applyPerspectiveScale(distance: Float)
```

**Extracted from HUDManager's SpatialRenderer:**
- Projection matrix setup (~50 lines)
- World-to-screen conversion (~30 lines)
- Depth sorting logic (~20 lines)
- Perspective scaling (~10 lines)

**Total:** ~150 lines of pure Kotlin math (KMP-compatible)

---

## Code Sharing Strategy

### Shared Code (Common/Cockpit - KMP)

**Both branches use these:**

```kotlin
Common/Cockpit/src/commonMain/kotlin/
├── core/
│   ├── window/
│   │   ├── AppWindow.kt                 ✅ Shared domain model
│   │   ├── WindowType.kt                ✅ Shared enum
│   │   └── WindowState.kt               ✅ Shared state
│   └── workspace/
│       ├── Vector3D.kt                   ✅ Shared 3D math
│       ├── Quaternion.kt                 ✅ Shared rotation
│       ├── Workspace.kt                  ✅ Shared workspace model
│       └── WorkspaceManager.kt           ✅ Shared business logic
├── layout/
│   ├── LayoutPreset.kt                   ✅ Shared interface
│   └── presets/
│       ├── LinearHorizontalLayout.kt     ✅ Shared layout
│       ├── ArcFrontLayout.kt             ✅ Shared layout
│       └── TheaterLayout.kt              ✅ Shared layout
└── rendering/
    ├── CurvedProjection.kt               ✅ Shared projection math
    └── CockpitSpatialRenderer.kt         ✅ Shared spatial rendering
```

### Branch-Specific Code

#### Cockpit-Development Only (Compose/Android)
```kotlin
android/apps/cockpit-mvp/src/main/java/
├── components/
│   └── GlassmorphicSurface.kt            (Compose wrapper)
├── MainActivity.kt                        (ComponentActivity)
├── HeadCursorOverlay.kt                   (Compose Canvas)
└── AndroidManifest.xml                    (Android-specific)
```

#### Cockpit-MagicUI Only (Cross-Platform)
```kotlin
Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/ui/
├── components/
│   ├── MagicWindowCard.kt                (MagicUI component)
│   ├── MagicWorkspaceView.kt             (MagicUI workspace)
│   └── MagicControlPanel.kt              (MagicUI controls)
├── theme/
│   └── MagicUIThemeProvider.kt           (MagicUI theme)
└── MagicCockpitApp.kt                    (Entry point)

// Platform-specific entry points:
src/androidMain/kotlin/ → MainActivity.kt
src/iosMain/kotlin/ → ContentView.swift (via Kotlin/Native)
src/desktopMain/kotlin/ → main.kt (Compose Desktop)
src/jsMain/kotlin/ → main.kt (React binding)
```

---

## Build Configuration

### Cockpit-Development (build.gradle.kts)

```kotlin
// android/apps/cockpit-mvp/build.gradle.kts

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.augmentalis.cockpit.mvp"
    compileSdk = 34

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose (Android-only)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Cockpit shared code
    implementation(project(":Common:Cockpit"))
}
```

### Cockpit-MagicUI (build.gradle.kts)

```kotlin
// Common/Cockpit/build.gradle.kts

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

kotlin {
    // Platform targets
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")
    js(IR) { browser() }

    sourceSets {
        commonMain {
            dependencies {
                // MagicUI (when ready)
                implementation(project(":Common:MagicUI"))

                // Existing shared
                implementation(project(":Common:Database"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        androidMain {
            dependencies {
                implementation("androidx.compose.ui:ui:1.6.0")
                implementation("androidx.compose.material3:material3:1.2.0")
            }
        }

        iosMain {
            // SwiftUI interop
        }

        desktopMain {
            dependencies {
                implementation("androidx.compose.desktop:desktop:1.6.0")
            }
        }

        jsMain {
            // React/Tailwind via MagicUI Web
        }
    }
}
```

---

## Development Workflow

### Working on Cockpit-Development (Compose)

```bash
# Switch to branch
git checkout Cockpit-Development

# Work on Android-specific Compose UI
cd android/apps/cockpit-mvp

# Build and test
./gradlew assembleDebug installDebug

# Commit changes
git add .
git commit -m "feat(cockpit-compose): add new window card styling"
git push origin Cockpit-Development
```

### Working on Cockpit-MagicUI (Cross-Platform)

```bash
# Switch to branch
git checkout Cockpit-MagicUI

# Work on KMP shared code
cd Common/Cockpit

# Build for all platforms
./gradlew build

# Test on specific platform
./gradlew :Common:Cockpit:iosX64Test
./gradlew :Common:Cockpit:desktopTest
./gradlew :Common:Cockpit:jsTest

# Commit changes
git add .
git commit -m "feat(cockpit-magicui): add cross-platform window card"
git push origin Cockpit-MagicUI
```

---

## Merging Strategy

### Sync Shared Code Between Branches

**Scenario:** Fixed bug in Vector3D on Cockpit-Development

```bash
# On Cockpit-Development
git add Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/core/workspace/Vector3D.kt
git commit -m "fix(cockpit-core): correct Vector3D distance calculation"
git push origin Cockpit-Development

# Switch to Cockpit-MagicUI
git checkout Cockpit-MagicUI

# Cherry-pick the commit
git cherry-pick <commit-hash>
git push origin Cockpit-MagicUI
```

**Automated Sync** (optional):
```yaml
# .github/workflows/sync-cockpit-shared.yml
name: Sync Cockpit Shared Code

on:
  push:
    branches: [Cockpit-Development]
    paths:
      - 'Common/Cockpit/src/commonMain/**'

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Cherry-pick to Cockpit-MagicUI
        run: |
          git checkout Cockpit-MagicUI
          git cherry-pick ${{ github.sha }}
          git push origin Cockpit-MagicUI
```

---

## Migration Checklist

### For Each Component

- [ ] Extract business logic to `Common/Cockpit/src/commonMain/`
- [ ] Create platform-specific UI in `src/androidMain/` (Compose) or `commonMain/` (MagicUI)
- [ ] Update imports (Compose → MagicUI)
- [ ] Test on target platforms
- [ ] Update documentation

### Example: WindowCard Migration

**Step 1:** Extract domain logic
```kotlin
// Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/core/window/WindowCard.kt
class WindowCardData(
    val window: AppWindow,
    val color: String,
    val isFocused: Boolean
) {
    fun getDisplayTitle(): String = window.title
    fun getTypeLabel(): String = when (window.type) {
        WindowType.ANDROID_APP -> "Android App"
        WindowType.WEB_APP -> "Web App"
        WindowType.WIDGET -> "Widget"
        WindowType.REMOTE_DESKTOP -> "Remote Desktop"
    }
}
```

**Step 2:** Compose UI (Cockpit-Development)
```kotlin
// android/apps/cockpit-mvp/src/main/java/.../WindowCard.kt
@Composable
fun WindowCard(data: WindowCardData, onClose: () -> Unit) {
    GlassmorphicSurface { /* Compose UI */ }
}
```

**Step 3:** MagicUI (Cockpit-MagicUI)
```kotlin
// Common/Cockpit/src/commonMain/kotlin/.../ui/MagicWindowCard.kt
@Composable
fun MagicWindowCard(data: WindowCardData, onClose: () -> Unit) {
    MagicSurface { /* MagicUI components */ }
}
```

---

## Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| **Phase 1** | ✅ Complete | Cockpit-Development with Compose + wrappers |
| **Phase 2** | When MagicUI ready | Create Cockpit-MagicUI branch |
| **Phase 3** | 1-2 weeks | Migrate components to MagicUI |
| **Phase 4** | 1 week | Cross-platform testing |
| **Phase 5** | Ongoing | Maintain both branches in parallel |

---

## Conclusion

**Dual Branch Strategy:**
- **Cockpit-Development:** Mature Android/Compose UI (production-ready NOW)
- **Cockpit-MagicUI:** Future cross-platform KMP UI (when MagicUI framework ships)

**Shared Foundation:**
- Business logic (WorkspaceManager, AppWindow)
- 3D math (Vector3D, Quaternion, CurvedProjection)
- Layout algorithms (ArcFrontLayout, TheaterLayout)
- Spatial rendering (CockpitSpatialRenderer - NO HUDManager needed)

**Path Forward:**
1. Continue Cockpit-Development with Compose (current production path)
2. Prepare Cockpit-MagicUI branch structure (when MagicUI ready)
3. Migrate components incrementally
4. Maintain both branches for different deployment targets
