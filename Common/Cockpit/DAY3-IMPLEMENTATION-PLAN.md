# Cockpit Day 3 Implementation Plan - Polish + Integration

**Status**: Days 1-2 Complete ✅ | Day 3 Ready to Start

**Progress**:
- ✅ Day 1: Window Model + WebView Renderer (7 files, 2,169 lines)
- ✅ Day 2: Layout System + Voice Commands (9 files, 2,245 lines)
- 🎯 Day 3: Polish + Integration (6 tasks, ~8 hours)

---

## What's Already Done (Days 1-2)

### Day 1 Completed ✅
| Component | Status | Files Created |
|-----------|--------|---------------|
| WindowState.kt | ✅ Complete | Scroll/zoom/media persistence |
| AppWindow.kt | ✅ Complete | Window model with voice integration |
| Workspace.kt | ✅ Complete | Multi-window workspace management |
| WebViewConfig.kt | ✅ Complete | Standard/YouTube/Widget configs |
| JavaScriptInjector.kt | ✅ Complete | Voice → JS execution bridge |
| WebViewRenderer.kt | ✅ Complete | Compose WebView with state |
| SampleWorkspaces.kt | ✅ Complete | 5 demo workspaces |

### Day 2 Completed ✅
| Component | Status | Files Created |
|-----------|--------|---------------|
| Vector3D.kt | ✅ Complete | 3D math library (400 lines) |
| LayoutPreset.kt | ✅ Complete | Layout interface |
| LinearHorizontalLayout.kt | ✅ Complete | Default layout preset |
| LayoutEngine.kt | ✅ Complete | Layout application engine |
| WorkspaceManager.kt | ✅ Complete | State management with Flow |
| WorkspaceStorage.kt | ✅ Complete | In-memory storage (Android stub) |
| VoiceOSBridge.kt | ✅ Complete | Voice integration interface |
| VoiceOSBridgeImpl.kt | ✅ Complete | Android stub implementation |
| VoiceCommandHandler.kt | ✅ Complete | 20 intent handlers |

**Total**: 16 files, 4,414 lines of production code

---

## Day 3 Overview

| Metric | Value |
|--------|-------|
| **Timeline** | 8 hours (1 development day) |
| **Tasks** | 6 tasks |
| **Goal** | Integrated Compose UI + Demo Activity |
| **Deliverable** | Working Android app with voice-controlled windows |

---

## Day 3 Tasks

### Task 3.1: Create CockpitScreen Compose Entry Point (2 hours)
**Priority**: HIGH
**File**: `androidMain/ui/compose/CockpitScreen.kt`

**Purpose**: Main Compose screen integrating all components

**Implementation**:
```kotlin
package com.avanues.cockpit.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.avanues.cockpit.core.workspace.WorkspaceManager
import com.avanues.cockpit.voice.VoiceOSBridgeImpl
import com.avanues.cockpit.voice.VoiceCommandHandler

@Composable
fun CockpitScreen(
    workspaceManager: WorkspaceManager,
    voiceBridge: VoiceOSBridgeImpl,
    modifier: Modifier = Modifier
) {
    val activeWorkspace by workspaceManager.activeWorkspace.collectAsState()
    val commandHandler = remember { VoiceCommandHandler(workspaceManager, voiceBridge) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // WindowDock at bottom
            WindowDockCompose(
                workspace = activeWorkspace,
                onWindowClick = { window ->
                    // Focus window via voice handler
                    commandHandler.handleFocusWindow(window.voiceName)
                }
            )
        },
        floatingActionButton = {
            // ControlRail as floating action button
            ControlRailCompose(
                onLayoutChange = { layoutName ->
                    // Change layout via voice handler
                    commandHandler.handleSwitchLayout(layoutName)
                },
                onVoiceInput = {
                    // Trigger voice input
                    // Will be implemented in Phase 5
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main window content area
            LinearHorizontalLayoutCompose(
                workspace = activeWorkspace,
                onWindowStateChange = { windowId, newState ->
                    workspaceManager.updateWindowInActive(windowId) { window ->
                        window.withState(newState)
                    }
                }
            )
        }
    }
}
```

**Acceptance Criteria**:
- ✅ Integrates WorkspaceManager StateFlow
- ✅ Renders WindowDock at bottom
- ✅ Renders ControlRail as FAB
- ✅ Displays windows in LinearHorizontalLayout
- ✅ Window state changes propagate to manager

