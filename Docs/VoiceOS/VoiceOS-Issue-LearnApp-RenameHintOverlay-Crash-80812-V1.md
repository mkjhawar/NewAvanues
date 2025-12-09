# VoiceOS-Issue-LearnApp-RenameHintOverlay-Crash-80812-V1

**Date:** 2025-12-08
**Module:** LearnApp - RenameHintOverlay
**Severity:** Critical
**Status:** Open - Investigating
**Version:** v1.0.0 (RenameHintOverlay)

---

## Executive Summary

**Problem:** App crashes immediately after showing debug popup with same `ViewTreeLifecycleOwner not found` exception that was fixed for ConsentDialog.

**Root Cause:** `RenameHintOverlay.kt` uses `ComposeView` with WindowManager.addView(), triggering the same Compose lifecycle requirement that ConsentDialog had.

**Previous Fix:** ConsentDialog was fixed (v1.0.6) by removing Material3 components and replacing with native Android widgets.

**Current Issue:** RenameHintOverlay still uses Compose, causing crash when it tries to display.

---

## Symptoms

1. ✅ App starts successfully
2. ✅ Debug popup appears
3. ❌ **App crashes immediately after**
4. ❌ Stack trace shows `ViewTreeLifecycleOwner not found from androidx.compose.ui.platform.ComposeView`

### Stack Trace

```
13:01:49.579 AndroidRuntime                                E  FATAL EXCEPTION: main
     Process: com.augmentalis.voiceos, PID: 8157
     java.lang.IllegalStateException: ViewTreeLifecycleOwner not found from androidx.compose.ui.platform.ComposeView{2981206 V.E...... ......I. 0,0-0,0}
         at androidx.compose.ui.platform.WindowRecomposer_androidKt.createLifecycleAwareWindowRecomposer(WindowRecomposer.android.kt:352)
         at androidx.compose.ui.platform.WindowRecomposer_androidKt.createLifecycleAwareWindowRecomposer$default(WindowRecomposer.android.kt:325)
         at androidx.compose.ui.platform.WindowRecomposerFactory$Companion.LifecycleAware$lambda$0(WindowRecomposer.android.kt:168)
         ...
         at androidx.compose.ui.platform.AbstractComposeView.onAttachedToWindow(ComposeView.android.kt:283)
         at android.view.View.dispatchAttachedToWindow(View.java:21361)
         at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3491)
         at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:2903)
```

---

## Root Cause Analysis (Chain of Thought)

### Step 1: Identify Crash Source

**Analysis:**
- Stack trace shows `androidx.compose.ui.platform.ComposeView` crashing
- Same error as previous ConsentDialog issue
- Crash occurs "right after showing the debug popup"

**Question:** What overlay is shown after debug popup?

### Step 2: Search for Compose Usage

**Findings:**
- 42 files in codebase use Compose (`ComposeView`, `setContent`, `@Composable`)
- Multiple overlays in `/learnapp/ui/` directory
- **KEY FINDING:** `RenameHintOverlay.kt` line 217 creates `ComposeView`

```kotlin
// RenameHintOverlay.kt:217
val composeView = ComposeView(context).apply {
    setContent {
        RenameHintCard(
            exampleCommand = buttonName,
            onDismiss = { hide() }
        )
    }
}
```

### Step 3: Understand Trigger Mechanism

**Findings:**
- `RenameHintOverlay` is triggered by `ScreenActivityDetector`
- Shows when screen has generated fallback labels (Button 1, Tab 2, etc.)
- Triggered during exploration or after screen change
- Uses WindowManager.addView() with TYPE_ACCESSIBILITY_OVERLAY

**Timeline:**
1. User clicks "Yes" on consent dialog
2. Exploration starts
3. Debug overlay shows (if enabled)
4. `ScreenActivityDetector` detects generated labels
5. **`RenameHintOverlay.showIfNeeded()` is called**
6. **ComposeView created → crash**

### Step 4: Why Does It Crash?

**Technical Chain:**
```
ComposeView created
  ↓
WindowManager.addView() called
  ↓
View attached to window
  ↓
AbstractComposeView.onAttachedToWindow()
  ↓
Looks for ViewTreeLifecycleOwner
  ↓
NOT FOUND (AccessibilityService context)
  ↓
IllegalStateException thrown
```

**Why AccessibilityService doesn't have LifecycleOwner:**
- AccessibilityService is a Service, not an Activity
- LifecycleOwner is typically provided by Activity/Fragment
- WindowManager.addView() doesn't set up LifecycleOwner
- Compose components expect LifecycleOwner for recomposition

---

## Code Analysis

### Affected File: RenameHintOverlay.kt

