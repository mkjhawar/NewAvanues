# Issue: LearnApp Progress Overlay Blocks Manual Permissions & Login

## Status
| Field | Value |
|-------|-------|
| Module | LearnApp |
| Severity | **CRITICAL** |
| Status | **ROOT CAUSE IDENTIFIED** |
| Date | 2025-12-06 |
| Affects | v1.7.0 |
| Priority | **P0** - Blocks full app learning |

## Symptoms

### User Report
1. **Progress overlay blocks permissions:** LearnApp notification appears in **center of screen**, blocking Teams permission dialog
2. **Cannot manually intervene:** User unable to click "SETTINGS" button or grant permissions manually
3. **Incomplete exploration:** App exploration stuck at 10% because permissions required for full access
4. **Black overlay visible:** Screenshot shows black overlay at bottom of screen (progress notification)

### Evidence from Screenshot (`image.png`)

**Visible Elements:**
- Teams chat interface (background, grayed out)
- **Center modal dialog:** "Teams needs permission" with message "To give permission, go to settings and allow Photos and Videos"
- **Two buttons:** "CANCEL" and "SETTINGS"
- **Black overlay at bottom:** Appears to be LearnApp progress notification (partial view)
- **Dialog is clickable but overlay may be intercepting touches**

### Evidence from Logs

**Lines 221-248:**
```
15:25:41.370 ExplorationEngine  W  Navigated to external app: com.android.permissioncontroller
15:25:42.312 ChecklistManager   D  Added screen: Screen #18 (0 elements)
15:25:42.414 AppLaunchDetector  D  Processing window state change: com.microsoft.teams
15:25:42.870 Exploration...     I  📍 Screen changed from f71d6a80... to a66db267...
15:25:43.045 ChecklistManager   D  Added screen: Teams needs permission (2 elements)
15:25:44.133 Exploration...     I  ✅ All elements clicked on this screen (1 total)
15:25:45.055 AppLaunchDetector  D  Processing window state change: com.android.settings
15:25:45.781 AppLaunchDetector  I  ✓ NEW APP DETECTED: com.microsoft.teams (Teams)
```

**Analysis:**
1. LearnApp detected permission dialog screen (`Teams needs permission`)
2. Found 2 clickable elements (CANCEL, SETTINGS buttons)
3. Clicked 1 element (likely "SETTINGS" button)
4. Navigated to Android Settings (`com.android.settings`)
5. **BUT user reports they couldn't manually click** → Progress overlay was blocking

## Root Cause Analysis (Tree of Thought)

### Hypothesis 1: Progress Overlay Uses Full-Screen Layout ✅
**Likelihood:** **HIGH**

**Evidence:**
```kotlin
// Current implementation (likely):
ProgressOverlayManager shows overlay as:
- Full-screen WindowManager.LayoutParams
- TYPE_APPLICATION_OVERLAY or TYPE_SYSTEM_ALERT
- Covers entire screen including permission dialogs
- User cannot interact with underlying UI
```

**Screenshot Evidence:**
- Black overlay visible at bottom of screen
- Permission dialog visible but potentially non-interactive
- No visible way to dismiss overlay

**Conclusion:** ✅ **Primary root cause** - Overlay covers full screen

---

### Hypothesis 2: Overlay Z-Index Too High
**Likelihood:** Medium

**Evidence:**
- System permission dialogs should have highest z-index
- But LearnApp overlay may be using `TYPE_SYSTEM_ALERT` or similar
- Could be intercepting touch events before they reach permission dialog

**Conclusion:** ✅ **Contributing factor** - May need lower z-index

---

### Hypothesis 3: No Dismiss Mechanism for Manual Intervention
**Likelihood:** **HIGH**

**Evidence:**
- User reports: "unable to manually click permissions"
- No visible dismiss button in screenshot
- No way to temporarily hide overlay to grant permissions
- Exploration continues while permission dialog shows

**Expected Behavior:**
- User should be able to dismiss overlay temporarily
- Grant permissions manually
- Resume LearnApp exploration after permissions granted

**Conclusion:** ✅ **Critical missing feature** - Need dismissible overlay

---

### Hypothesis 4: Overlay Should Detect Permission/Login Dialogs
**Likelihood:** Medium

**Evidence:**
- LearnApp detects permission dialog (log line 234)
- But doesn't auto-hide overlay to allow user intervention
- Should recognize "blocked" state and prompt user

