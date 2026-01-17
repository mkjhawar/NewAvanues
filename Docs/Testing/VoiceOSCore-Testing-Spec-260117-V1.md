# VoiceOSCore Testing Specification

**Version:** 1.0
**Date:** 2026-01-17
**Author:** Claude (Automated Testing Framework)
**Module:** Modules/VoiceOSCore

---

## 1. Overview

This document defines the comprehensive testing strategy for the VoiceOSCore KMP module. It covers unit tests, integration tests, and platform-specific validation.

### 1.1 Module Purpose

VoiceOSCore is a cross-platform Kotlin Multiplatform module that provides:
- Voice command processing and execution
- UI element detection and interaction
- Synonym-based command matching
- Framework-agnostic accessibility support

### 1.2 Test Coverage Goals

| Metric | Target | Current |
|--------|--------|---------|
| Line Coverage | 80% | Pending |
| Branch Coverage | 75% | Pending |
| Critical Path Coverage | 100% | Pending |

---

## 2. Test Categories

### 2.1 Unit Tests (commonTest)

Located in: `src/commonTest/kotlin/com/augmentalis/voiceoscore/`

| Test File | Component | Test Count | Status |
|-----------|-----------|------------|--------|
| `CommandGeneratorUtilsTest.kt` | CommandGeneratorUtils | 18 | Created |
| `QuantizedCommandTest.kt` | QuantizedCommand | 12 | Created |
| `HandlerResultTest.kt` | HandlerResult | 18 | Created |
| `SynonymSetTest.kt` | SynonymSet, SynonymMap | 12 | Created |

### 2.2 Android Instrumentation Tests

Located in: `src/androidInstrumentedTest/kotlin/`

| Test File | Component | Priority |
|-----------|-----------|----------|
| `AccessibilityIntegrationTest.kt` | Accessibility Service | P0 |
| `SpeechRecognitionTest.kt` | Speech Engine | P0 |
| `OverlayRenderingTest.kt` | Overlay System | P1 |
| `CommandExecutionTest.kt` | Action Coordinator | P0 |

### 2.3 Desktop Tests (desktopTest)

Located in: `src/desktopTest/kotlin/`

| Test File | Component | Priority |
|-----------|-----------|----------|
| `JvmLoggerTest.kt` | Logging System | P2 |
| `SynonymPathsTest.kt` | File System Paths | P1 |

---

## 3. Test Specifications by Component

### 3.1 CommandGeneratorUtils

**Purpose:** Generate voice commands from UI elements

**Test Scenarios:**

| ID | Scenario | Input | Expected Output |
|----|----------|-------|-----------------|
| CG-001 | Generate trigger for navigation | label="settings", category=NAVIGATION | "go to settings" |
| CG-002 | Generate trigger for action | label="submit", category=ACTION | "click submit" |
| CG-003 | Generate trigger for input | label="username", category=INPUT | "type in username" |
| CG-004 | Handle special characters | label="Save & Exit!" | "click save exit" |
| CG-005 | Generate synonyms for home | trigger="go to home" | ["go to main", "go to start"] |
| CG-006 | Skip blank labels | label="" | null |
| CG-007 | Skip unlabeled elements | label="unlabeled" | null |
| CG-008 | Skip non-actionable elements | actions="" | null |
| CG-009 | Validate command length | commandText="x" (1 char) | Filtered out |
| CG-010 | Validate confidence threshold | confidence=0.1 | Filtered out |
| CG-011 | Validate action type | actionType="invalid" | Filtered out |
| CG-012 | Deduplicate commands | duplicate triggers | Single entry |

### 3.2 QuantizedCommand

**Purpose:** Represent a voice command with metadata

**Test Scenarios:**

| ID | Scenario | Expected |
|----|----------|----------|
| QC-001 | Basic construction | All fields populated correctly |
| QC-002 | Default values | targetAvid="", confidence=1.0, aliases=[] |
| QC-003 | Multiple aliases | aliases list preserved |
| QC-004 | Action types | CLICK, LONG_CLICK, TYPE, SCROLL |
| QC-005 | Equality comparison | Same values = equal |
| QC-006 | Copy with modification | Original unchanged |

