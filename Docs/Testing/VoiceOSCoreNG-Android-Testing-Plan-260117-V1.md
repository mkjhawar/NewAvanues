# VoiceOSCoreNG Android Autonomous Testing Plan

**Version:** 1.0
**Date:** 2026-01-17
**Module:** android/apps/voiceoscoreng
**Test Type:** Instrumented + Unit
**Coverage Target:** Full (UI, Service, Database, Voice, Overlay)

---

## 1. Overview

This document defines the autonomous testing strategy for the VoiceOSCoreNG Android app. Tests will run on Android Studio Emulator (API 28-34) with full coverage of all components.

### 1.1 Test Environment

| Component | Configuration |
|-----------|---------------|
| Emulator | Android Studio AVD |
| API Level | 34 (primary), 28-33 (compatibility) |
| Device Profile | Pixel 6 (1080x2400) |
| Test Runner | AndroidJUnitRunner |
| UI Testing | Compose Testing + Espresso |
| Mocking | MockK / Mockito |

### 1.2 Coverage Goals

| Category | Target | Method |
|----------|--------|--------|
| Accessibility Service | 100% | Instrumented |
| Database Operations | 100% | Unit + Instrumented |
| Overlay System | 90% | Instrumented |
| Voice Recognition | 80% | Mocked |
| UI Components | 90% | Compose Testing |

---

## 2. Test Categories

### 2.1 Unit Tests (testDebugUnitTest)

Location: `src/test/kotlin/`

| Test Class | Component | Tests |
|------------|-----------|-------|
| `ScreenCacheManagerTest` | Screen hashing & caching | 12 |
| `ElementExtractorTest` | Tree traversal logic | 15 |
| `DynamicCommandGeneratorTest` | Command generation | 10 |
| `DeduplicationTest` | Duplicate detection | 8 |
| `OverlayStateManagerTest` | Overlay state logic | 10 |

### 2.2 Instrumented Tests (connectedDebugAndroidTest)

Location: `src/androidTest/kotlin/`

| Test Class | Component | Priority | Tests |
|------------|-----------|----------|-------|
| `AccessibilityServiceTest` | VoiceOSAccessibilityService | P0 | 25 |
| `DatabaseIntegrationTest` | SQLDelight operations | P0 | 20 |
| `OverlayServiceTest` | OverlayService lifecycle | P1 | 12 |
| `MainActivityTest` | MainActivity UI | P1 | 15 |
| `SettingsActivityTest` | AccessibilitySettingsActivity | P2 | 10 |
| `PermissionFlowTest` | Permission handling | P0 | 8 |

---

## 3. Component Test Specifications

### 3.1 VoiceOSAccessibilityService Tests

**File:** `AccessibilityServiceTest.kt`

#### Lifecycle Tests

| ID | Test Case | Expected | Verification |
|----|-----------|----------|--------------|
| AS-001 | Service connects | `isConnected.value == true` | StateFlow assertion |
| AS-002 | Service disconnects | Resources released | Memory check |
| AS-003 | Service restarts | State restored | StateFlow comparison |

#### Exploration Tests

| ID | Test Case | Input | Expected |
|----|-----------|-------|----------|
| AS-010 | Explore current app | Any foreground app | `explorationResults` populated |
| AS-011 | Explore all apps | N/A | Multiple app results |
| AS-012 | Get current commands | After exploration | Non-empty command list |
| AS-013 | Get dynamic command count | After exploration | Count > 0 |

#### Voice Listening Tests

| ID | Test Case | Action | Expected |
|----|-----------|--------|----------|
| AS-020 | Start listening | `startListening()` | `isVoiceListening.value == true` |
| AS-021 | Stop listening | `stopListening()` | `isVoiceListening.value == false` |
| AS-022 | Check listening state | `isListening()` | Matches StateFlow |
| AS-023 | Transcription received | Mock speech result | `lastTranscription` updated |

#### Continuous Monitoring Tests

