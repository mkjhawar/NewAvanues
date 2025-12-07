# VOS4 LearnApp Integration Guide
## Step-by-Step Wiring Instructions

**Date**: 2025-10-08
**Status**: ⚠️ **NOT YET WIRED** ⚠️
**Integration Adapter**: `VOS4LearnAppIntegration.kt` (CREATED, NOT WIRED)

---

## ⚠️ IMPORTANT NOTICE

**This guide documents HOW to wire LearnApp into VOS4, but the wiring has NOT been done yet.**

All LearnApp files are created and ready, but they are NOT connected to VOS4 yet.
This was done intentionally per user request: "keep the wiring for when i can oversee it, just create the files but do not wire, document what needs to be done"

---

## What's Been Done

✅ **Phase 1-7: Complete Implementation** - All LearnApp files created (37 files, ~7,400 lines)
✅ **Phase 8: Integration Adapter** - `VOS4LearnAppIntegration.kt` created (NOT wired)
✅ **This Guide** - Detailed wiring instructions

---

## What Needs to Be Done

To wire LearnApp into VOS4, you need to:

1. Add LearnApp dependencies to VOS4's build.gradle
2. Initialize `VOS4LearnAppIntegration` in VOS4 Application
3. Wire into VOS4 AccessibilityService
4. Add overlay permissions
5. Test with real app

**Estimated Time**: 30-60 minutes

---

## Step 1: Add LearnApp to Build Configuration

### File: `vos4/settings.gradle.kts`

Add LearnApp module:

```kotlin
include(":modules:libraries:UUIDCreator")
include(":modules:libraries:LearnApp")  // ADD THIS LINE
```

### File: `vos4/modules/app/build.gradle.kts`

Add LearnApp dependency:

```kotlin
dependencies {
    // Existing dependencies...
    implementation(project(":modules:libraries:UUIDCreator"))
    implementation(project(":modules:libraries:LearnApp"))  // ADD THIS LINE
}
```

---

## Step 2: Initialize Integration in VOS4 Application

### File: `vos4/modules/app/src/main/java/com/augmentalis/vos4/VOS4Application.kt`

Add integration initialization:

```kotlin
package com.augmentalis.vos4

import android.app.Application
import com.augmentalis.learnapp.integration.VOS4LearnAppIntegration  // ADD THIS IMPORT

class VOS4Application : Application() {

    // ADD THIS PROPERTY
    lateinit var learnAppIntegration: VOS4LearnAppIntegration
        private set

    override fun onCreate() {
        super.onCreate()

        // Existing initialization...
        // uuidIntegration = VOS4UUIDIntegration.initialize(this)

        // ADD THIS (requires accessibility service instance)
        // NOTE: You may need to defer this until AccessibilityService is created
        // learnAppIntegration = VOS4LearnAppIntegration.initialize(this, accessibilityService)
    }
}
```

**IMPORTANT**: The initialization requires an `AccessibilityService` instance, which may not be available in `Application.onCreate()`. You have two options:

**Option A**: Initialize lazily when accessibility service starts:
```kotlin
class VOS4Application : Application() {
    private var _learnAppIntegration: VOS4LearnAppIntegration? = null

    val learnAppIntegration: VOS4LearnAppIntegration?
        get() = _learnAppIntegration

    fun initializeLearnApp(accessibilityService: AccessibilityService) {
        if (_learnAppIntegration == null) {
            _learnAppIntegration = VOS4LearnAppIntegration.initialize(
                this,
                accessibilityService
            )
        }
    }
}
```

**Option B**: Pass service to integration later (requires code modification).

---

## Step 3: Wire into VOS4 AccessibilityService

### File: `vos4/modules/app/src/main/java/com/augmentalis/vos4/accessibility/VOS4AccessibilityService.kt`

Wire LearnApp into accessibility events:

```kotlin
package com.augmentalis.vos4.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.learnapp.integration.VOS4LearnAppIntegration  // ADD THIS IMPORT

class VOS4AccessibilityService : AccessibilityService() {

    // ADD THIS PROPERTY
    private lateinit var learnAppIntegration: VOS4LearnAppIntegration

    override fun onCreate() {
        super.onCreate()

        // ADD THIS - Initialize LearnApp integration
        val app = application as VOS4Application
        app.initializeLearnApp(this)  // If using Option A
        learnAppIntegration = app.learnAppIntegration!!
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Existing VOS4 event handling...
        // handleVOS4Event(event)

        // ADD THIS - Forward event to LearnApp
        learnAppIntegration.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        // Existing cleanup...
    }

    override fun onDestroy() {
        super.onDestroy()

        // ADD THIS - Cleanup LearnApp
        learnAppIntegration.cleanup()
    }
}
```

---

## Step 4: Add Permissions

### File: `vos4/modules/app/src/main/AndroidManifest.xml`

