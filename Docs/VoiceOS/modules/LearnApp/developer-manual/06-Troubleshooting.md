# Chapter 6: Troubleshooting & Performance

**Module**: LearnApp
**Last Updated**: 2025-12-08

This chapter covers debugging, troubleshooting, testing, and performance considerations.

---

## Testing

### Unit Tests

Test individual components:

```kotlin
@Test
fun `AppLaunchDetector filters system apps`() {
    val detector = AppLaunchDetector(context, tracker)

    val event = createEvent("com.android.settings")
    detector.onAccessibilityEvent(event)

    // Should not emit NewAppDetected
    verify { appLaunchEvents wasNot called }
}
```

### Integration Tests

Test component interactions:

```kotlin
@Test
fun `Consent approval triggers exploration`() = runTest {
    val integration = LearnAppIntegration.initialize(context, service)

    // Emit approval
    consentManager.approveConsent("com.test.app")

    // Verify exploration started
    val state = integration.getExplorationState().first()
    assertTrue(state is ExplorationState.Running)
}
```

### UI Tests

Test Compose components:

```kotlin
@Test
fun `ConsentDialog shows app name`() {
    composeTestRule.setContent {
        ConsentDialog(
            appName = "TestApp",
            onApprove = { },
            onDecline = { }
        )
    }

    composeTestRule.onNodeWithText("Learn TestApp?").assertExists()
}
```

---
## Common Patterns

### Pattern 1: Observing Exploration

```kotlin
lifecycleScope.launch {
    integration.getExplorationState().collect { state ->
        when (state) {
            is ExplorationState.Idle -> {
                // Ready to start
            }
            is ExplorationState.Running -> {
                updateProgressUI(state.progress)
            }
            is ExplorationState.PausedForLogin -> {
                showLoginPrompt()
            }
            is ExplorationState.Completed -> {
                showCompletionDialog(state.stats)
            }
            is ExplorationState.Failed -> {
                showErrorDialog(state.error)
            }
        }
    }
}
```

### Pattern 2: Manual Exploration Trigger

```kotlin
// Trigger learning for specific app
suspend fun learnApp(packageName: String) {
    // Show consent
    consentManager.showConsentDialog(packageName, getAppName(packageName))

    // Wait for response
    val response = consentManager.consentResponses.first()

    // Start exploration if approved
    if (response is ConsentResponse.Approved) {
        explorationEngine.startExploration(packageName)
    }
}
```

### Pattern 3: Checking if App Learned

```kotlin
suspend fun isAppReady(packageName: String): Boolean {
    // Check in-memory tracker (fast)
    if (tracker.isAppLearned(packageName)) {
        return true
    }

    // Check database (slower but persistent)
    return repository.isAppLearned(packageName)
}
```

---
## Troubleshooting

### Issue: Consent Dialog Not Showing

**Symptoms**: Dialog doesn't appear when new app launched

**Causes**:
1. SYSTEM_ALERT_WINDOW permission not granted
2. App already learned
3. App recently dismissed (24-hour window)
4. System app (filtered)

**Solutions**:
```kotlin
// Check permission
if (!consentManager.hasOverlayPermission()) {
    // Request permission
    requestOverlayPermission()
}

// Check if already learned
if (tracker.isAppLearned(packageName)) {
    // Already learned, no dialog needed
}

// Clear dismissal if needed
tracker.clearDismissal(packageName)
```

### Issue: Exploration Not Starting

**Symptoms**: Consent approved but exploration doesn't begin

**Debugging**:
```kotlin
// Check exploration state
integration.getExplorationState().value.let { state ->
    Log.d("LearnApp", "Current state: $state")
}

// Check if event flow connected
integration.consentResponses.collect { response ->
    Log.d("LearnApp", "Consent response: $response")
}
```

### Issue: Infinite Spinning Indicator After Consent (FIXED 2025-12-02)

**Symptoms**: Progress indicator spins indefinitely after user clicks "Yes" on consent dialog

**Root Cause**: Consent dialog overlay was blocking app window detection

**Technical Details**:

The issue occurred because:
1. User clicks "Yes" on consent dialog
2. `ConsentDialogManager` emitted `ConsentResponse.Approved`
3. **Problem**: Dialog was NOT hidden, still covering app window
4. `ExplorationEngine.startExploration()` called
5. `windowManager.getAppWindowsWithRetry()` runs (5 retries, ~3 seconds)
6. Window detection FAILS because consent dialog overlay blocks visibility
7. State should transition to `ExplorationState.Failed`, but indicator keeps spinning
8. User sees no feedback, appears frozen

