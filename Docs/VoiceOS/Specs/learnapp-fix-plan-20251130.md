# LearnApp Fix Plan

**Date:** 2025-11-30
**Analysis:** `specs/learnapp-analysis-20251130.md`
**Priority:** CRITICAL - Blocking consent dialog functionality

---

## Fix Order (Dependency-Based)

| Phase | File | Fix | Blocking |
|-------|------|-----|----------|
| 1 | VoiceOSService.kt | Init race condition | All functionality |
| 2 | VoiceOSService.kt | Add @Volatile | Thread safety |
| 3 | ConsentDialog.kt | WindowManager flags | Button clicks |
| 4 | AppLaunchDetector.kt | Remove double debounce | Event detection |
| 5 | LearnAppIntegration.kt | Error handling | Silent failures |
| 6 | LearnAppIntegration.kt | Fix dispatcher | UI operations |
| 7 | LearnAppIntegration.kt | Cleanup scope.cancel() | Memory leak |

---

## Phase 1: Fix Initialization Race Condition

**File:** `VoiceOSService.kt`
**Lines:** 664-683

### Current Code (WRONG)
```kotlin
if (!learnAppInitialized) {
    synchronized(this) {
        if (!learnAppInitialized) {
            learnAppInitialized = true  // ← SET TOO EARLY
            serviceScope.launch {
                initializeLearnAppIntegration()
            }
        }
    }
}
```

### Fixed Code
```kotlin
if (!learnAppInitialized) {
    synchronized(this) {
        if (!learnAppInitialized) {
            Log.i(TAG, "LEARNAPP_DEBUG: Starting initialization")
            serviceScope.launch {
                try {
                    initializeLearnAppIntegration()
                    learnAppInitialized = true  // ← MOVE HERE
                    Log.i(TAG, "LEARNAPP_DEBUG: Initialization complete, flag set")
                } catch (e: Exception) {
                    Log.e(TAG, "LEARNAPP_DEBUG: Initialization failed", e)
                    learnAppInitialized = false  // Allow retry
                }
            }
        }
    }
}
```

**Test:** Logs should show "Initialization complete" BEFORE any event forwarding.

---

## Phase 2: Add @Volatile Annotation

**File:** `VoiceOSService.kt`
**Line:** 217

### Current Code
```kotlin
private var learnAppIntegration: LearnAppIntegration? = null
```

### Fixed Code
```kotlin
@Volatile
private var learnAppIntegration: LearnAppIntegration? = null
```

**Test:** Thread visibility guaranteed across coroutine and event handler.

---

## Phase 3: Fix WindowManager Flags

**File:** `ConsentDialog.kt`
**Lines:** 186-192

### Current Code (WRONG)
```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
    PixelFormat.TRANSLUCENT
)
```

### Fixed Code
```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
    PixelFormat.TRANSLUCENT
)
```

**Test:** Tap Yes/No/Skip buttons - they should respond.

---

## Phase 4: Remove Double Debouncing

**File:** `AppLaunchDetector.kt`
**Lines:** 130-136

### Current Code (REMOVE)
```kotlin
// DELETE these lines - Layer 2 handles debouncing
if (packageName == lastProcessedPackage &&
    (now - lastEventTimestamp) < DEBOUNCE_WINDOW_MS) {
    Log.v(TAG, "Debouncing event for $packageName")
    return
}

lastEventTimestamp = now
lastProcessedPackage = packageName
```

### Fixed Code
```kotlin
// Layer 2 (Flow debounce) handles this - remove Layer 1
// Just log and proceed
Log.d(TAG, "Processing event for $packageName")
```

**Also remove:**
- Line 90-95: `lastEventTimestamp` and `lastProcessedPackage` variables
- Line 286-289: `DEBOUNCE_WINDOW_MS` constant

**Test:** Launch apps quickly in succession - all should trigger detection.

---

## Phase 5: Add Error Handling to Flow Collectors

**File:** `LearnAppIntegration.kt`
**Lines:** 191-258

### Current Code (NO ERROR HANDLING)
```kotlin
scope.launch {
    appLaunchDetector.appLaunchEvents
        .debounce(500.milliseconds)
        .collect { event ->
            // No try-catch
            consentDialogManager.showConsentDialog(...)
        }
}
```

