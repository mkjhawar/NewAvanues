# VoiceOSCoreNG KMP Migration Plan - Phases 10-15

**Date:** 2026-01-06
**Version:** V1
**Author:** VOS4 Development Team
**Methodology:** TDD (Test-Driven Development)
**Modifiers:** .swarm .yolo .cot .rot .tot .tdd

---

## Executive Summary

This plan completes the remaining ~50% of VoiceOSCore functionality migration to VoiceOSCoreNG KMP. Total effort: **~17,000 lines** across 6 phases, organized by **code proximity** to maximize context retention.

### Database Status (Verified 2026-01-06)

| Component | Status | Notes |
|-----------|--------|-------|
| UUID Tables | ❌ REMOVED | Legacy tables deleted |
| VUID Tables | ✅ ACTIVE | VUIDAlias, VUIDAnalytics, VUIDElement, VUIDHierarchy |
| IVuidRepository | ✅ IN PLACE | commonMain interface |
| SQLDelightVuidRepositoryAdapter | ✅ IN PLACE | Bridges to VoiceOS/core/database |
| ICommandRepository | ✅ IN PLACE | For generated commands |
| SQLDelightCommandRepositoryAdapter | ✅ IN PLACE | Bridges to VoiceOS/core/database |
| RepositoryProvider | ✅ IN PLACE | Configures SQLDelight at runtime |

**All phases must use VUID tables via existing adapters. No UUID references allowed.**

### Phase Overview

| Phase | Focus | Lines | TDD Tests | Priority | Duration |
|-------|-------|-------|-----------|----------|----------|
| 10 | Overlay System | ~6,810 | ~80 tests | P0 | 2 weeks |
| 11 | Cursor/Focus System | ~4,924 | ~60 tests | P0 | 1.5 weeks |
| 12 | Additional Handlers | ~3,359 | ~50 tests | P1 | 1.5 weeks |
| 13 | Speech Engine Ports | ~900 | ~30 tests | P1 | 1 week |
| 14 | LearnApp Complete | ~2,500 | ~40 tests | P2 | 2 weeks |
| 15 | iOS/Desktop Executors | ~1,500 | ~30 tests | P2 | 1.5 weeks |

**Total:** ~17,000 lines, ~290 tests, ~9.5 weeks

---

## TDD Methodology

### Red-Green-Refactor Cycle

```
1. RED: Write failing test first
2. GREEN: Write minimal code to pass
3. REFACTOR: Clean up while tests pass
```

### Test Structure per Component

```
commonTest/
├── {Component}Test.kt      # Unit tests (pure logic)
├── {Component}IntegrationTest.kt  # Integration tests
└── {Component}ContractTest.kt     # Interface contract tests

androidTest/
├── {Component}AndroidTest.kt      # Android-specific tests
└── {Component}InstrumentedTest.kt # Device tests
```

### Coverage Requirements

| Type | Minimum | Target |
|------|---------|--------|
| Unit Tests | 80% | 90% |
| Integration | 60% | 75% |
| Edge Cases | 100% | 100% |

---

## Phase 10: Overlay System (P0 - CRITICAL)

### Reasoning (Chain of Thought)

**Why Overlay First?**
1. Overlays provide visual feedback - critical for UX
2. NumberOverlay enables "tap 3" commands (core feature)
3. CommandStatusOverlay shows listening/processing state
4. Other handlers depend on overlay visualization

### Architecture Decision (Tree of Thought)

```
Option A: Port Compose UI directly (Android-only)
  ├── Pro: Fastest implementation
  ├── Pro: Reuse existing Compose code
  └── Con: No iOS/Desktop support

Option B: Compose Multiplatform (Recommended) ✓
  ├── Pro: Single UI codebase
  ├── Pro: iOS support via Compose for iOS
  ├── Con: Experimental on iOS
  └── Con: Desktop needs JVM Compose

Option C: Platform-specific UI
  ├── Pro: Native look and feel
  ├── Con: 3x code maintenance
  └── Con: Inconsistent behavior

Decision: Option B - Compose Multiplatform with platform-specific WindowManager
```

### Files to Create (TDD Order)