| ID | Test Case | Action | Expected |
|----|-----------|--------|----------|
| AS-030 | Enable monitoring | `setContinuousMonitoring(true)` | `isContinuousMonitoring.value == true` |
| AS-031 | Disable monitoring | `setContinuousMonitoring(false)` | `isContinuousMonitoring.value == false` |
| AS-032 | Check monitoring state | `isContinuousMonitoringEnabled()` | Matches StateFlow |
| AS-033 | Screen change detected | Navigate to new screen | Debounced scan triggered (300ms) |

#### Rescan Tests

| ID | Test Case | Action | Expected |
|----|-----------|--------|----------|
| AS-040 | Rescan current app | `rescanCurrentApp()` | Cache cleared for package |
| AS-041 | Rescan everything | `rescanEverything()` | All caches cleared |
| AS-042 | Get cached screen count | After scans | Count matches DB |
| AS-043 | Get cached for current app | After scans | Package-specific count |

#### Overlay Management Tests

| ID | Test Case | Action | Expected |
|----|-----------|--------|----------|
| AS-050 | Set numbers mode | `setNumbersOverlayMode(ON)` | Mode changes |
| AS-051 | Cycle numbers mode | `cycleNumbersOverlayMode()` | Cycles: ON→OFF→AUTO |
| AS-052 | Set instruction bar | `setInstructionBarMode(ON)` | Mode changes |
| AS-053 | Cycle instruction bar | `cycleInstructionBarMode()` | Cycles correctly |
| AS-054 | Set badge theme | `setBadgeTheme(BLUE)` | Theme changes |
| AS-055 | Cycle badge theme | `cycleBadgeTheme()` | Cycles through themes |

### 3.2 Database Integration Tests

**File:** `DatabaseIntegrationTest.kt`

#### Schema Tests

| ID | Test Case | Operation | Expected |
|----|-----------|-----------|----------|
| DB-001 | Database creates | App launch | Schema initialized |
| DB-002 | Tables exist | Query tables | All 3 FK tables present |
| DB-003 | Indexes exist | Query indexes | Performance indexes created |

#### CRUD Operations

| ID | Test Case | Operation | Expected |
|----|-----------|-----------|----------|
| DB-010 | Insert scraped app | `scrapedAppRepository.insert()` | Row inserted |
| DB-011 | Insert scraped element | `scrapedElementRepository.insert()` | Row inserted |
| DB-012 | Insert generated command | `generatedCommandRepository.insert()` | Row inserted |
| DB-013 | FK constraint enforced | Insert command without app | Exception thrown |
| DB-014 | Query commands by screen | `getCommandsForScreen(hash)` | Correct commands |
| DB-015 | Clear commands for app | `clearForPackage(pkg)` | Commands deleted |
| DB-016 | Clear all commands | `clearAll()` | All deleted |

#### Screen Cache Tests

| ID | Test Case | Operation | Expected |
|----|-----------|-----------|----------|
| DB-020 | Save screen hash | `saveScreen(hash, pkg, ver, count)` | Hash stored |
| DB-021 | Check screen exists | `hasScreen(hash)` | true if exists |
| DB-022 | Get screen count | `getScreenCount()` | Correct count |
| DB-023 | Get count for package | `getScreenCountForPackage(pkg)` | Package count |
| DB-024 | Clear screens for package | `clearScreensForPackage(pkg)` | Package screens deleted |
| DB-025 | Clear all screens | `clearAllScreens()` | All cleared |

#### Command Persistence Tests

| ID | Test Case | Operation | Expected |
|----|-----------|-----------|----------|
| DB-030 | Save commands for screen | `saveCommandsForScreen(hash, cmds)` | Commands linked |
| DB-031 | Load commands for screen | `getCommandsForScreen(hash)` | Commands returned |
| DB-032 | Update command usage | Increment `usageCount` | Count updated |
| DB-033 | Mark user approved | Set `isUserApproved` | Flag persisted |

### 3.3 Overlay Service Tests

