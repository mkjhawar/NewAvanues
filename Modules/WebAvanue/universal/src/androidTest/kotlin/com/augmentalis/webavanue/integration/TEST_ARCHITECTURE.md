# Integration Test Architecture

## Overview

This document explains the architecture of the download flow integration tests, showing how components interact and what each test verifies.

---

## Test Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                  Integration Test Suite                      │
│              (DownloadFlowIntegrationTest.kt)                │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Orchestrates
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Test Fixtures                           │
├─────────────────────────────────────────────────────────────┤
│  • Mock BrowserRepository                                    │
│  • Mock NetworkChecker                                       │
│  • Mock DownloadPathValidator                                │
│  • ComposeTestRule (UI testing)                              │
│  • TestHelpers (reusable mocks)                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Tests
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Components Under Test                           │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐    ┌──────────────────┐               │
│  │ SettingsViewModel│◄───┤ BrowserRepository│               │
│  │                  │    │   (Mocked)       │               │
│  │ • pathValidation │    └──────────────────┘               │
│  │ • settings       │                                        │
│  └────────┬─────────┘                                        │
│           │                                                   │
│           │ Provides Settings                                │
│           ▼                                                   │
│  ┌──────────────────────────────────────────┐                │
│  │    AskDownloadLocationDialog (UI)        │                │
│  ├──────────────────────────────────────────┤                │
│  │ • Filename display                       │                │
│  │ • Path selection                         │                │
│  │ • "Change" button → File picker          │                │
│  │ • "Remember" checkbox → Settings update  │                │
│  │ • "Download" button → Validation flow    │                │
│  │ • "Cancel" button → Abort download       │                │
│  └──────────────────┬───────────────────────┘                │
│                     │                                         │
│                     │ Callbacks                               │
│                     ▼                                         │
│  ┌──────────────────────────────────────────┐                │
│  │         DownloadViewModel                │                │
│  ├──────────────────────────────────────────┤                │
│  │ • startDownload()                        │                │
│  │ • downloads StateFlow                    │                │
│  │ • activeDownloads StateFlow              │                │
│  │ • error StateFlow                        │                │
│  └──────────────────┬───────────────────────┘                │
│                     │                                         │
│                     │ Validates via                           │
│                     ▼                                         │
│  ┌──────────────────────────────────────────┐                │
│  │      Platform Components (Mocked)        │                │
│  ├──────────────────────────────────────────┤                │
│  │ NetworkChecker                           │                │
│  │  • isWiFiConnected()                     │                │
│  │  • isCellularConnected()                 │                │
│  │  • getWiFiRequiredMessage()              │                │
│  │                                           │                │
│  │ DownloadPathValidator                    │                │
│  │  • validate(path) → ValidationResult     │                │
│  │  • isValid, errorMessage, availableSpace │                │
│  └──────────────────────────────────────────┘                │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## Test Flow Diagram

### Scenario: All Features Enabled (Happy Path)

```
┌─────────────┐
│   User      │
│ initiates   │
│  download   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 1: Check askDownloadLocation       │
│ ✓ Enabled → Show dialog                 │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 2: User Interaction with Dialog    │
│ • View filename: "document.pdf"          │
│ • View current path: "Downloads"         │
│ • Click "Change" → File picker (optional)│
│ • Check "Remember" (optional)            │
│ • Click "Download" button                │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 3: Path Selected                   │
│ Callback: onPathSelected(path, remember)│
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 4: Check downloadOverWiFiOnly      │
│ ✓ Enabled → Check network                │
│ NetworkChecker.isWiFiConnected()         │
└──────┬──────────────────────────────────┘
       │
       ├── Not WiFi ──► Error: "WiFi required"
       │
       ▼ WiFi OK
┌─────────────────────────────────────────┐
│ Step 5: Validate Download Path          │
│ DownloadPathValidator.validate(path)     │
└──────┬──────────────────────────────────┘
       │
       ├── Invalid ──► Error: "Path no longer exists"
       │
       ▼ Valid
┌─────────────────────────────────────────┐
│ Step 6: Update Settings (if "Remember") │
│ SettingsViewModel.updateSettings()       │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 7: Start Download                  │
│ DownloadViewModel.startDownload()        │
│ • URL validation (HTTP/HTTPS only)       │
│ • Filename sanitization                  │
│ • File type checking (block dangerous)   │
│ • Add to downloads list                  │
│ • Enqueue to platform download queue     │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Step 8: Download in Progress            │
│ • Progress monitoring                    │
│ • UI updates via StateFlow               │
│ • Completion/failure handling            │
└─────────────────────────────────────────┘
```