#### 10.1 Core Infrastructure (Week 1, Days 1-3)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 1 | `OverlayConfigTest.kt` | `OverlayConfig.kt` | 356 | 15 |
| 2 | `OverlayThemeTest.kt` | `OverlayTheme.kt` | 292 | 12 |
| 3 | `OverlayThemesTest.kt` | `OverlayThemes.kt` | 294 | 8 |
| 4 | `NumberOverlayStyleTest.kt` | `NumberOverlayStyle.kt` | 159 | 10 |

**Test-First Examples:**

```kotlin
// commonTest/overlay/OverlayConfigTest.kt
class OverlayConfigTest {
    @Test
    fun `default config has correct theme`() {
        val config = OverlayConfig.default()
        assertEquals("Material3Dark", config.themeName)
    }

    @Test
    fun `large text increases font sizes by 25%`() {
        val config = OverlayConfig.default().withLargeText(true)
        assertEquals(22.5f, config.effectiveTheme.titleFontSize) // 18 * 1.25
    }

    @Test
    fun `high contrast uses darker colors`() {
        val config = OverlayConfig.default().withHighContrast(true)
        assertTrue(config.effectiveTheme.backgroundColor.luminance() < 0.1f)
    }
}
```

#### 10.2 Base Overlay Components (Week 1, Days 4-5)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 5 | `IOverlayTest.kt` | `IOverlay.kt` | 50 | 5 |
| 6 | `BaseOverlayTest.kt` | `BaseOverlay.kt` | 228 | 12 |
| 7 | `OverlayManagerTest.kt` | `OverlayManager.kt` | 370 | 18 |

**Interface Contract Test:**

```kotlin
// commonTest/overlay/IOverlayContractTest.kt
abstract class IOverlayContractTest<T : IOverlay> {
    abstract fun createOverlay(): T

    @Test
    fun `show makes overlay visible`() {
        val overlay = createOverlay()
        overlay.show()
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `hide makes overlay invisible`() {
        val overlay = createOverlay()
        overlay.show()
        overlay.hide()
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `dispose cleans up resources`() {
        val overlay = createOverlay()
        overlay.show()
        overlay.dispose()
        assertFalse(overlay.isVisible)
    }
}
```

#### 10.3 Visual Overlays (Week 2, Days 1-3)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 8 | `CommandStatusOverlayTest.kt` | `CommandStatusOverlay.kt` | 440 | 15 |
| 9 | `ConfidenceOverlayTest.kt` | `ConfidenceOverlay.kt` | 297 | 10 |
| 10 | `ContextMenuOverlayTest.kt` | `ContextMenuOverlay.kt` | 441 | 12 |
| 11 | `NumberedSelectionOverlayTest.kt` | `NumberedSelectionOverlay.kt` | 435 | 15 |

**TDD for CommandStatusOverlay:**

```kotlin
// commonTest/overlay/CommandStatusOverlayTest.kt
class CommandStatusOverlayTest {
    @Test
    fun `showStatus with LISTENING state shows microphone icon`() {
        val overlay = CommandStatusOverlay(mockContext)
        overlay.showStatus("", CommandState.LISTENING, "Listening...")
        assertEquals(CommandState.LISTENING, overlay.currentState)
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `state transitions emit to StateFlow`() = runTest {
        val overlay = CommandStatusOverlay(mockContext)
        val states = mutableListOf<CommandState>()

        overlay.stateFlow.take(3).toList(states)

        overlay.showStatus("scroll", CommandState.PROCESSING, "")
        overlay.updateStatus("scroll", CommandState.EXECUTING, "")
        overlay.updateStatus("scroll", CommandState.SUCCESS, "Done")

        assertEquals(listOf(PROCESSING, EXECUTING, SUCCESS), states)
    }
}
```

#### 10.4 Renderer & Coordinator (Week 2, Days 4-5)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 12 | `NumberOverlayRendererTest.kt` | `NumberOverlayRenderer.kt` | 317 | 10 |
| 13 | `OverlayCoordinatorTest.kt` | `OverlayCoordinator.kt` | 248 | 8 |

#### 10.5 Platform-Specific (Android) (Week 2, Day 5)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 14 | `AndroidOverlayManagerTest.kt` | `AndroidOverlayManager.kt` | 200 | 8 |

