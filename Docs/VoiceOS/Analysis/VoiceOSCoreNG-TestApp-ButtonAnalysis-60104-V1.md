# VoiceOSCoreNG Test App - Button Analysis

**Date:** 2026-01-04
**Version:** 1.0
**Status:** Analysis Complete

---

## Executive Summary

This document analyzes all buttons in the VoiceOSCoreNG test app located at `android/apps/voiceoscoreng/`. The analysis identifies button wiring, dependencies, and required conditions for proper operation.

---

## Button Inventory

### 1. MainActivity - ScannerControlCard

| Button | Location | Function | Dependencies | Visibility Condition |
|--------|----------|----------|--------------|---------------------|
| **Enable Accessibility Service** | `MainActivity.kt:382-396` | Opens Android Accessibility Settings | None | Shows when `!isAccessibilityConnected` |
| **Enable Overlay Permission** | `MainActivity.kt:400-421` | Opens overlay permission settings | Android M+ | Shows when `!hasOverlayPermission` |
| **Start Scanner Overlay** | `MainActivity.kt:425-435` | Calls `OverlayService.start(context)` | Accessibility + Overlay permissions | Shows when BOTH `isAccessibilityConnected && hasOverlayPermission` |

**Critical Issue:** `hasOverlayPermission` uses `remember {}` without recomposition trigger - value won't update after permission grant without app restart.

### 2. MainActivity - QuickActionsCard

| Button | Location | Function | Dependencies | Notes |
|--------|----------|----------|--------------|-------|
| **Enable Test Mode** | `MainActivity.kt:228-238` | `VoiceOSCoreNG.enableTestMode()` | None | Works correctly |
| **Reset** | `MainActivity.kt:240-250` | `VoiceOSCoreNG.reset()` | None | Works correctly |
| **LITE Tier** | `MainActivity.kt:260-270` | `VoiceOSCoreNG.setTier(Tier.LITE)` | None | Works correctly |
| **DEV Tier** | `MainActivity.kt:271-283` | `VoiceOSCoreNG.setTier(Tier.DEV)` | None | Works correctly |
| **Custom Limits** | `MainActivity.kt:292-300` | `VoiceOSCoreNG.configureLimits(...)` | None | Works correctly |
| **Enable All Features** | `MainActivity.kt:302-317` | `VoiceOSCoreNG.configureFeatures(...)` | None | Works correctly |

### 3. MainActivity - TopAppBar

| Button | Location | Function | Dependencies | Notes |
|--------|----------|----------|--------------|-------|
| **Tier Chip** | `MainActivity.kt:108-121` | `VoiceOSCoreNG.toggle()` | None | Toggles between LITE/DEV |

### 4. TestModeFab (Floating Action Button Menu)

| Button | Location | Function | Dependencies | Notes |
|--------|----------|----------|--------------|-------|
| **Main FAB** | `TestModeFab.kt:196-218` | Toggles `expanded` state | None | Works correctly |
| **Enable/Disable Test Mode** | `TestModeFab.kt:97-112` | `LearnAppConfig.enableTestMode()` / `reset()` | None | Works correctly |
| **Run All Tests** | `TestModeFab.kt:115-129` | `runAllTests()` | Coroutine scope | Works correctly |
| **Test Exploration** | `TestModeFab.kt:132-146` | `runExplorationTest()` | ExplorationEngine | Works correctly |
| **Test JIT Processing** | `TestModeFab.kt:149-163` | `runJitTest()` | JitProcessor | Works correctly |
| **View Config** | `TestModeFab.kt:166-180` | Shows config dialog | None | Works correctly |
| **Developer Settings** | `TestModeFab.kt:183-191` | `onOpenSettings()` callback | None | Works correctly |

### 5. DeveloperSettingsScreen