**Dependencies**:
- LinearHorizontalLayoutCompose (Task 3.2)
- WindowDockCompose (already exists from Day 1)
- ControlRailCompose (already exists from Day 1)

---

### Task 3.2: Create LinearHorizontalLayoutCompose (2 hours)
**Priority**: HIGH
**File**: `androidMain/ui/compose/LinearHorizontalLayoutCompose.kt`

**Purpose**: Compose layout for horizontal window array

**Implementation**:
```kotlin
package com.avanues.cockpit.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowState
import com.avanues.cockpit.core.workspace.Workspace
import com.avanues.cockpit.webview.WebViewRenderer

@Composable
fun LinearHorizontalLayoutCompose(
    workspace: Workspace,
    onWindowStateChange: (String, WindowState) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        workspace.windows.forEach { window ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Render window content based on type
                when (window.type) {
                    WindowType.WEB_APP -> {
                        WebViewRenderer(
                            url = window.sourceId,
                            state = window.state,
                            onStateChange = { newState ->
                                onWindowStateChange(window.id, newState)
                            }
                        )
                    }
                    WindowType.WIDGET -> {
                        // Widget rendering (Phase 4+)
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    else -> {
                        // Fallback for unsupported types
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
```

**Acceptance Criteria**:
- ✅ Windows arranged horizontally
- ✅ Equal weight distribution
- ✅ 16dp spacing between windows
- ✅ WebView windows render correctly
- ✅ State changes propagate upwards

**Dependencies**:
- WebViewRenderer (already exists from Day 1)
- WindowState (already exists from Day 1)

---

### Task 3.3: Add CurvedWindowPreview Modifier (Optional - 1.5 hours)
**Priority**: MEDIUM (Nice-to-have for MVP)
**File**: `androidMain/ui/compose/CurvedWindowModifier.kt`

**Purpose**: 3D "cover flow" effect from old Task_Cockpit

**Implementation**:
```kotlin
package com.avanues.cockpit.ui.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import kotlin.math.pow

/**
 * Curved preview effect (ported from ViewPagerModifications.kt)
 *
 * Applies 3D transformation based on window position:
 * - Focused window: Largest, centered, no rotation
 * - Adjacent windows: Smaller, rotated 10-15°
 * - Edge windows: Smallest, rotated 20-30°
 */
fun Modifier.curvedWindowPreview(
    position: Float,  // -2, -1, 0, 1, 2 (center = 0)
    isFocused: Boolean,
    centerItemSize: Float = 0.7f,
    decrement: Float = 2.0f
): Modifier = this.then(
    Modifier.graphicsLayer {
        val adjustedPosition = abs(position)

        // Scale based on position
        val baseScale = centerItemSize * decrement.pow(adjustedPosition) * 0.98f
        val focusBoost = if (isFocused) 1.2f else 1.0f
        val finalScale = baseScale * focusBoost

        scaleX = finalScale
        scaleY = finalScale

        // 3D rotation
        rotationY = position * -15f

        // Alpha fade
        alpha = if (isFocused) 1f else 0.7f

        // Depth (Z translation)
        translationZ = if (isFocused) 10f else 0f
    }
)
```

**Acceptance Criteria**:
- ✅ Windows scale based on position
- ✅ Focused window is largest
- ✅ 3D rotation applied (Y-axis)
- ✅ Alpha fade for background windows
- ✅ Smooth transitions

**Usage in LinearHorizontalLayoutCompose**:
```kotlin
Box(
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .curvedWindowPreview(
            position = calculatePosition(window, workspace),
            isFocused = window.id == workspace.focusedWindowId
        )
) {
    // Window content
}
```

**Note**: This is optional for MVP. Can be added later if time permits.

---

### Task 3.4: Create CockpitActivity Demo (1.5 hours)
**Priority**: HIGH
**File**: `androidMain/demo/CockpitDemoActivity.kt`

**Purpose**: Standalone activity demonstrating Cockpit functionality