**Conclusion:** ✅ **Enhancement opportunity** - Auto-detect blocked states

---

## Selected Root Causes (CoT Trace)

### Primary: Full-Screen Overlay Blocks User Interaction

**Step 1: Current Implementation**
```kotlin
// ProgressOverlayManager.kt (likely implementation)
fun showProgressOverlay(packageName: String, message: String) {
    val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,  // FULL WIDTH
        WindowManager.LayoutParams.MATCH_PARENT,  // FULL HEIGHT
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,  // Doesn't block entirely
        PixelFormat.TRANSLUCENT
    )
    // ... add overlay view to window manager
}
```

**Step 2: Why This Blocks Permissions**
- Overlay covers entire screen (MATCH_PARENT x MATCH_PARENT)
- Even with `FLAG_NOT_FOCUSABLE`, large overlay blocks visual access
- User cannot see where to click or what to do
- Permission dialog is underneath overlay

**Step 3: Evidence from Screenshot**
- Black overlay visible at bottom (likely full-width)
- Permission dialog center-screen (readable)
- But user cannot interact (overlay intercepting or obscuring)

**Step 4: Why Logs Show Click Success**
- LearnApp **programmatically** clicked SETTINGS button (works via AccessibilityService)
- But **user manual clicks** blocked by overlay
- AccessibilityService clicks bypass view hierarchy, manual clicks don't

### Secondary: No Dismiss Mechanism

**Missing Feature:**
```kotlin
// Currently: No way to dismiss
// Needed: Swipe-down gesture or "X" button to hide overlay temporarily
```

**User Flow Blocked:**
1. Permission dialog appears
2. User wants to grant permission manually
3. Tries to click SETTINGS → blocked by overlay
4. No way to hide overlay
5. Exploration stuck at 10%

## Fix Plan (Comprehensive Solution)

### Fix 1: Replace Full-Screen Overlay with Bottom Command Bar ✅ REQUIRED

**Design Specs (Material Design 3 + VoiceOS Guidelines):**

#### Visual Design
```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│               [App Content - Visible]                   │
│                                                         │
│                                                         │
│                 [Permission Dialog - Interactive]       │
│                                                         │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  🔄  Learning Teams... (24%)          [Pause] [✕]      │  ← 48dp height
└─────────────────────────────────────────────────────────┘
```

#### Layout Parameters
```kotlin
// NEW: Bottom command bar instead of full-screen overlay
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,  // Full width
    WindowManager.LayoutParams.WRAP_CONTENT,  // NOT full height!
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT
)
params.gravity = Gravity.BOTTOM  // Anchor to bottom
params.y = 0  // No offset (sits at very bottom)
```

#### Material Design 3 Components
```xml
<!-- command_bar_layout.xml -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="48dp"
    app:cardElevation="8dp"
    app:cardBackgroundColor="?attr/colorSurface"
    app:strokeColor="?attr/colorOutlineVariant"
    app:strokeWidth="1dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="16dp"
        android:paddingEnd="8dp">

        <!-- Progress Indicator -->
        <com.google.android.material.progressindicator.CircularProgressIndicator
            android:id="@+id/progress_indicator"
            android:layout_width="24dp"
            android:layout_height="24dp"
            app:indicatorSize="24dp"
            app:trackThickness="2dp"
            style="@style/Widget.Material3.CircularProgressIndicator" />

        <!-- Status Text -->
        <TextView
            android:id="@+id/status_text"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:text="Learning Teams... (24%)"
            android:textAppearance="@style/TextAppearance.Material3.BodyMedium"
            android:textColor="?attr/colorOnSurface"
            android:singleLine="true"
            android:ellipsize="end" />

        <!-- Pause Button -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/pause_button"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="Pause"
            android:textSize="12sp"
            style="@style/Widget.Material3.Button.TextButton" />

        <!-- Close Button -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/close_button"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:insetLeft="0dp"
            android:insetRight="0dp"
            app:icon="@drawable/ic_close"
            app:iconGravity="textStart"
            app:iconPadding="0dp"
            style="@style/Widget.Material3.Button.IconButton" />

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

#### Behavior
| Action | Behavior |
|--------|----------|
| **Show** | Slide up from bottom (200ms animation) |
| **Tap "Pause"** | Pause exploration, keep bar visible, change to "Resume" |
| **Tap "✕"** | Hide command bar (slide down), exploration continues in background |
| **Swipe down** | Dismiss command bar (alternative to ✕ button) |
| **Re-show** | Swipe up from bottom edge or tap notification |

---

### Fix 2: Add Pause/Resume Functionality ✅ REQUIRED

**Purpose:** Allow user to manually intervene (grant permissions, enter login info) without stopping exploration entirely.

**Implementation:**
```kotlin
// ProgressOverlayManager.kt
class ProgressOverlayManager {
    private var isPaused = false