### Fixed Code
```kotlin
scope.launch {
    try {
        appLaunchDetector.appLaunchEvents
            .debounce(500.milliseconds)
            .catch { e ->
                Log.e(TAG, "Flow error in appLaunchEvents", e)
            }
            .collect { event ->
                try {
                    when (event) {
                        is AppLaunchEvent.NewAppDetected -> {
                            if (preferences.isAutoDetectEnabled()) {
                                withContext(Dispatchers.Main) {
                                    consentDialogManager.showConsentDialog(
                                        packageName = event.packageName,
                                        appName = event.appName
                                    )
                                }
                            }
                        }
                        else -> Log.d(TAG, "Event: ${event.javaClass.simpleName}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling event: $event", e)
                }
            }
    } catch (e: Exception) {
        Log.e(TAG, "Flow collector crashed", e)
    }
}
```

**Test:** Force an exception - should log error, not crash silently.

---

## Phase 6: Fix Dispatcher for UI Operations

**File:** `LearnAppIntegration.kt`
**Line:** 211

### Current Code
```kotlin
consentDialogManager.showConsentDialog(...)  // Wrong thread
```

### Fixed Code (in Phase 5)
```kotlin
withContext(Dispatchers.Main) {
    consentDialogManager.showConsentDialog(...)
}
```

**Note:** Already included in Phase 5 fix.

---

## Phase 7: Add scope.cancel() to cleanup()

**File:** `LearnAppIntegration.kt`
**Lines:** 649-654

### Current Code
```kotlin
fun cleanup() {
    hideLoginPromptOverlay()
    consentDialogManager.cleanup()
    progressOverlayManager.cleanup()
    justInTimeLearner.destroy()
    // MISSING: scope.cancel()
}
```

### Fixed Code
```kotlin
fun cleanup() {
    Log.d(TAG, "Cleaning up LearnAppIntegration")
    scope.cancel()  // ← ADD THIS
    hideLoginPromptOverlay()
    consentDialogManager.cleanup()
    progressOverlayManager.cleanup()
    justInTimeLearner.destroy()
}
```

**Test:** No coroutine leak warnings in logcat after cleanup.

---

## Verification Checklist

After all fixes applied:

### Logcat Filter
```bash
adb logcat | grep "LEARNAPP_DEBUG\|ConsentDialog\|AppLaunchDetector"
```

### Expected Log Sequence
```
LEARNAPP_DEBUG: Starting initialization
LEARNAPP_DEBUG: initializeLearnAppIntegration() CALLED
LEARNAPP_DEBUG: Initialization complete, flag set
LEARNAPP_DEBUG: learnAppInitialized=true, learnAppIntegration=EXISTS
AppLaunchDetector: Processing event for com.example.app
AppLaunchDetector: NEW APP DETECTED: com.example.app
LearnAppIntegration: showing consent dialog for com.example.app
ConsentDialog: show() called for appName: Example App
ConsentDialog: WindowManager.addView() succeeded
```

### Manual Test
1. Install APK on device
2. Enable VoiceOS accessibility service
3. Launch any third-party app (e.g., Calculator)
4. Consent dialog should appear
5. Tap "Skip" button - should respond
6. Check database has entry

---

## Build & Deploy

```bash
# Build
./gradlew assembleDebug

# Publish (MANDATORY per CLAUDE.md)
./scripts/publish-build.sh

# Install
adb install -r /Volumes/M-Drive/Coding/builds/VoiceOS/debug/voiceos-debug-*.apk

# Monitor
adb logcat | grep "LEARNAPP_DEBUG"
```

---

## Rollback Plan

If fixes cause regressions:

1. Git revert to pre-fix commit
2. Rebuild and reinstall
3. Document which fix caused issue
4. Address individually

---

## Success Criteria

| Criteria | Metric |
|----------|--------|
| Dialog appears | Within 2 seconds of app launch |
| Buttons clickable | All 3 buttons respond |
| No crashes | Zero FATAL in logcat |
| Events not dropped | All app launches detected |
| Database populated | Entries after button click |

---

**Status:** Ready for implementation
**Estimated Fixes:** 7 phases
**Risk:** LOW - Each fix is isolated and testable