### 3.3 HandlerResult

**Purpose:** Represent command execution results

**Test Scenarios:**

| ID | Scenario | Expected |
|----|----------|----------|
| HR-001 | Success with message | isSuccess=true, message set |
| HR-002 | Success with data | data map accessible |
| HR-003 | Failure with reason | isFailure=true, reason set |
| HR-004 | Failure recoverable flag | recoverable property works |
| HR-005 | NotHandled state | isSuccess=false, isFailure=false |
| HR-006 | RequiresInput types | TEXT, NUMBER, CHOICE, CONFIRMATION |
| HR-007 | InProgress with progress | progress 0-100, statusMessage |
| HR-008 | AwaitingSelection | matchCount, announcement |

### 3.4 SynonymMap

**Purpose:** Store and retrieve word synonyms

**Test Scenarios:**

| ID | Scenario | Expected |
|----|----------|----------|
| SM-001 | Add and retrieve | Synonyms returned for word |
| SM-002 | Unknown word | Empty list returned |
| SM-003 | containsWord check | true/false correctly |
| SM-004 | allWords enumeration | All added words returned |
| SM-005 | clear operation | Map emptied |
| SM-006 | size tracking | Correct count |

---

## 4. Integration Test Scenarios

### 4.1 Voice Command Flow (End-to-End)

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────────┐
│   Speech    │────▶│   Command    │────▶│   Handler   │────▶│    Action    │
│   Engine    │     │   Matcher    │     │   Registry  │     │  Coordinator │
└─────────────┘     └──────────────┘     └─────────────┘     └──────────────┘
       │                   │                    │                    │
       ▼                   ▼                    ▼                    ▼
   "click         QuantizedCommand      IHandler found      Element clicked
    submit"       (confidence=0.95)                         (success=true)
```

**Test Cases:**

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| E2E-001 | Simple click command | Say "click submit" | Submit button clicked |
| E2E-002 | Synonym recognition | Say "tap submit" | Submit button clicked |
| E2E-003 | Navigation command | Say "go to settings" | Settings screen opened |
| E2E-004 | Unknown command | Say "foo bar baz" | NotHandled result |
| E2E-005 | Disambiguation | Multiple matches | AwaitingSelection shown |
| E2E-006 | Low confidence | Ambiguous speech | RequiresInput prompt |

### 4.2 Framework Detection

| ID | Framework | Detection Signal | Expected |
|----|-----------|------------------|----------|
| FD-001 | Native Android | ViewGroup classes | FrameworkType.NATIVE |
| FD-002 | Jetpack Compose | ComposeView | FrameworkType.COMPOSE |
| FD-003 | Flutter | FlutterSurfaceView | FrameworkType.FLUTTER |
| FD-004 | React Native | ReactRootView | FrameworkType.REACT_NATIVE |
| FD-005 | WebView | WebView class | FrameworkType.WEBVIEW |
| FD-006 | Unity | UnityPlayer | FrameworkType.UNITY |

### 4.3 Accessibility Integration

| ID | Test Case | Precondition | Expected |
|----|-----------|--------------|----------|
| ACC-001 | Service enabled | Accessibility on | Elements extracted |
| ACC-002 | Service disabled | Accessibility off | Graceful fallback |
| ACC-003 | Element click | Element found | Click performed |
| ACC-004 | Text input | Text field focused | Text entered |
| ACC-005 | Scroll action | Scrollable view | Scroll performed |

---

## 5. Platform-Specific Tests

### 5.1 Android-Specific

| ID | Test Area | Test Case |
|----|-----------|-----------|
| AND-001 | AccessibilityService | Service lifecycle |
| AND-002 | Overlay permissions | SYSTEM_ALERT_WINDOW |
| AND-003 | Speech recognition | Google Speech API |
| AND-004 | TTS feedback | Text-to-speech output |
| AND-005 | Compose UI | Overlay rendering |

### 5.2 Desktop-Specific (JVM)

| ID | Test Area | Test Case |
|----|-----------|-----------|
| DSK-001 | File paths | User home directory |
| DSK-002 | Logging | System.out/err |
| DSK-003 | SQLite driver | Database operations |

### 5.3 iOS-Specific

| ID | Test Area | Test Case |
|----|-----------|-----------|
| IOS-001 | File paths | iOS sandbox |
| IOS-002 | Logging | OSLog/NSLog |
| IOS-003 | Native driver | SQLDelight native |

---

## 6. Test Execution

### 6.1 Run All Tests

```bash
# All platforms
./gradlew :Modules:VoiceOSCore:allTests