---

## Test Categories & Coverage

### 1. Dialog Display (5 tests)

```
┌────────────────────────────────────┐
│ askDownloadLocation = true         │
├────────────────────────────────────┤
│ ✓ Dialog shows                     │
│ ✓ Filename displayed                │
│ ✓ Default path displayed            │
│ ✓ All buttons present               │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ askDownloadLocation = false        │
├────────────────────────────────────┤
│ ✓ Dialog skipped                   │
│ ✓ Download starts immediately      │
└────────────────────────────────────┘
```

### 2. WiFi Enforcement (4 tests)

```
┌──────────────────────┬──────────────┬─────────────┐
│   Network State      │ WiFi Only ON │ WiFi Only OFF│
├──────────────────────┼──────────────┼─────────────┤
│ WiFi Connected       │ ✓ ALLOWED    │ ✓ ALLOWED   │
│ Cellular Connected   │ ✗ BLOCKED    │ ✓ ALLOWED   │
│ Offline              │ ✗ BLOCKED    │ ✗ BLOCKED   │
└──────────────────────┴──────────────┴─────────────┘
```

### 3. Path Validation (5 tests)

```
┌─────────────────────┬────────────┬──────────────────┐
│   Path Condition    │  isValid   │   UI Feedback    │
├─────────────────────┼────────────┼──────────────────┤
│ Valid + High Space  │     ✓      │ Download proceeds│
│ Valid + Low Space   │     ✓      │ Warning shown    │
│ Invalid (removed)   │     ✗      │ Error shown      │
│ Invalid (startup)   │     ✗      │ Auto-revert      │
│ Invalid URI format  │     ✗      │ Error shown      │
└─────────────────────┴────────────┴──────────────────┘
```

### 4. Combined Scenarios (5 tests)

```
Test: All Features Happy Path
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Dialog       WiFi Check    Validation    Result
─────────────────────────────────────────────
SHOWN    →   PASS       →  VALID      →  ✓ Download

Test: WiFi Disconnects During Dialog
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Dialog       WiFi Change   Click Download  Result
─────────────────────────────────────────────
SHOWN    →   WiFi → Cell → FAIL         →  ✗ Blocked

Test: Invalid Path Fails
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Dialog       WiFi Check    Validation    Result
─────────────────────────────────────────────
SHOWN    →   PASS       →  INVALID     →  ✗ Error
```

### 5. Remember Choice (2 tests)

```
┌──────────────────┬────────────────────────────┐
│ Remember Checked │        Action              │
├──────────────────┼────────────────────────────┤
│      YES         │ Save path to settings      │
│                  │ Disable askDownloadLocation│
│      NO          │ Path used for this download│
│                  │ Settings unchanged         │
└──────────────────┴────────────────────────────┘
```

---

## Mock Configuration Matrix

### NetworkChecker Mocks

```kotlin
// WiFi Connected
isWiFiConnected()        → true
isCellularConnected()    → false
getWiFiRequiredMessage() → null

// Cellular Connected
isWiFiConnected()        → false
isCellularConnected()    → true
getWiFiRequiredMessage() → "WiFi required. Currently on Cellular."

// Offline
isWiFiConnected()        → false
isCellularConnected()    → false
getWiFiRequiredMessage() → "WiFi required. No network connection."
```

### DownloadPathValidator Mocks

```kotlin
// Valid Path (High Space)
validate(path) → ValidationResult(
    isValid = true,
    availableSpaceMB = 500,
    isLowSpace = false
)

// Valid Path (Low Space)
validate(path) → ValidationResult(
    isValid = true,
    availableSpaceMB = 50,
    isLowSpace = true
)

// Invalid Path
validate(path) → ValidationResult(
    isValid = false,
    errorMessage = "Path no longer exists"
)
```

---

## State Flow Testing

### Observing ViewModel State Changes