**Implementation**:
```kotlin
package com.avanues.cockpit.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.avanues.cockpit.core.workspace.WorkspaceManager
import com.avanues.cockpit.core.workspace.WorkspaceStorage
import com.avanues.cockpit.layout.LayoutEngine
import com.avanues.cockpit.samples.SampleWorkspaces
import com.avanues.cockpit.ui.compose.CockpitScreen
import com.avanues.cockpit.ui.theme.CockpitTheme
import com.avanues.cockpit.voice.VoiceOSBridgeImpl
import kotlinx.coroutines.launch

/**
 * Standalone demo activity for Cockpit MVP
 */
class CockpitDemoActivity : ComponentActivity() {

    private lateinit var workspaceManager: WorkspaceManager
    private lateinit var voiceBridge: VoiceOSBridgeImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize components
        val storage = WorkspaceStorage()
        val layoutEngine = LayoutEngine.createDefault()
        workspaceManager = WorkspaceManager(layoutEngine, storage)
        voiceBridge = VoiceOSBridgeImpl()

        // Load sample workspace
        lifecycleScope.launch {
            workspaceManager.initialize()

            // If no workspaces exist, load "Minimal Browser" sample
            if (workspaceManager.workspaces.value.isEmpty()) {
                val sample = SampleWorkspaces.MINIMAL_BROWSER
                workspaceManager.saveWorkspace(sample)
                workspaceManager.loadWorkspace(sample.id)
            }
        }

        // Set Compose UI
        setContent {
            CockpitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CockpitDemoScreen()
                }
            }
        }
    }

    @Composable
    private fun CockpitDemoScreen() {
        CockpitScreen(
            workspaceManager = workspaceManager,
            voiceBridge = voiceBridge,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

**Acceptance Criteria**:
- ✅ Activity launches successfully
- ✅ Loads "Minimal Browser" sample workspace
- ✅ Displays 3 windows (Google, GitHub, Calculator)
- ✅ WorkspaceManager initialized
- ✅ VoiceOSBridge connected

**AndroidManifest.xml Entry**:
```xml
<activity
    android:name=".demo.CockpitDemoActivity"
    android:exported="true"
    android:theme="@style/Theme.Cockpit">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

---

### Task 3.5: Add Sample Data Initialization (1 hour)
**Priority**: HIGH
**File**: `commonMain/samples/SampleDataLoader.kt`

**Purpose**: Utility to initialize sample workspaces

**Implementation**:
```kotlin
package com.avanues.cockpit.samples

import com.avanues.cockpit.core.workspace.Workspace
import com.avanues.cockpit.core.workspace.WorkspaceManager

/**
 * Sample data loader for MVP testing
 */
object SampleDataLoader {

    /**
     * Loads all sample workspaces into WorkspaceManager
     */
    suspend fun loadAll(manager: WorkspaceManager) {
        SampleWorkspaces.ALL.forEach { workspace ->
            manager.saveWorkspace(workspace)
        }
    }

    /**
     * Loads a specific sample workspace and activates it
     */
    suspend fun loadAndActivate(
        manager: WorkspaceManager,
        voiceName: String
    ): Workspace? {
        val workspace = SampleWorkspaces.getByVoiceName(voiceName)
        if (workspace != null) {
            manager.saveWorkspace(workspace)
            manager.loadWorkspace(workspace.id)
        }
        return workspace
    }

    /**
     * Gets default demo workspace for first launch
     */
    fun getDefaultDemo(): Workspace {
        return SampleWorkspaces.MINIMAL_BROWSER
    }

    /**
     * Creates a simple test workspace (for quick testing)
     */
    fun createQuickTest(): Workspace {
        return Workspace(
            id = "quick_test",
            name = "Quick Test",
            voiceName = "test",
            layoutPresetId = "LINEAR_HORIZONTAL",
            windows = listOf(
                AppWindow.webApp(
                    id = "test1",
                    title = "Google",
                    url = "https://www.google.com",
                    voiceName = "google"
                ),
                AppWindow.webApp(
                    id = "test2",
                    title = "GitHub",
                    url = "https://github.com",
                    voiceName = "github"
                )
            )
        )
    }
}
```

**Acceptance Criteria**:
- ✅ Can load all sample workspaces
- ✅ Can load and activate by voice name
- ✅ Provides default demo workspace
- ✅ Creates quick test workspace

**Usage in CockpitDemoActivity**:
```kotlin
lifecycleScope.launch {
    workspaceManager.initialize()

    // Load default demo
    val demo = SampleDataLoader.getDefaultDemo()
    workspaceManager.saveWorkspace(demo)
    workspaceManager.loadWorkspace(demo.id)
}
```

---