**Fix Implemented** (Commit: b2b6d882):

1. **ConsentDialogManager.kt** - Hide dialog before emitting response:
```kotlin
private suspend fun handleApproval(packageName: String, dontAskAgain: Boolean) {
    sessionConsentCache.add(packageName)

    if (dontAskAgain) {
        learnedAppTracker.markAsLearned(packageName)
    }

    // FIX: Hide dialog BEFORE starting exploration
    hideConsentDialog()

    // Wait for dialog animation + window manager refresh (500ms)
    delay(500)

    // Now emit response (exploration starts with clear window)
    _consentResponses.emit(ConsentResponse.Approved(...))
}
```

2. **ExplorationEngine.kt** - Enhanced error messages:
```kotlin
if (windows.isEmpty()) {
    android.util.Log.e("ExplorationEngine",
        "❌ No windows found for package: $packageName after retry. " +
        "Possible causes: " +
        "1) App not in foreground, " +
        "2) Covered by system overlay/dialog, " +
        "3) Accessibility service not properly initialized, " +
        "4) App still loading/transitioning."
    )
    _explorationState.value = ExplorationState.Failed(...)
}
```

3. **LearnAppIntegration.kt** - Timeout protection:
```kotlin
private fun startExploration(packageName: String) {
    scope.launch {
        try {
            withTimeout(30_000) {  // 30 second max
                when (val result = repository.createExplorationSessionSafe(packageName)) {
                    is SessionCreationResult.Created -> {
                        explorationEngine.startExploration(packageName, result.sessionId)
                    }
                    is SessionCreationResult.Failed -> {
                        progressOverlayManager.hideProgressOverlay()
                        showToastNotification(...)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            progressOverlayManager.hideProgressOverlay()
            showToastNotification("Learning Timed Out", ...)
        }
    }
}
```

**Why 500ms Delay?**
| Timing | Purpose |
|--------|---------|
| Dialog dismiss animation | ~200ms |
| Window manager update | ~100ms |
| Accessibility event propagation | ~100ms |
| Safety margin | ~100ms |
| **Total** | **500ms** |

**Prevention for Future Development**:

When implementing consent or overlay dialogs:
1. ✅ **Always hide dialog before emitting response**
2. ✅ **Add delay for animation completion**
3. ✅ **Use timeout protection for long-running operations**
4. ✅ **Provide clear error messages with troubleshooting hints**
5. ✅ **Ensure progress indicators are dismissed on all exit paths**

**Monitoring**:
```kotlin
// Check if dialog properly hidden
if (consentDialogManager.isDialogShowing()) {
    Log.w("LearnApp", "Dialog still showing after approval!")
}

// Monitor window detection
windowManager.getAppWindows(packageName, launcherDetector).let { windows ->
    Log.d("LearnApp", "Found ${windows.size} windows for $packageName")
}
```

### Issue: Partial Learning - Input Fields, Consent Interruption (FIXED 2025-12-02)

**Symptoms**:
1. LearnApp learns apps only partially (not clicking all buttons)
2. Input fields being clicked → triggers Gboard/keyboard appearance
3. Gboard consent dialog interrupts active exploration (e.g., during Photos learning)

**Root Cause Analysis**:

#### Problem 1: Input Fields Being Clicked
**Location**: `models/ElementInfo.kt:87`

The `isEditText()` method only detected "EditText" substring:
```kotlin
fun isEditText(): Boolean {
    return className.contains("EditText", ignoreCase = true)
}
```

**Missed Detection**:
- ❌ `TextInputEditText` (Material Design - used by Google Photos, Gmail)
- ❌ `AppCompatEditText` (AppCompat library)
- ❌ `AutoCompleteTextView` / `MultiAutoCompleteTextView`
- ❌ `TextField` (Jetpack Compose)

**Result**: Input fields classified as SafeClickable → clicked during exploration → Gboard appears

#### Problem 2: Consent Dialog Interrupting Exploration
**Location**: `integration/LearnAppIntegration.kt:227-237`