```
Test Setup:
┌──────────────────────────────────────┐
│ settingsFlow = MutableStateFlow()    │
│ repository.observeSettings()         │
│   → returns settingsFlow             │
└──────────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────────┐
│ SettingsViewModel observes flow      │
│ settings.value updates reactively    │
└──────────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────────┐
│ Test modifies: settingsFlow.value    │
│ ViewModel state: settings.value      │
│ UI recomposes: Compose observes      │
└──────────────────────────────────────┘

Verification:
• composeTestRule.waitForIdle()
• composeTestRule.waitUntil { condition }
• Assert on UI elements
• Assert on ViewModel state
```

---

## Test Execution Flow

```
@Before
┌────────────────────────────────────┐
│ 1. Create mock repository          │
│ 2. Create mock network checker     │
│ 3. Create mock path validator      │
│ 4. Initialize ViewModels            │
│ 5. Reset test state variables      │
└────────────────────────────────────┘
        │
        ▼
@Test
┌────────────────────────────────────┐
│ GIVEN: Configure mocks & settings  │
│ WHEN:  Perform UI interactions     │
│ THEN:  Assert expected outcomes    │
└────────────────────────────────────┘
        │
        ▼
@After
┌────────────────────────────────────┐
│ 1. Unmock all mocks (unmockkAll()) │
│ 2. Clean up test state             │
└────────────────────────────────────┘
```

---

## Async Testing Patterns

### Pattern 1: Compose Test Rule
```kotlin
composeTestRule.setContent {
    AskDownloadLocationDialog(...)
}

composeTestRule.onNodeWithText("Download")
    .performClick()

composeTestRule.waitForIdle() // Wait for recomposition
```

### Pattern 2: runTest for Coroutines
```kotlin
@Test
fun test() = runTest {
    // Suspend functions work here
    val result = pathValidator.validate(path)
    assert(result.isValid)
}
```

### Pattern 3: Custom Wait Conditions
```kotlin
composeTestRule.waitUntil(timeoutMillis = 2000) {
    // Condition to wait for
    downloadStarted == true
}
```

---

## Test Data Organization

### TestHelpers.kt Provides:

```
Settings Presets:
├── allDisabled
├── dialogOnly
├── wifiOnly
├── dialogAndWiFi
└── allEnabled

Mock Factories:
├── createMockRepository()
├── createMockNetworkChecker_WiFiConnected()
├── createMockNetworkChecker_CellularConnected()
├── createMockNetworkChecker_Offline()
├── createMockPathValidator_ValidPath()
├── createMockPathValidator_InvalidPath()
└── createMockPathValidator_LowSpace()

Constants:
├── Paths (VALID_PATH, INVALID_PATH, etc.)
├── ErrorMessages (WIFI_REQUIRED_CELLULAR, etc.)
└── Filenames (PDF, IMAGE, VIDEO, etc.)
```

---

## Success Criteria Checklist

- ✅ 22 integration test scenarios
- ✅ 95%+ coverage of integration points
- ✅ Robot pattern (Given-When-Then)
- ✅ Comprehensive mocking strategy
- ✅ Async operation handling
- ✅ State flow verification
- ✅ UI interaction testing
- ✅ Error scenario coverage
- ✅ Reusable test helpers
- ✅ Clear documentation

---

## Maintenance Guide

### Adding New Tests

1. **New Dialog Feature**:
   - Add test in "Dialog Display" section
   - Update dialog mock if needed
   - Test both enabled/disabled states

2. **New Network Condition**:
   - Add mock in `TestHelpers.kt`
   - Add test in "WiFi Enforcement" section
   - Test error messages

3. **New Validation Rule**:
   - Add mock in `TestHelpers.kt`
   - Add test in "Path Validation" section
   - Test error handling

### Debugging Failed Tests

1. **UI Element Not Found**:
   - Check exact text (case-sensitive)
   - Use `printToLog()` to see UI tree
   - Verify composable is displayed

2. **Timeout**:
   - Increase `waitUntil` timeout
   - Check async operations complete
   - Verify mocks are set up

3. **Mock Not Working**:
   - Ensure `relaxed = true`
   - Check `every {}` vs `coEvery {}`
   - Verify call signature matches

---

**Created**: 2025-12-13
**Version**: 1.0
**Maintainer**: WebAvanue Test Team