**Android WindowManager Integration Test:**

```kotlin
// androidTest/overlay/AndroidOverlayManagerTest.kt
@RunWith(RobolectricTestRunner::class)
class AndroidOverlayManagerTest {
    @Test
    fun `overlay uses TYPE_ACCESSIBILITY_OVERLAY`() {
        val manager = AndroidOverlayManager(context)
        val params = manager.createLayoutParams(OverlayType.FULLSCREEN)
        assertEquals(
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            params.type
        )
    }
}
```

### Phase 10 Deliverables

```
Modules/VoiceOSCoreNG/
├── src/commonMain/kotlin/.../overlay/
│   ├── IOverlay.kt
│   ├── OverlayConfig.kt
│   ├── OverlayTheme.kt
│   ├── OverlayThemes.kt
│   ├── BaseOverlay.kt
│   ├── OverlayManager.kt
│   ├── OverlayCoordinator.kt
│   ├── CommandStatusOverlay.kt
│   ├── ConfidenceOverlay.kt
│   ├── ContextMenuOverlay.kt
│   ├── NumberedSelectionOverlay.kt
│   ├── NumberOverlayRenderer.kt
│   └── NumberOverlayStyle.kt
├── src/androidMain/kotlin/.../overlay/
│   └── AndroidOverlayManager.kt
├── src/iosMain/kotlin/.../overlay/
│   └── IOSOverlayManager.kt (stub)
├── src/desktopMain/kotlin/.../overlay/
│   └── DesktopOverlayManager.kt (stub)
└── src/commonTest/kotlin/.../overlay/
    ├── OverlayConfigTest.kt
    ├── OverlayThemeTest.kt
    ├── CommandStatusOverlayTest.kt
    ├── ConfidenceOverlayTest.kt
    ├── ContextMenuOverlayTest.kt
    ├── NumberedSelectionOverlayTest.kt
    └── OverlayCoordinatorTest.kt
```

---

## Phase 11: Cursor/Focus System (P0 - CRITICAL)

### Reasoning (Chain of Thought)

**Why Cursor/Focus Next?**
1. Spatial cursor enables precise element targeting
2. Focus indicator shows current selection visually
3. Required for "tap at position" and cursor commands
4. Works with overlay system for visual feedback

### Architecture Decision (Tree of Thought)

```
Option A: Direct port (keep all 11 components)
  ├── Pro: Feature parity
  ├── Con: Complex, tightly coupled
  └── Con: 4,924 lines to maintain

Option B: Simplified architecture (Recommended) ✓
  ├── Core: CursorManager (facade)
  ├── Sub: PositionTracker, StyleManager, GestureHandler
  ├── Pro: Cleaner API
  └── Pro: Easier testing

Option C: Merge into handler system
  ├── Pro: Fewer components
  └── Con: Loses cursor-specific features

Decision: Option B - Simplified with CursorManager facade
```

### Files to Create (TDD Order)

#### 11.1 Position & Bounds (Week 3, Days 1-2)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 1 | `CursorPositionTest.kt` | `CursorPosition.kt` | 80 | 8 |
| 2 | `CursorPositionTrackerTest.kt` | `CursorPositionTracker.kt` | 359 | 15 |
| 3 | `BoundaryDetectorTest.kt` | `BoundaryDetector.kt` | 414 | 12 |

**TDD for Position Tracker:**

```kotlin
// commonTest/cursor/CursorPositionTrackerTest.kt
class CursorPositionTrackerTest {
    @Test
    fun `moveBy updates position correctly`() {
        val tracker = CursorPositionTracker(screenBounds = ScreenBounds(1080, 1920))
        tracker.updatePosition(100f, 100f)
        tracker.moveBy(50f, -30f)

        assertEquals(150f, tracker.getCurrentPosition().x)
        assertEquals(70f, tracker.getCurrentPosition().y)
    }

    @Test
    fun `position is clamped to screen bounds`() {
        val tracker = CursorPositionTracker(screenBounds = ScreenBounds(1080, 1920))
        tracker.updatePosition(2000f, 3000f)

        assertEquals(1080f, tracker.getCurrentPosition().x)
        assertEquals(1920f, tracker.getCurrentPosition().y)
    }

    @Test
    fun `centerCursor places cursor at screen center`() {
        val tracker = CursorPositionTracker(screenBounds = ScreenBounds(1080, 1920))
        tracker.centerCursor()

        assertEquals(540f, tracker.getCurrentPosition().x)
        assertEquals(960f, tracker.getCurrentPosition().y)
    }
}
```