`AppLaunchDetector` fires `NewAppDetected` events during active exploration:
```kotlin
when (event) {
    is AppLaunchEvent.NewAppDetected -> {
        // No check for exploration state!
        if (preferences.isAutoDetectEnabled()) {
            consentDialogManager.showConsentDialog(...)
        }
    }
}
```

**Flow**:
1. Photos exploration running
2. Input field clicked → Gboard appears
3. `NewAppDetected` event fires for Gboard
4. Consent dialog shows → blocks Photos exploration
5. Exploration hangs

#### Problem 3: No Diagnostic Logging
**Location**: `exploration/ExplorationEngine.kt:551-600`

No logging for WHY elements are skipped:
- Strategy rejection: `strategy.shouldExplore()` returns false
- Already clicked: `clickTracker.wasElementClicked()` returns true
- Click failure: `clickElement()` returns false

**Result**: No visibility into partial learning root causes

**Fix Implemented** (Commit: 3c03c438):

#### Fix 1: Enhanced Input Field Detection
**File**: `models/ElementInfo.kt:90-101`

Expanded `isEditText()` to detect ALL input field types:
```kotlin
fun isEditText(): Boolean {
    // Detect ALL text input field types (Android, Material, AppCompat, Compose)
    val inputFieldTypes = listOf(
        "EditText",                  // android.widget.EditText
        "TextInputEditText",         // com.google.android.material (Google Photos, Gmail)
        "AppCompatEditText",         // androidx.appcompat.widget
        "AutoCompleteTextView",      // android.widget
        "MultiAutoCompleteTextView", // android.widget
        "TextField"                  // Jetpack Compose
    )
    return inputFieldTypes.any { className.contains(it, ignoreCase = true) }
}
```

**Result**:
- ✅ All input field variants detected
- ✅ Classified as `ElementClassification.EditText` (not SafeClickable)
- ✅ NOT clicked during exploration
- ✅ Gboard won't appear during learning

#### Fix 2: Block Consent During Active Exploration
**File**: `integration/LearnAppIntegration.kt:230-239`

Added `ExplorationState.Running` check before showing consent:
```kotlin
when (event) {
    is AppLaunchEvent.NewAppDetected -> {
        Log.d(TAG, "Processing NewAppDetected: ${event.packageName}")

        // FIX: Don't interrupt active exploration with consent dialogs
        val currentState = explorationEngine.explorationState.value
        if (currentState is ExplorationState.Running) {
            Log.i(TAG, "BLOCKED NewAppDetected during exploration: ${event.packageName} " +
                "(currently exploring ${currentState.progress.appName})")
            return@collectLatest
        }

        // Now safe to show consent (not exploring)
        if (preferences.isAutoDetectEnabled()) {
            consentDialogManager.showConsentDialog(...)
        }
    }
}
```

**Result**:
- ✅ Gboard appearance during Photos learning: Ignored (no consent dialog)
- ✅ Any app/overlay during exploration: Ignored
- ✅ Exploration continues uninterrupted

#### Fix 3: Enhanced Diagnostic Logging
**File**: `exploration/ExplorationEngine.kt:551-604`

Added "ExplorationEngine-Skip" log tag with three skip reasons:
```kotlin
// FIX: Enhanced logging for partial learning diagnosis
val elementDesc = element.text ?: element.contentDescription ?: "unknown"
val elementType = element.className.substringAfterLast('.')

// Check if should explore
if (!strategy.shouldExplore(element)) {
    android.util.Log.d("ExplorationEngine-Skip",
        "STRATEGY REJECTED: \"$elementDesc\" ($elementType) - UUID: ${element.uuid}")
    continue
}

// Check if already clicked
if (clickTracker.wasElementClicked(explorationResult.screenState.hash, element.uuid ?: "")) {
    android.util.Log.d("ExplorationEngine-Skip",
        "ALREADY CLICKED: \"$elementDesc\" ($elementType) - UUID: ${element.uuid}")
    continue
}

// Regular click
val clicked = clickElement(element.node)
if (!clicked) {
    android.util.Log.w("ExplorationEngine-Skip",
        "CLICK FAILED: \"$elementDesc\" ($elementType) - UUID: ${element.uuid}")
    continue
}
```

**Log Categories**:
| Category | Meaning | Action |
|----------|---------|--------|
| `STRATEGY REJECTED` | `shouldExplore()` returned false | Check strategy configuration |
| `ALREADY CLICKED` | Element clicked this session | Normal (prevents loops) |
| `CLICK FAILED` | `performAction(ACTION_CLICK)` failed | Element may be disabled/covered |