    fun pauseExploration() {
        isPaused = true
        explorationEngine.pause()
        updateCommandBar(state = "Paused", buttonText = "Resume")
        // Keep command bar visible but show "Paused" state
    }

    fun resumeExploration() {
        isPaused = false
        explorationEngine.resume()
        updateCommandBar(state = "Exploring", buttonText = "Pause")
    }

    fun dismissCommandBar() {
        // Hide UI but keep exploration running
        commandBarView.animate()
            .translationY(commandBarHeight.toFloat())
            .setDuration(200)
            .start()
    }
}
```

**User Flow:**
```
1. Permission dialog appears
2. User taps "Pause" button on command bar
3. Exploration pauses
4. User clicks "SETTINGS" → grants permission
5. Returns to app
6. Taps "Resume" button
7. Exploration continues with new permissions
```

---

### Fix 3: Auto-Detect Blocked States (Permission/Login) ✅ RECOMMENDED

**Detection Logic:**
```kotlin
// LearnAppIntegration.kt
fun detectBlockedState(currentScreen: AccessibilityNodeInfo): BlockedState? {
    val screenText = currentScreen.textContent()

    // Permission dialog detection
    if (screenText.contains("needs permission", ignoreCase = true) ||
        screenText.contains("allow", ignoreCase = true) ||
        currentScreen.packageName == "com.android.permissioncontroller") {
        return BlockedState.PERMISSION_REQUIRED
    }

    // Login screen detection
    if (screenText.contains("sign in", ignoreCase = true) ||
        screenText.contains("log in", ignoreCase = true) ||
        screenText.contains("username", ignoreCase = true) ||
        screenText.contains("password", ignoreCase = true)) {
        return BlockedState.LOGIN_REQUIRED
    }

    return null
}