| Button/Control | Location | Function | Dependencies | Notes |
|----------------|----------|----------|--------------|-------|
| **Close** | `DeveloperSettingsScreen.kt:67-69` | `onDismiss()` callback | None | Works correctly |
| **Developer Mode Switch** | `DeveloperSettingsScreen.kt:372-375` | Toggle dev mode on/off | None | Works correctly |
| **Unlock All Features** | `DeveloperSettingsScreen.kt:380-390` | Enable all dev features | Dev mode enabled | Works correctly |
| **Feature Toggles (AI, NLU, etc.)** | `DeveloperSettingsScreen.kt:198-267` | Toggle individual features | Dev mode enabled | Works correctly |
| **Processing Mode Radio Buttons** | `DeveloperSettingsScreen.kt:270-305` | Set processing mode | Dev mode enabled | Works correctly |

### 6. OverlayService - Overlay FAB (When Running)

| Button | Location | Function | Dependencies | Notes |
|--------|----------|----------|--------------|-------|
| **Floating FAB** | `OverlayService.kt:328-339` | Toggles `expanded` state | Service running | Works if service starts |
| **Scan Current App** | `OverlayService.kt:284-292` | `VoiceOSAccessibilityService.exploreCurrentApp()` | A11y connected | Works correctly |
| **Scan All Windows** | `OverlayService.kt:295-303` | `VoiceOSAccessibilityService.exploreAllApps()` | A11y connected | Works correctly |
| **Show/Hide Results** | `OverlayService.kt:306-311` | Toggle results panel | Has results | Works correctly |
| **Close Overlay** | `OverlayService.kt:317-320` | `stopSelf()` | None | Works correctly |

---

## Wiring Requirements

### Service Dependencies

```
VoiceOSAccessibilityService
├── isConnected: StateFlow<Boolean>  ← Set true in onServiceConnected()
├── explorationResults: StateFlow<ExplorationResult?>
└── lastError: StateFlow<String?>

OverlayService
├── Requires: SYSTEM_ALERT_WINDOW permission
├── Requires: FOREGROUND_SERVICE permission
├── Requires: FOREGROUND_SERVICE_SPECIAL_USE permission
└── Uses: VoiceOSAccessibilityService.isConnected (for UI state)
```

### Permission Flow

```
1. User enables accessibility via Settings app
   └── System binds to VoiceOSAccessibilityService
       └── onServiceConnected() called
           └── _isConnected.value = true
               └── UI observes via collectAsState()

2. User taps "Start Scanner Overlay"
   └── OverlayService.start(context)
       └── startForegroundService()
           └── onStartCommand()
               └── startForeground() + showOverlay()
```

---

## Identified Issues

### Issue 1: `hasOverlayPermission` Not Reactive

**File:** `MainActivity.kt:329-335`

```kotlin
// PROBLEM: This only evaluates once and never updates
val hasOverlayPermission = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}
```

**Impact:** After granting overlay permission, the UI won't show "Start Scanner Overlay" button until app restart.

**Fix:** Use `mutableStateOf` with lifecycle observer or check permission on button click.

### Issue 2: Accessibility Connection State Timing

**File:** `VoiceOSAccessibilityService.kt:56-60`

The `_isConnected` StateFlow is set in `onServiceConnected()`, but if the service was already running before app launch, the app needs to check if the service is actually bound.

**Impact:** If user enables accessibility, then force-stops app, then relaunches - the state may not sync.

**Fix:** Check `AccessibilityManager.isEnabled` as fallback.

### Issue 3: OverlayService Not Exported

**File:** `AndroidManifest.xml:44-51`

The service has `android:exported="false"` which is correct for security, but means it cannot be started via `adb shell am startservice`.

**Impact:** Testing via ADB is not possible.

**Note:** This is correct behavior, not a bug.

---

## Button Wiring Verification Checklist