### Task 3.6: Add build.gradle.kts Configuration (0.5 hours)
**Priority**: HIGH
**File**: `Common/Cockpit/build.gradle.kts` (update existing)

**Purpose**: Ensure all dependencies are configured

**Implementation**:
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose") version "1.5.11"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Cockpit"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                // Compose
                implementation(platform("androidx.compose:compose-bom:2024.02.00"))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.compose.foundation:foundation")
                implementation("androidx.compose.runtime:runtime")

                // Activity
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

                // WebView
                implementation("androidx.webkit:webkit:1.8.0")

                // VoiceOS Logger (internal)
                // implementation(project(":Common:VoiceOSLogger"))
            }
        }

        val iosMain by getting {
            dependencies {
                // iOS-specific dependencies
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.avanues.cockpit"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

**Acceptance Criteria**:
- ✅ All Compose dependencies included
- ✅ kotlinx-serialization configured
- ✅ Activity Compose dependencies added
- ✅ WebView dependency included
- ✅ Builds successfully

---

## Day 3 Deliverable

**Working Android Demo App** with:
- ✅ CockpitScreen integrating all components
- ✅ LinearHorizontalLayout rendering 2-3 windows
- ✅ WindowDock at bottom showing active windows
- ✅ ControlRail as floating action button
- ✅ WorkspaceManager managing state
- ✅ Sample workspace loaded automatically
- ✅ Window state persistence (scroll/zoom)
- ✅ Smooth 60fps animations

---

## Testing Checklist

Before marking Day 3 complete:

| Test | Expected Result | Status |
|------|-----------------|--------|
| Launch CockpitDemoActivity | Activity opens with 3 windows | ⏳ |
| Windows render horizontally | Google, GitHub, Calculator visible | ⏳ |
| Click window in dock | Window focuses | ⏳ |
| Scroll in WebView | Scroll persists when switching windows | ⏳ |
| Zoom in WebView | Zoom persists when switching windows | ⏳ |
| Control Rail visible | Floating button shows at corner | ⏳ |
| WindowDock visible | Dots show at bottom, active highlighted | ⏳ |

---

## Post-MVP Enhancements (Phase 4+)

After Day 3 MVP is complete:

**Phase 4** (Week 2):
- Complete 4 remaining layout presets (Arc, Grid, Stack, Theater)
- Advanced WebView integration (auth, forms, file upload)
- Media renderers (Image, Video, PDF)

**Phase 5** (Week 3):
- VoiceOS deep integration (real TTS, Speech Recognition)
- Accessibility service connection
- NLU for natural language commands

**Phase 6** (Week 4):
- Spatial audio with HRTF
- AR mode with ARCore
- Eye tracking integration

**Phase 7-12** (Weeks 5-12):
- iOS support
- Desktop support
- Performance optimization
- Production polish

---

## Risk Mitigation

| Risk | Mitigation | Status |
|------|-----------|--------|
| WebView rendering issues | Use AndroidView wrapper, test on real device | ⏳ |
| State persistence failures | In-memory fallback, graceful degradation | ✅ Done |
| Compose performance | Profile with Layout Inspector, optimize recomposition | ⏳ |
| Voice bridge stubs not working | Fallback to manual testing, Phase 5 will complete | ✅ Expected |

---

## Day 3 Time Estimates

| Task | Estimated | Actual | Status |
|------|-----------|--------|--------|
| 3.1: CockpitScreen entry point | 2 hours | - | Pending |
| 3.2: LinearHorizontalLayoutCompose | 2 hours | - | Pending |
| 3.3: CurvedWindowModifier (optional) | 1.5 hours | - | Optional |
| 3.4: CockpitDemoActivity | 1.5 hours | - | Pending |
| 3.5: SampleDataLoader | 1 hour | - | Pending |
| 3.6: build.gradle.kts updates | 0.5 hours | - | Pending |
| **Total** | **8.5 hours** | - | 0% |

---

## Success Criteria

Day 3 is complete when:
- ✅ CockpitDemoActivity launches successfully
- ✅ 3 windows render in horizontal layout
- ✅ WindowDock shows active window indicator
- ✅ ControlRail visible as FAB
- ✅ Window switching works (click dock)
- ✅ WebView scroll/zoom persistence works
- ✅ Smooth 60fps animations
- ✅ No crashes during basic interaction
- ✅ Demo video recorded (optional but recommended)

---

**Ready to implement Day 3!** 🚀