#### 11.2 Style & Visibility (Week 3, Days 2-3)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 4 | `CursorStyleManagerTest.kt` | `CursorStyleManager.kt` | 419 | 12 |
| 5 | `CursorVisibilityManagerTest.kt` | `CursorVisibilityManager.kt` | 430 | 10 |
| 6 | `SpeedControllerTest.kt` | `SpeedController.kt` | 320 | 10 |

#### 11.3 Gestures & Actions (Week 3, Days 3-4)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 7 | `CursorGestureHandlerTest.kt` | `CursorGestureHandler.kt` | 573 | 15 |
| 8 | `CommandMapperTest.kt` | `CommandMapper.kt` | 576 | 12 |

**TDD for Gesture Handler:**

```kotlin
// commonTest/cursor/CursorGestureHandlerTest.kt
class CursorGestureHandlerTest {
    @Test
    fun `performClick returns success result`() = runTest {
        val handler = CursorGestureHandler(mockExecutor)
        val result = handler.performClick()

        assertTrue(result.success)
        assertEquals(GestureType.CLICK, result.type)
    }

    @Test
    fun `performSwipe calculates correct path`() = runTest {
        val handler = CursorGestureHandler(mockExecutor)
        handler.setPosition(500f, 500f)

        val result = handler.performSwipe(Direction.UP, distance = 300f)

        assertTrue(result.success)
        verify { mockExecutor.dispatchSwipe(500f, 500f, 500f, 200f, any()) }
    }
}
```

#### 11.4 History & Snapping (Week 3, Days 4-5)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 9 | `CursorHistoryTrackerTest.kt` | `CursorHistoryTracker.kt` | 474 | 10 |
| 10 | `SnapToElementHandlerTest.kt` | `SnapToElementHandler.kt` | 508 | 12 |

#### 11.5 Integration (Week 3, Day 5)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 11 | `FocusIndicatorTest.kt` | `FocusIndicator.kt` | 397 | 8 |
| 12 | `CursorManagerTest.kt` | `CursorManager.kt` | 300 | 10 |

### Phase 11 Deliverables

```
Modules/VoiceOSCoreNG/
├── src/commonMain/kotlin/.../cursor/
│   ├── CursorPosition.kt
│   ├── CursorPositionTracker.kt
│   ├── CursorStyleManager.kt
│   ├── CursorVisibilityManager.kt
│   ├── CursorGestureHandler.kt
│   ├── CursorHistoryTracker.kt
│   ├── CommandMapper.kt
│   ├── SpeedController.kt
│   ├── BoundaryDetector.kt
│   ├── SnapToElementHandler.kt
│   ├── FocusIndicator.kt
│   └── CursorManager.kt (facade)
├── src/androidMain/kotlin/.../cursor/
│   ├── AndroidGestureDispatcher.kt
│   └── AndroidSnapProvider.kt
└── src/commonTest/kotlin/.../cursor/
    └── [12 test files]
```

---

## Phase 12: Additional Handlers (P1)

### Reasoning (Chain of Thought)

**Priority Order by Usage Frequency:**
1. **SelectHandler** - Core selection/clipboard (HIGHEST)
2. **GestureHandler** - Multi-touch gestures (HIGH)
3. **NumberHandler** - "tap 3" commands (HIGH)
4. **DragHandler** - Drag operations (MEDIUM)
5. **DeviceHandler** - Volume/brightness (MEDIUM)
6. **BluetoothHandler** - Connectivity (LOW)
7. **HelpMenuHandler** - Documentation (LOW)
8. **AppHandler** - App launching (LOW)

### Files to Create (TDD Order)

#### 12.1 Selection & Clipboard (Week 4, Days 1-2)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 1 | `SelectHandlerTest.kt` | `SelectHandler.kt` | 500 | 15 |
| 2 | `AndroidSelectExecutorTest.kt` | `AndroidSelectExecutor.kt` | 336 | 10 |