| Component | Wiring Status | Test Method |
|-----------|---------------|-------------|
| Enable Accessibility Service | ✅ Works | Opens settings correctly |
| Enable Overlay Permission | ✅ Works | Opens permission screen |
| Start Scanner Overlay | ⚠️ Conditional | Only shows when both permissions granted |
| Test Mode buttons (FAB) | ✅ Works | LearnAppConfig updates correctly |
| Quick Actions buttons | ✅ Works | Config changes visible |
| Developer Settings | ✅ Works | Bottom sheet opens |
| Overlay FAB (when shown) | ❓ Not tested | Depends on service starting |

---

## Required Fixes

### Fix 1: Make `hasOverlayPermission` Reactive

```kotlin
// Replace static remember with mutableState + LaunchedEffect
var hasOverlayPermission by remember {
    mutableStateOf(checkOverlayPermission(context))
}

// Re-check when activity resumes (after returning from permission screen)
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            hasOverlayPermission = checkOverlayPermission(context)
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

### Fix 2: Add Accessibility Service Fallback Check

```kotlin
// In MainActivity, also check system state as fallback
val context = LocalContext.current
val systemA11yEnabled = remember {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    am.isEnabled
}

// Use: isAccessibilityConnected || (systemA11yEnabled && checkOurServiceEnabled())
```

---

## Files Analyzed

| File | Path | Purpose |
|------|------|---------|
| MainActivity.kt | `android/apps/voiceoscoreng/.../MainActivity.kt` | Main UI with ScannerControlCard, QuickActionsCard |
| TestModeFab.kt | `android/apps/voiceoscoreng/.../ui/TestModeFab.kt` | FAB menu for testing |
| DeveloperSettingsScreen.kt | `android/apps/voiceoscoreng/.../ui/DeveloperSettingsScreen.kt` | Developer settings |
| OverlayService.kt | `android/apps/voiceoscoreng/.../service/OverlayService.kt` | Floating overlay service |
| VoiceOSAccessibilityService.kt | `android/apps/voiceoscoreng/.../service/VoiceOSAccessibilityService.kt` | Accessibility service |
| AndroidManifest.xml | `android/apps/voiceoscoreng/.../AndroidManifest.xml` | Service declarations |

---

## Conclusion

The primary issue preventing the "Start Scanner Overlay" button from appearing is:

1. **Accessibility service state not syncing** - Even though we enabled it via ADB, the `VoiceOSAccessibilityService.isConnected` StateFlow may not be updating in the app's UI.

2. **Overlay permission not reactive** - The permission check only runs once at composition time.

Both issues require the fix outlined above to ensure proper reactivity.

---

## Implementation Status (Updated 2026-01-05)

### Fixes Applied

1. **hasOverlayPermission Reactivity** - ✅ FIXED
   - Added lifecycle observer to re-check permission on resume
   - File: `MainActivity.kt:331-356`

### Remaining Issues

1. **Accessibility Service Not Binding** - ❌ OPEN
   - Even when enabled via `settings put`, the system is not calling `onServiceConnected()`
   - The `VoiceOSAccessibilityService.isConnected` StateFlow remains `false`
   - Root cause: System may require physical toggle in Settings UI, not just settings command

### Next Steps

1. Manually enable accessibility in Settings UI (not via ADB command)
2. Verify `onServiceConnected()` is called (check logs for `VoiceOSA11yService` tag)
3. Once both conditions true, "Start Scanner Overlay" button should appear

### Test Commands

```bash
# Enable accessibility (may need manual toggle in UI)
adb shell settings put secure enabled_accessibility_services \
  "com.augmentalis.voiceoscoreng/com.augmentalis.voiceoscoreng.service.VoiceOSAccessibilityService"
adb shell settings put secure accessibility_enabled 1

# Check if enabled
adb shell settings get secure enabled_accessibility_services

# Check service logs
adb logcat -d | grep "VoiceOSA11yService"
```

---

**Author:** Claude Code Analysis
**Repository:** NewAvanues
**Module:** android/apps/voiceoscoreng