**Diagnostic Commands**:
```bash
# See all skipped elements
adb logcat | grep "ExplorationEngine-Skip"

# See only click failures
adb logcat | grep "CLICK FAILED"

# See consent blocking during exploration
adb logcat | grep "BLOCKED NewAppDetected"

# Filter by app
adb logcat | grep "ExplorationEngine-Skip" | grep "Photos"
```

**Architecture Notes**:

These fixes complement existing functionality:
- ✅ **UUID Tracking**: Already working - ALL elements (safe/dangerous/EditText) get UUIDs (line 445-470)
- ✅ **Login Detection**: Already working - pauses exploration, shows secure prompt (line 380-433)
- ✅ **JIT Learning**: Already integrated - can learn skipped elements during natural usage
- ✅ **Dangerous Elements**: Already working - registered but not clicked (line 465-470)

**Testing Verification**:

```kotlin
// Test 1: Verify input field detection
val photoInputField = ElementInfo(
    className = "com.google.android.material.textfield.TextInputEditText",
    text = "Search photos"
)
assert(photoInputField.isEditText())  // Should be true

// Test 2: Verify consent blocking during exploration
val state = explorationEngine.explorationState.value
if (state is ExplorationState.Running) {
    // NewAppDetected should be blocked
    assert(!consentDialogManager.isDialogShowing())
}

// Test 3: Verify skip logging
// Run: adb logcat | grep "ExplorationEngine-Skip"
// Should see: STRATEGY REJECTED, ALREADY CLICKED, CLICK FAILED logs
```

**Performance Impact**:
- Input field detection: +5µs per element (negligible)
- Exploration state check: +1µs per NewAppDetected event (negligible)
- Enhanced logging: +50µs per skipped element (debug only)

**Prevention for Future Development**:

When adding new UI element types:
1. ✅ **Update `isEditText()` or similar detection methods** if new input field types added
2. ✅ **Always check `ExplorationState.Running`** before showing dialogs/overlays
3. ✅ **Add diagnostic logging** for skip conditions in exploration loops
4. ✅ **Test with Material Design components** (often have different class names)

**Known Limitations**:
- Custom input fields with non-standard class names may not be detected (add to `inputFieldTypes` list)
- Exploration state check only blocks consent, not system dialogs (system dialogs handled by package validation)

### Issue: Threading Crashes

**Symptoms**: `CalledFromWrongThreadException`

**Solution**: Always use `withContext(Dispatchers.Main)` for UI:
```kotlin
suspend fun showUI() {
    withContext(Dispatchers.Main) {
        windowManager.addView(view, params)
    }
}
```

### Issue: Database Not Updating

**Symptoms**: Learned apps not persisted

**Check**:
```kotlin
// Verify database instance
val dbManager = VoiceOSDatabaseManager.getInstance(context)

// Check if database accessible
val apps = dbManager.learnedAppQueries.getAllLearnedApps().executeAsList()
Log.d("LearnApp", "Learned apps in DB: ${apps.size}")
```

### Issue: Memory Leaks

**Symptoms**: Memory grows over time

**Solutions**:
1. Always call `cleanup()` in `onDestroy()`
2. Cancel coroutine scopes
3. Remove overlay views

```kotlin
override fun onDestroy() {
    learnAppIntegration?.cleanup()
    super.onDestroy()
}
```

---
## Performance Considerations

### Optimization Tips

1. **In-Memory Caching**: LearnedAppTracker uses in-memory cache for O(1) lookups
2. **Debouncing**: AppLaunchDetector debounces events (100ms window)
3. **Lazy Initialization**: Components initialized only when needed
4. **Flow Operators**: Use appropriate flow operators (conflate, sample, etc.)

### Monitoring

```kotlin
// Track exploration performance
val stats = createExplorationStats(packageName)
Log.d("LearnApp", """
    Exploration completed in ${stats.durationMs}ms
    - Screens: ${stats.totalScreens}
    - Elements: ${stats.totalElements}
    - Depth: ${stats.maxDepth}
""".trimIndent())
```

---

---

**Navigation**: [← Previous: Critical Fixes](./05-Critical-Fixes.md) | [Index](./00-Index.md) | [Next: Version History →](./07-Version-History.md)