**File:** `OverlayServiceTest.kt`

| ID | Test Case | Action | Expected |
|----|-----------|--------|----------|
| OS-001 | Service starts | `OverlayService.start(context)` | Foreground notification |
| OS-002 | Service stops | `OverlayService.stop(context)` | Service destroyed |
| OS-003 | Numbers overlay shows | Numbers mode ON | Badges visible |
| OS-004 | Numbers overlay hides | Numbers mode OFF | Badges hidden |
| OS-005 | Debug FAB shows | Service running | FAB visible |
| OS-006 | Debug FAB draggable | Drag gesture | Position updates |
| OS-007 | FAB corner positions | Position controls | 4 corners work |
| OS-008 | App detection dialog | New app detected | Dialog shows |
| OS-009 | Dialog response saves | User selects "Always" | Preference saved |

### 3.4 MainActivity Tests

**File:** `MainActivityTest.kt`

| ID | Test Case | Action | Expected |
|----|-----------|--------|----------|
| MA-001 | Activity launches | Launch | No crash |
| MA-002 | Status card shows | Check UI | Status indicators visible |
| MA-003 | Accessibility status | Service disabled | Shows "Enable" button |
| MA-004 | Overlay status | Permission missing | Shows "Enable" button |
| MA-005 | Tier toggle works | Click toggle | DEV↔LITE switches |
| MA-006 | Drawer opens | Click menu | Drawer slides out |
| MA-007 | Test mode toggle | Toggle in drawer | State changes |
| MA-008 | Rescan current app | Click button | Rescan triggered |
| MA-009 | Rescan everything | Click + confirm | Full rescan |
| MA-010 | Settings navigation | Click Settings | Opens SettingsActivity |

### 3.5 Permission Flow Tests

**File:** `PermissionFlowTest.kt`

| ID | Test Case | Precondition | Expected |
|----|-----------|--------------|----------|
| PF-001 | Accessibility check | Service disabled | `isAccessibilityServiceEnabled() == false` |
| PF-002 | Accessibility check | Service enabled | `isAccessibilityServiceEnabled() == true` |
| PF-003 | Overlay check | Permission missing | `canDrawOverlays() == false` |
| PF-004 | Overlay check | Permission granted | `canDrawOverlays() == true` |
| PF-005 | Boot receiver | BOOT_COMPLETED | Service starts if permissions OK |
| PF-006 | Boot receiver | Missing permissions | Service not started, no crash |

---

## 4. Screen Hash Algorithm Tests

**File:** `ScreenHashTests.kt`

| ID | Test Case | Input | Expected |
|----|-----------|-------|----------|
| SH-001 | Same screen same hash | Same elements | Identical hash |
| SH-002 | Different screen different hash | Different elements | Different hash |
| SH-003 | Content agnostic | Same structure, different text | Same hash |
| SH-004 | Orientation aware | Portrait vs landscape | Different hashes |
| SH-005 | Structural properties | className, resourceId, bounds | Included in hash |
| SH-006 | Dynamic text ignored | Text changes | Hash unchanged |

---

## 5. Element Extraction Tests

**File:** `ElementExtractionTests.kt`

| ID | Test Case | Input | Expected |
|----|-----------|-------|----------|
| EE-001 | Extract button | Button node | ElementInfo with type=BUTTON |
| EE-002 | Extract text field | EditText node | ElementInfo with type=TEXT_FIELD |
| EE-003 | Extract clickable | Clickable view | `isClickable == true` |
| EE-004 | Extract scrollable | ScrollView | `isScrollable == true` |
| EE-005 | Detect dynamic container | RecyclerView | `isDynamicContainer() == true` |
| EE-006 | Track list index | List item | `listIndex` populated |
| EE-007 | Build hierarchy | Nested views | HierarchyNode tree |
| EE-008 | Detect duplicates | Repeated elements | DuplicateInfo list |
| EE-009 | Depth tracking | Deep nesting | Correct depth values |

---