**TDD for SelectHandler:**

```kotlin
// commonTest/handlers/SelectHandlerTest.kt
class SelectHandlerTest {
    @Test
    fun `canHandle returns true for selection commands`() {
        val handler = SelectHandler(mockExecutor)

        assertTrue(handler.canHandle("select"))
        assertTrue(handler.canHandle("copy"))
        assertTrue(handler.canHandle("paste"))
        assertTrue(handler.canHandle("select all"))
        assertFalse(handler.canHandle("scroll down"))
    }

    @Test
    fun `execute copy returns clipboard content`() = runTest {
        val handler = SelectHandler(mockExecutor)
        every { mockExecutor.copySelection() } returns "copied text"

        val result = handler.execute(QuantizedCommand("copy", 1.0f))

        assertTrue(result is HandlerResult.Success)
        assertEquals("copied text", (result as HandlerResult.Success).data["content"])
    }

    @Test
    fun `execute paste requires clipboard content`() = runTest {
        val handler = SelectHandler(mockExecutor)
        every { mockExecutor.hasClipboardContent() } returns false

        val result = handler.execute(QuantizedCommand("paste", 1.0f))

        assertTrue(result is HandlerResult.Failure)
        assertEquals("Clipboard is empty", (result as HandlerResult.Failure).message)
    }
}
```

#### 12.2 Gesture & Number (Week 4, Days 2-3)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 3 | `GestureHandlerTest.kt` | `GestureHandler.kt` | 350 | 12 |
| 4 | `NumberHandlerTest.kt` | `NumberHandler.kt` | 400 | 12 |

#### 12.3 Drag & Device (Week 4, Days 4-5)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 5 | `DragHandlerTest.kt` | `DragHandler.kt` | 300 | 10 |
| 6 | `DeviceHandlerTest.kt` | `DeviceHandler.kt` | 200 | 8 |
| 7 | `BluetoothHandlerTest.kt` | `BluetoothHandler.kt` | 180 | 6 |
| 8 | `HelpMenuHandlerTest.kt` | `HelpMenuHandler.kt` | 400 | 8 |
| 9 | `AppHandlerTest.kt` | `AppHandler.kt` | 70 | 4 |

### Phase 12 Deliverables

```
Modules/VoiceOSCoreNG/
├── src/commonMain/kotlin/.../handlers/
│   ├── SelectHandler.kt
│   ├── GestureHandler.kt
│   ├── NumberHandler.kt
│   ├── DragHandler.kt
│   ├── DeviceHandler.kt
│   ├── BluetoothHandler.kt
│   ├── HelpMenuHandler.kt
│   └── AppHandler.kt
├── src/androidMain/kotlin/.../handlers/
│   ├── AndroidSelectExecutor.kt
│   ├── AndroidGestureExecutor.kt
│   ├── AndroidDeviceExecutor.kt
│   └── AndroidBluetoothExecutor.kt
└── src/commonTest/kotlin/.../handlers/
    └── [9 test files]
```

---

## Phase 13: Speech Engine Ports (P1)

### Reasoning (Chain of Thought)

**Why Speech Engines Now?**
1. Vosk enables offline recognition (critical for privacy)
2. Azure provides high accuracy for complex commands
3. Both already have VoiceOSCore implementations to port

### Files to Create (TDD Order)

#### 13.1 Vosk Offline Engine (Week 5, Days 1-2)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 1 | `VoskEngineImplTest.kt` | `VoskEngineImpl.kt` | 423 | 15 |

**TDD for Vosk:**