# Common tests only (fastest)
./gradlew :Modules:VoiceOSCore:desktopTest

# Android unit tests
./gradlew :Modules:VoiceOSCore:testDebugUnitTest

# Android instrumented tests (requires device/emulator)
./gradlew :Modules:VoiceOSCore:connectedDebugAndroidTest
```

### 6.2 Test Reports

Reports generated at:
- `build/reports/tests/desktopTest/`
- `build/reports/tests/testDebugUnitTest/`
- `build/reports/androidTests/connected/`

### 6.3 Coverage Reports

```bash
# Generate coverage report
./gradlew :Modules:VoiceOSCore:koverReport
```

Reports at: `build/reports/kover/`

---

## 7. Test Data

### 7.1 Sample Elements

```kotlin
val sampleButton = QuantizedElement(
    vuid = "btn-submit-123",
    label = "Submit",
    type = ElementType.BUTTON,
    actions = "click",
    bounds = "100,200,300,250",
    aliases = listOf("send", "confirm")
)

val sampleTextField = QuantizedElement(
    vuid = "txt-username-456",
    label = "Username",
    type = ElementType.TEXT_FIELD,
    actions = "click,edit",
    bounds = "50,100,350,150",
    aliases = emptyList()
)
```

### 7.2 Sample Commands

```kotlin
val sampleClickCommand = QuantizedCommand(
    avid = "cmd-click-submit",
    phrase = "click submit",
    action = CommandActionType.CLICK,
    targetAvid = "btn-submit-123",
    confidence = 0.95f,
    aliases = listOf("tap submit", "press submit")
)
```

---

## 8. Continuous Integration

### 8.1 CI Pipeline Stages

1. **Build** - Compile all targets
2. **Unit Tests** - Run commonTest and desktopTest
3. **Android Tests** - Run on Firebase Test Lab
4. **Coverage** - Generate and verify coverage
5. **Quality Gates** - Fail if coverage < 80%

### 8.2 Pre-commit Checks

```bash
# Quick validation before commit
./gradlew :Modules:VoiceOSCore:compileKotlinMetadata \
          :Modules:VoiceOSCore:compileDebugKotlinAndroid \
          :Modules:VoiceOSCore:desktopTest
```

---

## 9. Known Limitations

1. **iOS Tests** - Require macOS with Xcode; not run in standard CI
2. **Accessibility Tests** - Require physical device or emulator with accessibility enabled
3. **Speech Tests** - Require microphone access; mocked in CI
4. **Overlay Tests** - Require SYSTEM_ALERT_WINDOW permission

---

## 10. Appendix

### A. Error Codes Reference

| Code | Meaning |
|------|---------|
| MODULE_NOT_AVAILABLE | Required module not loaded |
| COMMAND_NOT_FOUND | No handler for command |
| INVALID_PARAMETERS | Bad command parameters |
| PERMISSION_DENIED | Missing permission |
| EXECUTION_FAILED | Action failed |
| TIMEOUT | Operation timed out |
| NO_ACCESSIBILITY_SERVICE | Service not running |

### B. Action Types Reference

| Type | Description |
|------|-------------|
| CLICK | Single tap/click |
| LONG_CLICK | Long press |
| TYPE | Text input |
| SCROLL | Scroll gesture |
| FOCUS | Focus element |
| BACK | Navigate back |

---

**Document Status:** Active
**Last Updated:** 2026-01-17
**Next Review:** 2026-02-17