## 6. Voice Command Flow Tests

**File:** `VoiceCommandFlowTests.kt`

| ID | Test Case | Voice Input | Expected |
|----|-----------|-------------|----------|
| VC-001 | Simple click | "click submit" | Element clicked |
| VC-002 | Synonym recognition | "tap submit" | Same element clicked |
| VC-003 | Number selection | "3" | Third element selected |
| VC-004 | Navigation command | "go to settings" | Settings opened |
| VC-005 | Unknown command | "foobar123" | NotHandled result |
| VC-006 | Disambiguation | "click button" (5 matches) | AwaitingSelection |
| VC-007 | Low confidence | Ambiguous speech | RequiresInput prompt |

---

## 7. Test Execution

### 7.1 Run Commands

```bash
# Unit tests only
./gradlew :android:apps:voiceoscoreng:testDebugUnitTest

# Instrumented tests (requires emulator)
./gradlew :android:apps:voiceoscoreng:connectedDebugAndroidTest

# All tests
./gradlew :android:apps:voiceoscoreng:testDebugUnitTest \
          :android:apps:voiceoscoreng:connectedDebugAndroidTest

# Specific test class
./gradlew :android:apps:voiceoscoreng:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.augmentalis.voiceoscoreng.AccessibilityServiceTest
```

### 7.2 Emulator Setup

```bash
# Create emulator (if not exists)
avdmanager create avd -n VoiceOS_Test -k "system-images;android-34;google_apis;x86_64" -d pixel_6

# Start emulator
emulator -avd VoiceOS_Test -no-snapshot-load

# Grant accessibility service (via adb)
adb shell settings put secure enabled_accessibility_services \
    com.augmentalis.voiceoscoreng/.service.VoiceOSAccessibilityService

# Grant overlay permission
adb shell appops set com.augmentalis.voiceoscoreng SYSTEM_ALERT_WINDOW allow
```

### 7.3 Test Reports

Reports generated at:
- `android/apps/voiceoscoreng/build/reports/tests/testDebugUnitTest/`
- `android/apps/voiceoscoreng/build/reports/androidTests/connected/`

---

## 8. Mock Strategy

### 8.1 Components to Mock

| Component | Mock Library | Purpose |
|-----------|--------------|---------|
| AccessibilityNodeInfo | MockK | Fake accessibility tree |
| SpeechRecognizer | MockK | Mock voice recognition |
| WindowManager | Robolectric | Mock overlay window |
| SharedPreferences | In-memory | Fast preference tests |
| DatabaseDriver | SQLDelight test driver | In-memory database |

### 8.2 Mock Accessibility Tree

```kotlin
fun createMockAccessibilityTree(): AccessibilityNodeInfo {
    val root = mockk<AccessibilityNodeInfo> {
        every { className } returns "android.widget.FrameLayout"
        every { childCount } returns 3
        every { getChild(0) } returns createMockButton("Submit")
        every { getChild(1) } returns createMockTextField("Username")
        every { getChild(2) } returns createMockScrollView()
    }
    return root
}

fun createMockButton(text: String): AccessibilityNodeInfo {
    return mockk<AccessibilityNodeInfo> {
        every { className } returns "android.widget.Button"
        every { text } returns text
        every { isClickable } returns true
        every { isEnabled } returns true
        every { childCount } returns 0
    }
}
```

---

## 9. Continuous Integration

### 9.1 CI Pipeline

```yaml
test-android:
  runs-on: macos-latest
  steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Run unit tests
      run: ./gradlew :android:apps:voiceoscoreng:testDebugUnitTest

    - name: Start emulator
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 34
        script: ./gradlew :android:apps:voiceoscoreng:connectedDebugAndroidTest

    - name: Upload reports
      uses: actions/upload-artifact@v3
      with:
        name: test-reports
        path: android/apps/voiceoscoreng/build/reports/
```

### 9.2 Quality Gates