**Location:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/RenameHintOverlay.kt`

**Problematic Code (Lines 216-224):**

```kotlin
// Create ComposeView
val composeView = ComposeView(context).apply {
    setContent {
        RenameHintCard(
            exampleCommand = buttonName,
            onDismiss = { hide() }
        )
    }
}
```

**Window Manager Usage (Lines 227-246):**

```kotlin
// Window layout parameters
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
    PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    y = dpToPx(16) // 16dp from top
}

// Add to WindowManager
try {
    windowManager.addView(composeView, params)  // ← CRASH HERE
    currentView = composeView
    Log.i(TAG, "Rename hint overlay displayed")
} catch (e: Exception) {
    Log.e(TAG, "Failed to add rename hint overlay", e)
}
```

### Compose Components Used

**Material3 Components:**
- Card (Material3)
- Text (Material3)
- Icon (Material3)
- Surface (Material3)
- Row, Column (Compose Foundation)
- AnimatedVisibility (Compose Animation)

**All require Compose runtime with LifecycleOwner**

---

## Impact Assessment

### Critical Impact:
- ❌ **App crashes immediately after exploration starts**
- ❌ **Blocks all LearnApp functionality** (can't explore apps)
- ❌ **ConsentDialog fix (v1.0.6) was ineffective** (RenameHintOverlay crashes instead)
- ❌ **Phase 4 implementation still blocked**

### Frequency:
- **Always:** Crashes whenever RenameHintOverlay tries to display
- **Trigger:** Any screen with generated labels (very common)
- **User Impact:** Cannot use LearnApp at all

### Scope:
- **Single File:** Only RenameHintOverlay.kt affected
- **Created:** 2025-12-08 (very recent addition)
- **Other Overlays:** ConsentDialog (fixed), DebugOverlay (doesn't use Compose)

---

## Solution Options

### Option 1: Remove Compose from RenameHintOverlay ⭐ RECOMMENDED

**Approach:** Same fix as ConsentDialog - replace Compose with native Android widgets + XML layout

**Implementation:**
1. Create XML layout for hint card (similar to consent dialog)
2. Create custom drawable for card background
3. Replace ComposeView with View inflation
4. Use MaterialThemeHelper.inflateOverlay() for theming
5. Preserve animations with native View animations

**Pros:**
- ✅ Proven solution (worked for ConsentDialog)
- ✅ Fast implementation (1-2 hours)
- ✅ LOW risk
- ✅ No Compose dependency
- ✅ Works reliably in AccessibilityService
- ✅ Consistent with ConsentDialog fix

**Cons:**
- ⚠️ Slightly less polished animations (no Compose animations)
- ⚠️ More XML code to maintain

**Effort:** 1-2 hours
**Risk:** LOW
**Complexity:** SIMPLE

---

### Option 2: Add LifecycleOwner to RenameHintOverlay

**Approach:** Create custom LifecycleOwner for the overlay

**Implementation:**
1. Create custom LifecycleOwner implementation
2. Implement Lifecycle with state management
3. Set ViewTreeLifecycleOwner on ComposeView
4. Set ViewTreeSavedStateRegistryOwner
5. Manage lifecycle states (CREATED → STARTED → RESUMED → DESTROYED)
6. Handle edge cases (rapid show/hide, service restart)

**Pros:**
- ✅ Keeps Compose animations
- ✅ Maintains Material3 polish
- ✅ Reusable pattern for other overlays

**Cons:**
- ❌ Complex implementation (4-6 hours)
- ❌ MEDIUM-HIGH risk (lifecycle management is tricky)
- ❌ Ongoing maintenance burden
- ❌ May have edge cases/bugs
- ❌ Not the Android-recommended pattern for overlays

**Effort:** 4-6 hours
**Risk:** MEDIUM-HIGH
**Complexity:** COMPLEX

---

### Option 3: Disable RenameHintOverlay (Temporary)

**Approach:** Temporarily disable the overlay until proper fix is implemented

**Implementation:**
1. Add feature flag to disable RenameHintOverlay
2. Comment out showIfNeeded() call in ScreenActivityDetector
3. Add TODO comment

**Pros:**
- ✅ Immediate unblocking (5 minutes)
- ✅ ZERO risk
- ✅ Allows testing of other functionality

**Cons:**
- ❌ Loses RenameHintOverlay feature
- ❌ Not a real solution (temporary only)
- ❌ Technical debt

**Effort:** 5 minutes
**Risk:** ZERO
**Complexity:** TRIVIAL

---

### Option 4: Systematic Compose Removal ⭐ LONG-TERM

**Approach:** Remove ALL Compose usage from AccessibilityService overlays

**Implementation:**
1. Audit all 42 files using Compose
2. Identify which are used in AccessibilityService context
3. Convert each to native Android widgets
4. Create reusable XML layout patterns
5. Establish coding standards (no Compose in overlays)

**Pros:**
- ✅ Eliminates entire class of bugs
- ✅ Consistent approach across codebase
- ✅ Better performance (no Compose overhead)
- ✅ Simpler maintenance

**Cons:**
- ❌ Large effort (20-30 hours for all 42 files)
- ❌ High churn (many file changes)
- ❌ Requires coordination

**Effort:** 20-30 hours (full audit + fixes)
**Risk:** MEDIUM
**Complexity:** MEDIUM

---

## Recommendation

**Immediate (Today):**
- **Option 3:** Disable RenameHintOverlay (5 minutes) → Unblocks testing
- **Option 1:** Fix RenameHintOverlay (1-2 hours) → Restore feature

**Short-Term (This Week):**
- Verify no other overlays crash
- Test exploration flow end-to-end
- Document pattern for future overlays

**Long-Term (Phase 5+):**
- **Option 4:** Systematic Compose removal from overlays
- Establish coding standard: "No Compose in AccessibilityService overlays"
- Add lint rule to prevent future Compose usage in overlays

---

## Related Files

### Files That Use Compose in LearnApp

**Already Fixed:**
- ✅ `ConsentDialog.kt` - Fixed in v1.0.6

**Currently Crashing:**
- ❌ `RenameHintOverlay.kt` - **THIS ISSUE**

**Potentially Affected (Need Review):**
- ⚠️ `CommandDiscoveryOverlay.kt` - May use Compose
- ⚠️ `ManualLabelDialog.kt` - Uses Dialog (Activity context, likely safe)
- ⚠️ `LoginPromptOverlay.kt` - May use Compose
- ⚠️ `MetadataNotificationView.kt` - May use Compose

**Safe (Don't Use Compose):**
- ✅ `ProgressOverlayManager.kt` - Uses WidgetOverlayHelper
- ✅ `FloatingProgressWidget.kt` - XML-based
- ✅ `DebugOverlayManager.kt` - XML-based
- ✅ `DebugOverlayView.kt` - XML-based

---

## Testing Plan

### After Fix Implementation:

#### Test Case 1: Exploration Start (Critical)
**Steps:**
1. Install fixed build
2. Launch Instagram
3. Click "Yes" on consent dialog
4. Wait for exploration to start

**Expected:**
- ✅ No crash after debug popup
- ✅ Exploration proceeds normally
- ✅ RenameHintOverlay displays (if Option 1)
- ✅ Or hint is silently skipped (if Option 3)

#### Test Case 2: Generated Labels Detection
**Steps:**
1. During exploration, wait for screen with generated labels
2. Observe RenameHintOverlay behavior

**Expected:**
- ✅ Overlay displays without crash (Option 1)
- ✅ Or no overlay shown (Option 3)
- ✅ 3-second auto-dismiss works (Option 1)

#### Test Case 3: Multiple Screen Changes
**Steps:**
1. Navigate between multiple screens during exploration
2. Trigger multiple RenameHintOverlay displays

**Expected:**
- ✅ No crashes on repeated displays
- ✅ Previous overlays dismissed properly
- ✅ No memory leaks

---

## Prevention Measures

### Immediate:
1. **Code Review Checklist:** "Does this overlay use Compose?"
2. **Documentation:** Add warning in WidgetOverlayHelper about Compose
3. **Example Code:** Provide XML-based overlay template

### Long-Term:
1. **Lint Rule:** Detect ComposeView in AccessibilityService context
2. **Coding Standard:** "No Compose in AccessibilityService overlays"
3. **Architecture Doc:** Document why Compose doesn't work in overlays

---

## Next Steps

**Immediate Actions:**
1. ☐ Implement Option 3 (disable) OR Option 1 (fix) - **USER DECISION**
2. ☐ Test with exploration flow
3. ☐ Verify no other overlays crash
4. ☐ Update documentation

**Follow-Up:**
1. ☐ Audit remaining 40 Compose files
2. ☐ Identify other potential crash sources
3. ☐ Create systematic fix plan (Option 4)

---

## Related Documentation

- `VoiceOS-Issue-LearnApp-Compose-Lifecycle-Crash-50812-V1.md` - Original ConsentDialog issue
- `VoiceOS-Fix-LearnApp-Compose-Crash-Complete-50812-V1.md` - ConsentDialog fix
- `LearnApp-Rename-Hint-Overlay-Mockups-5081220-V1.md` - Original RenameHintOverlay spec
- `LearnApp-On-Demand-Command-Renaming-5081220-V2.md` - Feature spec

---

**Status:** ⚠️ CRITICAL - REQUIRES IMMEDIATE FIX
**Blocks:** All LearnApp functionality, Phase 4 implementation
**Discovered:** 2025-12-08
**Assigned:** Pending user decision on Option 1 vs Option 3