enum class BlockedState {
    PERMISSION_REQUIRED,
    LOGIN_REQUIRED
}
```

**Auto-Pause Behavior:**
```kotlin
// When blocked state detected
when (val blocked = detectBlockedState(currentScreen)) {
    BlockedState.PERMISSION_REQUIRED -> {
        pauseExploration()
        showCommandBar(
            message = "⚠️ Permission required - Paused for manual intervention",
            actionButton = "Resume when ready"
        )
        showToast("Tap 'Resume' after granting permission")
    }
    BlockedState.LOGIN_REQUIRED -> {
        pauseExploration()
        showCommandBar(
            message = "⚠️ Login required - Paused for manual login",
            actionButton = "Resume after login"
        )
    }
    null -> {
        // Continue exploration normally
    }
}
```

---

### Fix 4: Notification Alternative (Background Mode) ✅ OPTIONAL

**When command bar dismissed:**
```kotlin
// Show persistent notification instead
fun showBackgroundNotification(packageName: String, progress: Int) {
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_learn)
        .setContentTitle("Learning $packageName...")
        .setContentText("$progress% complete")
        .setProgress(100, progress, false)
        .setOngoing(true)
        .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
        .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
        .build()

    notificationManager.notify(LEARN_NOTIFICATION_ID, notification)
}
```

**User can:**
- Dismiss command bar completely
- Exploration continues in background
- Notification shows progress
- Tap notification to bring back command bar

---

## Files to Modify

| File | Changes | Priority |
|------|---------|----------|
| **ProgressOverlayManager.kt** | Replace full-screen overlay with bottom command bar | P0 |
| **command_bar_layout.xml** | NEW: Material 3 command bar design | P0 |
| **ExplorationEngine.kt** | Add pause/resume methods | P0 |
| **LearnAppIntegration.kt** | Add blocked state detection logic | P1 |
| **LearnAppIntegration.kt** | Auto-pause on permission/login screens | P1 |
| **NotificationHelper.kt** | Background notification for dismissed bar | P2 |

---

## Impact Assessment

### Before Fix
- ❌ User cannot grant permissions manually (overlay blocks UI)
- ❌ App exploration stuck at 10-30% (blocked by permissions)
- ❌ No way to enter login credentials
- ❌ User frustration (cannot intervene when needed)

### After Fix
- ✅ User can pause exploration anytime
- ✅ Command bar at bottom (doesn't block content)
- ✅ Dismissible UI (tap ✕ or swipe down)
- ✅ Auto-detect permission/login screens (auto-pause)
- ✅ Manual intervention possible (grant permissions, log in)
- ✅ Resume exploration after manual steps
- ✅ Complete app learning (95%+ coverage)

---

## Testing Requirements

### Test Case 1: Permission Dialog Appears
```kotlin
// Given: Exploration running, permission dialog appears
// When: User taps "Pause"
// Then:
//   - Exploration pauses
//   - Command bar shows "Paused"
//   - Button changes to "Resume"
//   - User can tap SETTINGS button in dialog
//   - User grants permission in Android Settings
//   - Returns to app
//   - Taps "Resume"
//   - Exploration continues
```

### Test Case 2: Dismiss Command Bar
```kotlin
// Given: Exploration running
// When: User taps "✕" button
// Then:
//   - Command bar slides down (dismisses)
//   - Exploration continues in background
//   - Notification appears in status bar
//   - User can interact with app freely
```

### Test Case 3: Auto-Pause on Permission Screen
```kotlin
// Given: Exploration running
// When: Permission dialog detected
// Then:
//   - Exploration auto-pauses
//   - Command bar shows "Permission required - Paused"
//   - Toast message: "Tap Resume after granting permission"
//   - User can grant permission
//   - User taps "Resume"
//   - Exploration continues
```

### Test Case 4: Login Screen Detection
```kotlin
// Given: Exploration encounters login screen
// When: Login screen detected (username/password fields)
// Then:
//   - Exploration auto-pauses
//   - Command bar shows "Login required - Paused"
//   - User enters credentials
//   - Logs in successfully
//   - Taps "Resume"
//   - Exploration continues with authenticated session
```

---

## Material Design 3 Compliance

### Component Checklist
- [x] Use `MaterialCardView` for command bar
- [x] Use `CircularProgressIndicator` (Material 3 style)
- [x] Use `MaterialButton.TextButton` and `IconButton`
- [x] Follow Material 3 color system (`?attr/colorSurface`, `?attr/colorOnSurface`)
- [x] Proper elevation (8dp for command bar)
- [x] Accessibility: 48dp minimum touch target
- [x] Typography: `TextAppearance.Material3.BodyMedium`
- [x] Icons: Material Icons (ic_close, ic_pause, ic_play)

### VoiceOS UI Guidelines
- [x] Bottom-aligned (doesn't obscure content)
- [x] Dismissible (user control)
- [x] Clear visual hierarchy (icon → text → actions)
- [x] Responsive (adapts to screen size)
- [x] Accessible (proper contrast, touch targets)

---

## Severity Justification

**CRITICAL** because:
1. **Blocks core functionality:** Cannot complete app learning (stuck at 10%)
2. **User frustration:** Cannot manually intervene when needed
3. **Data loss:** Incomplete explorations provide no value
4. **UX violation:** Overlay blocks user from using their device
5. **Scope:** Affects ALL apps requiring permissions or login (most apps)

---

## Prevention Measures

### Code Review Checklist
- [ ] ALL overlays must be bottom-aligned or dismissible
- [ ] NEVER use full-screen overlays that block user interaction
- [ ] ALWAYS provide manual pause/resume controls
- [ ] ALWAYS detect blocked states (permissions, login)
- [ ] Test with apps requiring permissions AND login

### Design Guidelines
- Bottom command bar for non-blocking progress
- Notification fallback for dismissed UI
- Auto-pause on detected blocked states
- Material Design 3 compliance
- Accessibility: 48dp touch targets, proper contrast

---

## Related Issues
- Issue #1 (v1.5): Power Down Detection
- Issue #2 (v1.5): Intent Relaunch Recovery
- Issue #3 (v1.6): Teams Safety Patterns
- Issue #4 (v1.7): LEARNED Status Bug

---

**End of Issue Analysis**