| Gate | Threshold | Action |
|------|-----------|--------|
| Unit test pass rate | 100% | Block merge |
| Instrumented test pass rate | 95% | Block merge |
| Code coverage | 80% | Warning |
| Critical path coverage | 100% | Block merge |

---

## 10. Test Data Fixtures

### 10.1 Sample Elements

```kotlin
object TestFixtures {
    val sampleButton = ElementInfo(
        className = "android.widget.Button",
        resourceId = "com.test.app:id/btn_submit",
        text = "Submit",
        contentDescription = "Submit form",
        bounds = Bounds(100, 200, 300, 250),
        isClickable = true,
        isLongClickable = false,
        isScrollable = false,
        isEnabled = true,
        packageName = "com.test.app",
        isInDynamicContainer = false,
        containerType = "",
        listIndex = -1
    )

    val sampleTextField = ElementInfo(
        className = "android.widget.EditText",
        resourceId = "com.test.app:id/txt_username",
        text = "",
        contentDescription = "Username",
        bounds = Bounds(50, 100, 350, 150),
        isClickable = true,
        isLongClickable = false,
        isScrollable = false,
        isEnabled = true,
        packageName = "com.test.app",
        isInDynamicContainer = false,
        containerType = "",
        listIndex = -1
    )

    val sampleListItem = ElementInfo(
        className = "android.view.View",
        resourceId = "",
        text = "Item 1",
        contentDescription = "",
        bounds = Bounds(0, 0, 1080, 100),
        isClickable = true,
        isLongClickable = true,
        isScrollable = false,
        isEnabled = true,
        packageName = "com.test.app",
        isInDynamicContainer = true,
        containerType = "RecyclerView",
        listIndex = 0
    )
}
```

### 10.2 Sample Commands

```kotlin
object CommandFixtures {
    val clickSubmit = QuantizedCommand(
        avid = "cmd-click-submit",
        phrase = "click submit",
        actionType = CommandActionType.CLICK,
        targetAvid = "btn-submit-123",
        confidence = 0.95f,
        metadata = mapOf("packageName" to "com.test.app")
    )

    val typeUsername = QuantizedCommand(
        avid = "cmd-type-username",
        phrase = "type in username",
        actionType = CommandActionType.TYPE,
        targetAvid = "txt-username-456",
        confidence = 0.9f,
        metadata = mapOf("packageName" to "com.test.app")
    )
}
```

---

## 11. Logging & Verification

### 11.1 Log Categories

| Tag | Purpose |
|-----|---------|
| `VOS_TEST` | Test execution logs |
| `VOS_DB` | Database operation logs |
| `VOS_A11Y` | Accessibility service logs |
| `VOS_OVERLAY` | Overlay system logs |
| `VOS_VOICE` | Voice recognition logs |

### 11.2 Assertion Patterns

```kotlin
// StateFlow assertion
suspend fun <T> assertStateFlow(
    flow: StateFlow<T>,
    expected: T,
    timeout: Duration = 5.seconds
) {
    withTimeout(timeout) {
        flow.first { it == expected }
    }
}

// Database assertion
fun assertDatabaseContains(
    table: String,
    column: String,
    value: Any
) {
    val result = database.query("SELECT * FROM $table WHERE $column = ?", arrayOf(value))
    assertTrue(result.moveToFirst(), "Expected row with $column = $value in $table")
}

// UI assertion (Compose)
fun ComposeTestRule.assertElementExists(testTag: String) {
    onNodeWithTag(testTag).assertExists()
}
```

---

## 12. Known Limitations

1. **Voice Recognition** - Cannot be fully tested without physical microphone; mocked in CI
2. **Overlay Rendering** - Visual verification requires screenshot comparison
3. **Accessibility Events** - Synthetic events differ from real device events
4. **Multi-app Testing** - Requires test apps installed on emulator
5. **Battery/Performance** - Long-running tests may be affected by emulator performance

---

**Document Status:** Active
**Last Updated:** 2026-01-17
**Next Review:** 2026-02-17