```kotlin
// androidTest/speech/VoskEngineImplTest.kt
class VoskEngineImplTest {
    @Test
    fun `initialize loads model from config path`() = runTest {
        val engine = VoskEngineImpl(context)
        val config = SpeechConfig(modelPath = "assets/vosk-model-small-en")

        val result = engine.initialize(config)

        assertTrue(result.isSuccess)
        assertEquals(EngineState.READY, engine.state.value)
    }

    @Test
    fun `startListening emits partial results`() = runTest {
        val engine = VoskEngineImpl(context)
        engine.initialize(SpeechConfig(modelPath = "assets/vosk-model"))

        val results = mutableListOf<SpeechResult>()
        engine.results.take(2).toList(results)

        engine.startListening()
        // Simulate audio input...

        assertTrue(results[0].isPartial)
        assertFalse(results[1].isPartial)
    }

    @Test
    fun `grammar constrains recognition to command list`() = runTest {
        val engine = VoskEngineImpl(context)
        val config = SpeechConfig(
            modelPath = "assets/vosk-model",
            grammar = listOf("scroll up", "scroll down", "click")
        )
        engine.initialize(config)

        // Vosk should only recognize these commands
        verify { voskRecognizer.setGrammar("""["scroll up", "scroll down", "click"]""") }
    }
}
```

#### 13.2 Azure Cloud Engine (Week 5, Days 3-4)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 2 | `AzureEngineImplTest.kt` | `AzureEngineImpl.kt` | 477 | 15 |

**TDD for Azure:**

```kotlin
// androidTest/speech/AzureEngineImplTest.kt
class AzureEngineImplTest {
    @Test
    fun `initialize requires subscription key`() = runTest {
        val engine = AzureEngineImpl()
        val config = SpeechConfig(
            // No Azure credentials
        )

        val result = engine.initialize(config)

        assertTrue(result.isFailure)
        assertEquals("Azure subscription key required", result.exceptionOrNull()?.message)
    }

    @Test
    fun `initialize with environment variables succeeds`() = runTest {
        System.setProperty("AZURE_SPEECH_KEY", "test-key")
        System.setProperty("AZURE_SPEECH_REGION", "eastus")

        val engine = AzureEngineImpl()
        val result = engine.initialize(SpeechConfig())

        assertTrue(result.isSuccess)
    }

    @Test
    fun `phrase list improves recognition accuracy`() = runTest {
        val engine = AzureEngineImpl()
        val config = SpeechConfig(
            phraseList = listOf("VoiceOS", "scroll down", "click button")
        )
        engine.initialize(config)

        verify { azurePhraseListGrammar.addPhrase("VoiceOS") }
        verify { azurePhraseListGrammar.addPhrase("scroll down") }
    }
}
```

### Phase 13 Deliverables

```
Modules/VoiceOSCoreNG/
├── src/androidMain/kotlin/.../speech/
│   ├── VoskEngineImpl.kt
│   └── AzureEngineImpl.kt
├── src/androidTest/kotlin/.../speech/
│   ├── VoskEngineImplTest.kt
│   └── AzureEngineImplTest.kt
└── build.gradle.kts (updated with dependencies)
```

**Dependencies to Add:**

```kotlin
// androidMain dependencies
implementation("com.alphacephei:vosk-android:0.3.47")
implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.38.0")
```

---

## Phase 14: LearnApp Complete (P2)

### Reasoning (Chain of Thought)

**What's Missing in LearnApp?**
1. **ExplorationEngine** - Screen exploration orchestration
2. **JIT Learning** - Just-in-time command learning
3. **Consent Management** - User consent dialogs
4. **Session Management** - Exploration session state

### Database Integration (VUID System)

**CRITICAL:** All LearnApp components MUST use the existing VUID infrastructure:

```
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../repository/
├── IVuidRepository.kt          # Interface for VUID operations
├── ICommandRepository.kt       # Interface for command operations
├── SQLDelightVuidRepositoryAdapter.kt    # Bridges to VoiceOS/core/database
├── SQLDelightCommandRepositoryAdapter.kt # Bridges to VoiceOS/core/database
└── RepositoryProvider.kt       # Runtime configuration

Database Tables Used (VoiceOS/core/database):
├── vuid_elements     # Scraped UI elements with VUID
├── vuid_aliases      # Voice aliases for VUIDs
├── vuid_analytics    # Usage analytics per VUID
└── vuid_hierarchy    # Parent-child VUID relationships
```

**Usage Pattern:**