Add overlay permission (for consent dialog and progress overlay):

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.augmentalis.vos4">

    <!-- ADD THIS PERMISSION -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <!-- Existing permissions... -->
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

    <application
        ...>
        <!-- Existing components... -->
    </application>
</manifest>
```

---

## Step 5: Request Overlay Permission at Runtime

### File: `vos4/modules/app/src/main/java/com/augmentalis/vos4/MainActivity.kt`

Add code to request overlay permission:

```kotlin
package com.augmentalis.vos4

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // ADD THIS LAUNCHER
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted
                showToast("Overlay permission granted")
            } else {
                // Permission denied
                showToast("Overlay permission required for LearnApp")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ADD THIS - Check overlay permission
        checkOverlayPermission()
    }

    // ADD THIS METHOD
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Request permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }
    }
}
```

---

## Step 6: (Optional) Add Voice Commands

### File: `vos4/modules/app/src/main/java/com/augmentalis/vos4/commands/VOS4CommandHandler.kt`

Add voice commands for controlling LearnApp:

```kotlin
package com.augmentalis.vos4.commands

import com.augmentalis.vos4.VOS4Application

class VOS4CommandHandler(private val app: VOS4Application) {

    fun handleCommand(command: String): Boolean {
        val learnApp = app.learnAppIntegration ?: return false

        return when (command.lowercase()) {
            "pause learning" -> {
                learnApp.pauseExploration()
                true
            }

            "resume learning" -> {
                learnApp.resumeExploration()
                true
            }

            "stop learning" -> {
                learnApp.stopExploration()
                true
            }

            else -> false
        }
    }
}
```

---

## Configuration Options

### Exploration Strategy

You can customize the exploration strategy:

```kotlin
// In VOS4Application or AccessibilityService
val strategy = when (userPreference) {
    "dfs" -> DFSExplorationStrategy()
    "bfs" -> BFSExplorationStrategy()
    "prioritized" -> PrioritizedExplorationStrategy()
    else -> DFSExplorationStrategy()
}

// Pass to ExplorationEngine constructor
val engine = ExplorationEngine(
    accessibilityService = this,
    uuidCreator = uuidCreator,
    thirdPartyGenerator = thirdPartyGenerator,
    aliasManager = aliasManager,
    strategy = strategy  // Custom strategy
)
```

### Exploration Limits

Customize limits by extending `ExplorationStrategy`:

```kotlin
class CustomExplorationStrategy : ExplorationStrategy {
    override fun getMaxDepth(): Int = 100  // Increase from default 50
    override fun getMaxExplorationTime(): Long = 60 * 60 * 1000L  // 1 hour
}
```

---

## Testing

### Manual Testing Checklist

After wiring, test the following:

- [ ] Launch VOS4
- [ ] Enable VOS4 Accessibility Service in Android Settings
- [ ] Grant overlay permission
- [ ] Launch an unlearned app (e.g., Instagram)
- [ ] Consent dialog appears: "Do you want VoiceOS to Learn Instagram?"
- [ ] Tap "Yes"
- [ ] Progress overlay appears showing exploration progress
- [ ] Elements are being clicked automatically
- [ ] Progress updates in real-time
- [ ] Can pause exploration (tap "Pause" or say "pause learning")
- [ ] Can resume exploration
- [ ] Can stop exploration (tap "Stop" or say "stop learning")
- [ ] Exploration completes successfully
- [ ] Re-launch same app → no consent dialog (already learned)
- [ ] Voice commands work: "Open Instagram", "Tap Instagram like button"

### Dangerous Element Testing

Test that dangerous elements are skipped:

- [ ] Launch app with "Delete Account" button
- [ ] Verify button is NOT clicked during exploration
- [ ] Check logs: "Skipped dangerous element: Delete Account"

### Login Screen Testing

Test login screen detection:

- [ ] Launch app that requires login
- [ ] Exploration pauses at login screen
- [ ] Overlay shows: "Login screen detected. Please sign in manually."
- [ ] Manually login
- [ ] Exploration resumes after login

### Scroll Testing

Test scrolling to find offscreen elements:

- [ ] Launch app with long scrollable list (e.g., Instagram feed)
- [ ] Verify scrolling occurs during exploration
- [ ] Verify elements below fold are discovered

---

## Troubleshooting

### Consent Dialog Not Showing

**Problem**: Consent dialog doesn't appear when launching new app.

**Solutions**:
1. Check overlay permission is granted
2. Check `AppLaunchDetector.onAccessibilityEvent()` is being called
3. Check app is not filtered as system app
4. Check app is not already marked as learned

**Debug**:
```kotlin
// Add logging to AppLaunchDetector
Log.d("LearnApp", "New app detected: $packageName")
```

### Exploration Not Starting

**Problem**: Click "Yes" on consent dialog but exploration doesn't start.

**Solutions**:
1. Check `ExplorationEngine.startExploration()` is being called
2. Check accessibility service has `getRootInActiveWindow()` permission
3. Check no exceptions in logs

**Debug**:
```kotlin
// Add logging to ExplorationEngine
Log.d("LearnApp", "Starting exploration: $packageName")
```

### Elements Not Being Clicked

**Problem**: Progress overlay shows but elements aren't being clicked.

**Solutions**:
1. Check `node.performAction(ACTION_CLICK)` returns true
2. Check elements are classified as `SafeClickable`
3. Check elements are not disabled

**Debug**:
```kotlin
// Add logging to ElementClassifier
Log.d("LearnApp", "Classification: ${element.text} -> $classification")
```

### Memory Issues

**Problem**: App crashes with OutOfMemoryError during exploration.

**Solutions**:
1. Ensure `AccessibilityNodeInfo.recycle()` is called for all nodes
2. Reduce max depth limit
3. Reduce max scroll attempts

**Fix**:
```kotlin
// Always use try-finally for recycling
val node = rootNode.getChild(i)
try {
    // Process node
} finally {
    node?.recycle()
}
```

---

## Performance Optimization

### Reduce Context Usage

If exploration is too slow:

1. **Reduce max depth**: Change `strategy.getMaxDepth()` from 50 to 25
2. **Reduce scroll attempts**: Change `MAX_SCROLL_ATTEMPTS` from 50 to 25
3. **Skip non-essential elements**: Filter out decorative elements

### Improve Speed

If exploration is too fast (missing elements):

1. **Increase wait times**: Change `delay(1000)` to `delay(2000)`
2. **Increase scroll delay**: Change `delay(300)` to `delay(500)`
3. **Add screen transition detection**: Wait for screen hash to change

---

## Database Queries

### Check Learned Apps

```kotlin
val repository = LearnAppRepository(database.learnAppDao())