```kotlin
// In ExplorationEngine
class ExplorationEngine(
    private val vuidRepository: IVuidRepository,
    private val commandRepository: ICommandRepository
) {
    suspend fun saveLearnedElement(element: QuantizedElement) {
        // Convert to VUID entry and persist
        val vuidEntry = element.toVuidEntry()
        vuidRepository.insert(vuidEntry)
    }
}

// Configured via RepositoryProvider
val engine = ExplorationEngine(
    vuidRepository = RepositoryProvider.getVuidRepository(),
    commandRepository = RepositoryProvider.getCommandRepository()
)
```

### Files to Create (TDD Order)

#### 14.1 Exploration Engine (Week 6, Days 1-3)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 1 | `ExplorationSessionTest.kt` | `ExplorationSession.kt` | 200 | 10 |
| 2 | `ExplorationEngineTest.kt` | `ExplorationEngine.kt` | 500 | 15 |
| 3 | `ExplorationStateTest.kt` | `ExplorationState.kt` | 150 | 8 |

#### 14.2 JIT Learning (Week 6, Days 3-4)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 4 | `JITLearnerTest.kt` | `JITLearner.kt` | 400 | 12 |
| 5 | `CommandLearnerTest.kt` | `CommandLearner.kt` | 300 | 10 |

#### 14.3 Consent & Session (Week 6, Day 5 + Week 7, Days 1-2)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 6 | `ConsentManagerTest.kt` | `ConsentManager.kt` | 250 | 8 |
| 7 | `SessionManagerTest.kt` | `SessionManager.kt` | 200 | 6 |

### Phase 14 Deliverables

```
Modules/VoiceOSCoreNG/
├── src/commonMain/kotlin/.../learnapp/
│   ├── ExplorationEngine.kt
│   ├── ExplorationSession.kt
│   ├── ExplorationState.kt
│   ├── JITLearner.kt
│   ├── CommandLearner.kt
│   ├── ConsentManager.kt
│   └── SessionManager.kt
└── src/commonTest/kotlin/.../learnapp/
    └── [7 test files]
```

---

## Phase 15: iOS/Desktop Executors (P2)

### Reasoning (Chain of Thought)

**Why Platform Executors Last?**
1. Core logic is in commonMain (already tested)
2. Executors are thin platform adapters
3. Can be stubbed initially, implemented later
4. iOS requires UIAccessibility, Desktop requires AWT/Robot

### Files to Create (TDD Order)

#### 15.1 iOS Executors (Week 7, Days 3-4)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 1 | `IOSNavigationExecutorTest.kt` | `IOSNavigationExecutor.kt` | 150 | 8 |
| 2 | `IOSUIExecutorTest.kt` | `IOSUIExecutor.kt` | 150 | 8 |
| 3 | `IOSInputExecutorTest.kt` | `IOSInputExecutor.kt` | 100 | 6 |
| 4 | `IOSSystemExecutorTest.kt` | `IOSSystemExecutor.kt` | 100 | 5 |

#### 15.2 Desktop Executors (Week 7, Day 5 + Week 8, Day 1)

| # | Test File | Implementation | Lines | TDD Tests |
|---|-----------|----------------|-------|-----------|
| 5 | `DesktopNavigationExecutorTest.kt` | `DesktopNavigationExecutor.kt` | 200 | 8 |
| 6 | `DesktopUIExecutorTest.kt` | `DesktopUIExecutor.kt` | 200 | 8 |
| 7 | `DesktopInputExecutorTest.kt` | `DesktopInputExecutor.kt` | 150 | 6 |
| 8 | `DesktopSystemExecutorTest.kt` | `DesktopSystemExecutor.kt` | 150 | 5 |

### Phase 15 Deliverables

```
Modules/VoiceOSCoreNG/
├── src/iosMain/kotlin/.../handlers/
│   ├── IOSNavigationExecutor.kt
│   ├── IOSUIExecutor.kt
│   ├── IOSInputExecutor.kt
│   └── IOSSystemExecutor.kt
├── src/desktopMain/kotlin/.../handlers/
│   ├── DesktopNavigationExecutor.kt
│   ├── DesktopUIExecutor.kt
│   ├── DesktopInputExecutor.kt
│   └── DesktopSystemExecutor.kt
└── src/iosTest/ & src/desktopTest/
    └── [8 test files]
```

---

## Swarm Execution Strategy

### Parallel Agent Assignment

```
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 10: OVERLAY SYSTEM                      │
├─────────────────────────────────────────────────────────────────┤
│ Agent 1: Theme System        │ Agent 2: Visual Overlays         │
│ - OverlayConfig.kt           │ - CommandStatusOverlay.kt        │
│ - OverlayTheme.kt            │ - ConfidenceOverlay.kt           │
│ - OverlayThemes.kt           │ - ContextMenuOverlay.kt          │
│ - NumberOverlayStyle.kt      │ - NumberedSelectionOverlay.kt    │
├─────────────────────────────────────────────────────────────────┤
│ Agent 3: Infrastructure      │ Agent 4: Android Integration     │
│ - IOverlay.kt                │ - AndroidOverlayManager.kt       │
│ - BaseOverlay.kt             │ - Platform tests                 │
│ - OverlayManager.kt          │                                  │
│ - OverlayCoordinator.kt      │                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                 PHASE 11: CURSOR/FOCUS SYSTEM                    │
├─────────────────────────────────────────────────────────────────┤
│ Agent 1: Position/Bounds     │ Agent 2: Gestures/Actions        │
│ - CursorPositionTracker.kt   │ - CursorGestureHandler.kt        │
│ - BoundaryDetector.kt        │ - CommandMapper.kt               │
│ - CursorPosition.kt          │ - SnapToElementHandler.kt        │
├─────────────────────────────────────────────────────────────────┤
│ Agent 3: Style/Visibility    │ Agent 4: Integration             │
│ - CursorStyleManager.kt      │ - CursorManager.kt (facade)      │
│ - CursorVisibilityManager.kt │ - FocusIndicator.kt              │
│ - SpeedController.kt         │ - CursorHistoryTracker.kt        │
└─────────────────────────────────────────────────────────────────┘
```

### Sequential Dependencies

```
Phase 10 (Overlay) ─┬─► Phase 11 (Cursor) ─┬─► Phase 12 (Handlers)
                    │                       │
                    │                       └─► Phase 13 (Speech)
                    │
                    └─► Phase 14 (LearnApp) ─► Phase 15 (Executors)
```

---

## Quality Gates

### Per-Phase Gates

| Gate | Criteria | Blocking |
|------|----------|----------|
| Tests Pass | 100% green | Yes |
| Coverage | ≥80% unit, ≥60% integration | Yes |
| No TODO/FIXME | 0 in new code | Yes |
| Lint Clean | 0 warnings | No |
| Build Success | All targets compile | Yes |

### TDD Checkpoints

| Checkpoint | Verification |
|------------|--------------|
| Before implementation | Test file exists and fails |
| After implementation | Test passes, no extra code |
| After refactor | Tests still pass |
| Before commit | All tests green |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Compose Multiplatform iOS issues | Keep Android-only initially, add iOS later |
| WindowManager API changes | Abstract behind interface |
| Speech SDK compatibility | Version-lock dependencies |
| Test flakiness | Use TestDispatcher, mock time |

---

## Success Criteria

### Phase Completion

- [ ] Phase 10: All overlay tests pass, Android overlays work
- [ ] Phase 11: Cursor movement works, focus visible
- [ ] Phase 12: All 8 handlers ported with tests
- [ ] Phase 13: Vosk + Azure engines functional
- [ ] Phase 14: LearnApp exploration sessions work
- [ ] Phase 15: iOS/Desktop basic functionality

### Final Validation

- [ ] 290+ tests passing
- [ ] 80%+ code coverage
- [ ] All P0/P1 commands working
- [ ] No regression from VoiceOSCore

---

## Appendix: File Count Summary

| Phase | Files | Tests | Total Lines |
|-------|-------|-------|-------------|
| 10 | 14 | 14 | ~3,800 |
| 11 | 12 | 12 | ~4,400 |
| 12 | 9 | 9 | ~2,400 |
| 13 | 2 | 2 | ~900 |
| 14 | 7 | 7 | ~2,000 |
| 15 | 8 | 8 | ~1,200 |
| **Total** | **52** | **52** | **~14,700** |

---

**Created:** 2026-01-06
**Methodology:** TDD with .swarm parallel execution
**Next Step:** `/i.implement` Phase 10