// Get all learned apps
val learnedApps = repository.getAllLearnedApps()

// Check if specific app is learned
val isLearned = repository.isAppLearned("com.instagram.android")

// Get navigation graph
val graph = repository.getNavigationGraph("com.instagram.android")
```

### Clear Learned Data

```kotlin
// Delete specific app
repository.deleteLearnedApp("com.instagram.android")

// Clear all learned apps
val allApps = repository.getAllLearnedApps()
allApps.forEach { app ->
    repository.deleteLearnedApp(app.packageName)
}
```

---

## Known Limitations

1. **No Actual Wiring**: Integration adapter exists but is NOT wired to VOS4 yet
2. **No Migration Scripts**: Database v1, no migration from UUIDCreator v1→v2
3. **No Integration Tests**: No tests with real Android device yet
4. **No Gesture Support**: Only clicks, no swipes or long presses
5. **No Multi-Window Support**: Assumes single-window apps
6. **No Background Exploration**: Exploration stops if user switches apps

---

## Future Enhancements

These are NOT implemented but could be added:

1. **Smart Exploration**: ML model to predict important screens
2. **Gesture Support**: Swipe gestures beyond scrolling
3. **Multi-Language**: Support for non-English apps
4. **Cloud Sync**: Share learned apps across devices
5. **Visual Recognition**: OCR for screen understanding
6. **Accessibility Audit**: Report accessibility issues
7. **Custom Rules**: User-defined dangerous element patterns
8. **Exploration Replay**: Record and replay exploration sessions

---

## Summary

### What You Need to Do

1. ✅ **Read this guide** - You're doing it!
2. ⬜ **Add LearnApp to build config** (5 min)
3. ⬜ **Initialize in VOS4Application** (10 min)
4. ⬜ **Wire into AccessibilityService** (10 min)
5. ⬜ **Add overlay permission** (5 min)
6. ⬜ **Request permission at runtime** (10 min)
7. ⬜ **Test with real app** (15 min)
8. ⬜ **(Optional) Add voice commands** (10 min)

**Total Time**: 30-60 minutes

### Files to Modify

- `settings.gradle.kts`
- `build.gradle.kts`
- `VOS4Application.kt`
- `VOS4AccessibilityService.kt`
- `AndroidManifest.xml`
- `MainActivity.kt`

### Files Created (Ready to Use)

- `VOS4LearnAppIntegration.kt` (Integration adapter)
- All LearnApp implementation files (37 files)
- This integration guide

---

## Need Help?

If you encounter issues during wiring:

1. Check this guide's Troubleshooting section
2. Review LearnApp source files for implementation details
3. Check Android logs for exceptions
4. Verify all permissions are granted

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)

**Integration Guide Created**: 2025-10-08
**Status**: ⚠️ NOT YET WIRED ⚠️
**Ready for Wiring**: YES ✅
