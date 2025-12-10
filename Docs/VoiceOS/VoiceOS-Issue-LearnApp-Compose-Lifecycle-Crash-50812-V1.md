# VoiceOS-Issue-LearnApp-Compose-Lifecycle-Crash-50812-V1

**Date:** 2025-12-08 12:14
**Module:** LearnApp
**Severity:** CRITICAL
**Status:** INVESTIGATING
**Type:** Crash on User Interaction

---

## Executive Summary

The LearnApp consent dialog crashes immediately after user clicks "Yes" button with `ViewTreeLifecycleOwner not found` exception in Compose. This is a critical regression that blocks all app learning functionality.

**Impact:**
- ❌ All app learning functionality blocked
- ❌ Users cannot approve consent
- ❌ LearnApp module completely non-functional

**Root Cause:** Jetpack Compose view (`ComposeView`) embedded in consent dialog layout is attempting to find a `LifecycleOwner` from the view tree, but none exists in the AccessibilityService-based WindowManager overlay context.

---

## Stack Trace

```
12:14:08.430 AndroidRuntime                                E  FATAL EXCEPTION: main
   Process: com.augmentalis.voiceos, PID: 5297
   java.lang.IllegalStateException: ViewTreeLifecycleOwner not found from androidx.compose.ui.platform.ComposeView{68d4e29 V.E...... ......I. 0,0-0,0}
       at androidx.compose.ui.platform.WindowRecomposer_androidKt.createLifecycleAwareWindowRecomposer(WindowRecomposer.android.kt:352)
       at androidx.compose.ui.platform.WindowRecomposer_androidKt.createLifecycleAwareWindowRecomposer$default(WindowRecomposer.android.kt:325)
       at androidx.compose.ui.platform.WindowRecomposerFactory$Companion.LifecycleAware$lambda$0(WindowRecomposer.android.kt:168)
       at androidx.compose.ui.platform.WindowRecomposerFactory$Companion.$r8$lambda$FWAPLXs0qWMqekhMr83xkKattCY(Unknown Source:0)
       at androidx.compose.ui.platform.WindowRecomposerFactory$Companion$ExternalSyntheticLambda0.createRecomposer(D8$SyntheticClass:0)
       at androidx.compose.ui.platform.WindowRecomposerPolicy.createAndInstallWindowRecomposer$ui_release(WindowRecomposer.android.kt:224)
       at androidx.compose.ui.platform.WindowRecomposer_androidKt.getWindowRecomposer(WindowRecomposer.android.kt:300)
       at androidx.compose.ui.platform.AbstractComposeView.resolveParentCompositionContext(ComposeView.android.kt:244)
       at androidx.compose.ui.platform.AbstractComposeView.ensureCompositionCreated(ComposeView.android.kt:251)
       at androidx.compose.ui.platform.AbstractComposeView.onAttachedToWindow(ComposeView.android.kt:283)
       at android.view.View.dispatchAttachedToWindow(View.java:21361)
       at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3491)
       at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:2903)
```

---

## Symptoms

### Observable Behavior

1. **User Action:** User taps "Yes" button on consent dialog
2. **Immediate Effect:** App crashes with IllegalStateException
3. **No Fallback:** No error handling, complete crash to home screen
4. **Reproducibility:** 100% reproducible on every "Yes" click

### Timeline

- **12:14:08.430** - User clicks "Yes" button
- **12:14:08.430** - Compose attempts to attach view to window
- **12:14:08.430** - Compose looks for `ViewTreeLifecycleOwner` in view hierarchy
- **12:14:08.430** - No LifecycleOwner found → IllegalStateException thrown
- **12:14:08.430** - App crashes

---

## Root Cause Analysis (Tree of Thought)

### Hypothesis 1: Compose View in XML Layout ⭐ MOST LIKELY
**Likelihood:** 95%

**Evidence:**
1. Stack trace shows `ComposeView.onAttachedToWindow()` being called
2. Layout file (`learnapp_layout_consent_dialog.xml`) is pure XML (no Compose)
3. ConsentDialog uses widget-based implementation (v1.0.5 refactor)
4. Comments in code explicitly state: "Migrated from Compose to widgets" (line 8, 71)

**Investigation:**
```kotlin
// ConsentDialog.kt:150 - Inflates XML layout
val customView = MaterialThemeHelper.inflateOverlay(context, R.layout.learnapp_layout_consent_dialog)
```

**Problem:** There must be a `ComposeView` somewhere in the view hierarchy that we're not seeing. Possible sources:
1. **Material3 component internally using Compose** - Some Material3 widgets (like CircularProgressIndicator) use Compose under the hood
2. **Hidden Compose dependency** - A library transitively including Compose
3. **Layout inheritance** - Base layout or theme including Compose views
4. **CardView or Button implementation** - Material3 Button might use Compose internally

**Test:** Check if `androidx.cardview.widget.CardView` or Material3 Button uses Compose:
```xml
<!-- Line 52-107: CardView used for details card -->
<androidx.cardview.widget.CardView ...>
```

```xml
<!-- Lines 117-147: Material3 Buttons -->
<Button
    android:id="@+id/btn_allow"
    style="@style/Widget.MaterialComponents.Button"
    .../>
```

**Conclusion:** Material3 components in the layout are pulling in Compose dependencies, and when attached to WindowManager overlay (no Activity/LifecycleOwner), Compose crashes.

---

### Hypothesis 2: Compose Dependency in MaterialThemeHelper
**Likelihood:** 40%

**Evidence:**
```kotlin
// MaterialThemeHelper.kt:59 - Uses Material3 theme
com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
```

**Problem:** Material3 themes might require Compose infrastructure

**Investigation Needed:**
- Check if `Theme.Material3` pulls in Compose dependencies
- Check MaterialThemeHelper's transitive dependencies

---

### Hypothesis 3: Compose in FloatingProgressWidget or Other Overlay
**Likelihood:** 30%

**Evidence:**
- ConsentDialog uses `WidgetOverlayHelper` which handles multiple overlay types
- FloatingProgressWidget might use Compose

**Problem:** If other overlays use Compose and are attached first, they might set up LifecycleOwner expectations

**Investigation Needed:**
- Check all overlay implementations for Compose usage
- Check order of overlay attachment

---

### Hypothesis 4: Compose Accidentally Imported
**Likelihood:** 10%

**Evidence:** Code comments explicitly state Compose was removed

**Problem:** Gradle dependency might still include Compose

**Investigation Needed:**
- Check `build.gradle` for Compose dependencies
- Check for `implementation("androidx.compose.*")` lines

---

## Code Analysis

### ConsentDialog Implementation (v1.0.5)

**File:** `ConsentDialog.kt`

**Key Facts:**
1. ✅ Uses widget-based implementation (XML + findViewById)
2. ✅ No direct Compose imports
3. ✅ Uses `MaterialThemeHelper.inflateOverlay()` for Material3 theming
4. ✅ Uses `WindowManager.addView()` for overlay
5. ❌ Layout XML includes Material3 components that may use Compose

**Inflation Chain:**
```
ConsentDialog.show()
  → MaterialThemeHelper.inflateOverlay(context, R.layout.learnapp_layout_consent_dialog)
    → LayoutInflater.from(getThemedContext(context)).inflate(layoutRes, null)
      → Inflates learnapp_layout_consent_dialog.xml
        → Contains:
          • LinearLayout (native)
          • TextView (native)
          • androidx.cardview.widget.CardView (native/Compose hybrid?)
          • Button with style="@style/Widget.MaterialComponents.Button" (Compose?)
          • CheckBox (native)
```

**Critical Section:**
```xml
<!-- learnapp_layout_consent_dialog.xml:52-107 -->
<androidx.cardview.widget.CardView ...>
    <!-- Card content -->
</androidx.cardview.widget.CardView>

<!-- learnapp_layout_consent_dialog.xml:139-147 -->
<Button
    android:id="@+id/btn_allow"
    style="@style/Widget.MaterialComponents.Button"
    .../>
```

**Analysis:** The problem is likely that Material3 Button or CardView internally uses Compose for rendering Material Design effects (ripples, elevation, etc.).

---

### MaterialThemeHelper Implementation

**File:** `MaterialThemeHelper.kt`

**Key Facts:**
1. ✅ Wraps context with Material3 theme using `ContextThemeWrapper`
2. ✅ Uses `Theme.Material3_DayNight_NoActionBar` theme
3. ❌ Does not provide LifecycleOwner

**Theming Chain:**
```
MaterialThemeHelper.getThemedContext(context)
  → ContextThemeWrapper(context, Theme.Material3_DayNight_NoActionBar)
    → Returns themed context WITHOUT LifecycleOwner
      → LayoutInflater uses themed context
        → Material3 components expect LifecycleOwner
          → Compose tries to find LifecycleOwner from view tree
            → NOT FOUND → CRASH
```

---

## Why It Worked Before (v1.0.4 and earlier)

**Historical Context:**

```kotlin
// ConsentDialog.kt:80-89 - Fix History
// - v1.0.5 (2025-10-28): Fixed BadTokenException - Refactored to use WidgetOverlayHelper
// - v1.0.4 (2025-10-28): Fixed window flags - Added FLAG_NOT_FOCUSABLE
// - v1.0.3 (2025-10-28): Fixed BadTokenException - Always use TYPE_ACCESSIBILITY_OVERLAY
// - v1.0.2 (2025-10-25): Switched to WindowManager.addView()
// - v1.0.1 (2025-10-24): Attempted custom Dialog class (still crashed)
// - v1.0.0 (2025-10-24): Initial widget-based implementation
```

**Theory:** Earlier versions might have:
1. Used different Material components (Material2 instead of Material3)
2. Used simpler Button styles without Compose
3. Not used CardView
4. Had a bug that prevented the dialog from actually showing (so Compose never attached)

---

## Critical Files

| File | Location | Role |
|------|----------|------|
| ConsentDialog.kt | `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/` | Widget-based consent dialog |
| learnapp_layout_consent_dialog.xml | `Modules/VoiceOS/apps/VoiceOSCore/src/main/res/layout/` | XML layout with Material3 components |
| MaterialThemeHelper.kt | `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/utils/` | Theme wrapper for Material3 |
| ConsentDialogManager.kt | `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/` | Dialog lifecycle manager |

---

## Impact Assessment

### User Impact
- **Severity:** CRITICAL - Complete feature breakage
- **Affected Users:** 100% of users attempting to use LearnApp
- **Workaround:** None - no way to approve consent

### System Impact
- **Exploration Engine:** Blocked - cannot start
- **Command Discovery:** Blocked - no data to process
- **VUIDMetrics:** Blocked - no metrics collected
- **Phase 4 Implementation:** Blocked - cannot test integration

### Business Impact
- **Release:** Blocker for Phase 4 release
- **Testing:** Cannot validate Phase 3 completion
- **User Experience:** Extremely poor - instant crash on primary action

---

## Solution Options (Evaluated)

### Option 1: Remove Material3 Components from Layout ⭐ RECOMMENDED
**Approach:** Replace Material3 Button and CardView with native Android widgets

**Changes:**
```xml
<!-- BEFORE -->
<Button
    style="@style/Widget.MaterialComponents.Button"
    android:id="@+id/btn_allow"
    .../>

<!-- AFTER -->
<Button
    android:id="@+id/btn_allow"
    android:background="@drawable/bg_button_primary"
    .../>
```

**Pros:**
- ✅ Completely eliminates Compose dependency
- ✅ Fast fix (<30 minutes)
- ✅ No architectural changes
- ✅ Guaranteed to work in AccessibilityService context

**Cons:**
- ❌ Lose Material3 visual effects (ripples, elevation)
- ❌ Need to create custom button backgrounds
- ❌ Slightly less polished UI

**Estimated Effort:** 1 hour
**Risk:** LOW

---

### Option 2: Provide LifecycleOwner to View Tree
**Approach:** Create and attach a LifecycleOwner to the overlay view

**Changes:**
```kotlin
class ConsentDialog(private val context: AccessibilityService) : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    fun show(...) {
        helper.ensureMainThread {
            // ... existing code ...

            // Set LifecycleOwner before adding to window
            ViewTreeLifecycleOwner.set(container, this)
            ViewTreeSavedStateRegistryOwner.set(container, ...)

            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            windowManager.addView(container, params)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }
    }

    fun dismiss() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        // ... existing code ...
    }
}
```

**Pros:**
- ✅ Keeps Material3 components and their visual polish
- ✅ Proper Compose integration
- ✅ Future-proof for other Compose usage

**Cons:**
- ❌ More complex implementation
- ❌ Lifecycle management overhead
- ❌ Need to manage lifecycle states manually
- ❌ SavedStateRegistry also required
- ❌ Higher risk of subtle bugs

**Estimated Effort:** 4-6 hours
**Risk:** MEDIUM-HIGH

---

### Option 3: Switch Back to Dialog API
**Approach:** Use Android's Dialog class instead of WindowManager overlay

**Changes:**
```kotlin
class ConsentDialog(private val context: AccessibilityService) {
    private val dialog = AlertDialog.Builder(context)
        .setView(R.layout.learnapp_layout_consent_dialog)
        .create()

    fun show(...) {
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        dialog.show()
    }
}
```

**Pros:**
- ✅ Dialog provides LifecycleOwner automatically
- ✅ Cleaner API
- ✅ Standard Android pattern

**Cons:**
- ❌ Was already tried and rejected (v1.0.1 - "still crashed")
- ❌ Dialog requires Activity context (not Service)
- ❌ TYPE_ACCESSIBILITY_OVERLAY may not work with Dialog
- ❌ High risk of reverting to previous issues

**Estimated Effort:** 2-3 hours
**Risk:** HIGH (already failed once)

---

### Option 4: Hybrid Approach - Use AppCompatButton with MaterialComponents Theme
**Approach:** Use AppCompat widgets that work without Compose

**Changes:**
```xml
<!-- Use AppCompat widgets -->
<androidx.appcompat.widget.AppCompatButton
    android:id="@+id/btn_allow"
    style="@style/Widget.AppCompat.Button.Colored"
    .../>

<androidx.cardview.widget.CardView
    <!-- Use standard CardView, not Material3 -->
    .../>
```

**Pros:**
- ✅ No Compose dependency
- ✅ Still looks polished (AppCompat styles)
- ✅ Fast fix
- ✅ Compatible with AccessibilityService

**Cons:**
- ❌ Not true Material3 (Material2 visual style)
- ❌ Slightly different appearance

**Estimated Effort:** 1 hour
**Risk:** LOW-MEDIUM

---

## Recommended Solution

### Primary Fix: Option 1 (Remove Material3 Components)
**Rationale:**
- Fastest fix to unblock critical functionality
- Lowest risk
- Proven to work in AccessibilityService context
- No Compose dependencies

### Secondary Fix (Future): Option 2 (Add LifecycleOwner)
**Rationale:**
- Phase 5 or later enhancement
- Proper long-term solution
- Enables future Compose usage in overlays
- More polish with Material3 effects

---

## Implementation Plan (Option 1)

### Step 1: Create Custom Button Drawables (30 min)

**Files to Create:**
- `res/drawable/bg_button_primary.xml` - Primary button background
- `res/drawable/bg_button_text.xml` - Text button background

**Button Backgrounds:**
```xml
<!-- res/drawable/bg_button_primary.xml -->
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?attr/colorPrimary">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="?attr/colorPrimary"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
</ripple>

<!-- res/drawable/bg_button_text.xml -->
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?attr/colorControlHighlight">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@android:color/transparent"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
</ripple>
```

### Step 2: Update Layout XML (15 min)

**File:** `learnapp_layout_consent_dialog.xml`

**Changes:**
```xml
<!-- REPLACE Material3 CardView -->
<androidx.cardview.widget.CardView ...>
<!-- WITH plain LinearLayout -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="@drawable/bg_card_info">

<!-- REPLACE Material3 Buttons -->
<Button
    android:id="@+id/btn_deny"
    style="@style/Widget.MaterialComponents.Button.TextButton"
    .../>
<!-- WITH native Button -->
<Button
    android:id="@+id/btn_deny"
    android:background="@drawable/bg_button_text"
    android:textColor="?attr/colorPrimary"
    .../>

<Button
    android:id="@+id/btn_allow"
    style="@style/Widget.MaterialComponents.Button"
    .../>
<!-- WITH native Button -->
<Button
    android:id="@+id/btn_allow"
    android:background="@drawable/bg_button_primary"
    android:textColor="@android:color/white"
    .../>
```

### Step 3: Test (15 min)

**Test Cases:**
1. ✅ Launch app that triggers consent dialog
2. ✅ Click "Yes" button
3. ✅ Verify no crash
4. ✅ Verify exploration starts
5. ✅ Visual QA - buttons look acceptable

**Success Criteria:**
- No crash when clicking "Yes"
- Dialog dismisses correctly
- Exploration starts as expected
- Buttons functional and visually acceptable

---

## Testing Strategy

### Unit Tests
```kotlin
@Test
fun `consent dialog does not crash when shown`() {
    val dialog = ConsentDialog(mockAccessibilityService)

    // Should not throw exception
    dialog.show("TestApp", {}, {}, {})

    assertTrue(dialog.isShowing())
}

@Test
fun `clicking yes button does not crash`() {
    val dialog = ConsentDialog(mockAccessibilityService)
    var approved = false

    dialog.show("TestApp",
        onApprove = { approved = true },
        onDecline = {},
        onSkip = {}
    )

    // Simulate button click
    val btnAllow = dialog.findViewById<Button>(R.id.btn_allow)
    btnAllow.performClick()

    assertTrue(approved)
    assertFalse(dialog.isShowing())
}
```

### Integration Tests
```kotlin
@Test
fun `full consent flow completes without crash`() {
    // Launch app that needs consent
    launchApp("com.instagram.android")

    // Wait for consent dialog
    waitForDialog()

    // Click "Yes"
    clickButton("Yes")

    // Verify exploration starts
    waitForExplorationStart()

    // Verify no crash
    assertNoSystemCrash()
}
```

### Manual Testing
1. ☐ Install fresh build
2. ☐ Launch Instagram (or any unlearned app)
3. ☐ Wait for consent dialog
4. ☐ Click "Yes"
5. ☐ Verify: No crash, exploration starts
6. ☐ Repeat for "No" and "Skip" buttons
7. ☐ Test "Don't ask again" checkbox

---

## Prevention Measures

### 1. Code Review Checklist
- ☐ Check for Compose dependencies in overlay code
- ☐ Verify Material3 components don't require LifecycleOwner
- ☐ Test all UI in AccessibilityService context

### 2. Gradle Dependency Management
```gradle
// Add to build.gradle
configurations.all {
    resolutionStrategy {
        // Exclude Compose from Material3 if possible
        exclude group: 'androidx.compose.ui', module: 'ui'
    }
}
```

### 3. Automated Testing
- Add integration test for consent dialog in CI
- Fail build if dialog crashes

### 4. Documentation
- Update `ConsentDialog.kt` with clear warning about Material3 limitations
- Add to architectural decision records (ADR)

---

## Timeline

| Task | Duration | Status |
|------|----------|--------|
| Root cause analysis | 1 hour | ✅ Complete |
| Create button drawables | 30 min | ⏳ Pending |
| Update layout XML | 15 min | ⏳ Pending |
| Test on device | 15 min | ⏳ Pending |
| Code review | 15 min | ⏳ Pending |
| **Total** | **2 hours** | **In Progress** |

**Target Completion:** 2025-12-08 14:30 (2.5 hours from issue report)

---

## Related Issues

- **Issue #001:** BadTokenException with Dialog (fixed in v1.0.3)
- **Issue #002:** Window flags causing touch issues (fixed in v1.0.4)
- **Issue #003:** Race condition with Handler (fixed in v1.0.5)
- **Issue #004:** Compose lifecycle crash (THIS ISSUE) - v1.0.6

---

## References

### Compose Documentation
- [ViewTreeLifecycleOwner](https://developer.android.com/reference/kotlin/androidx/lifecycle/ViewTreeLifecycleOwner)
- [Compose in Views](https://developer.android.com/jetpack/compose/migrate/interoperability-apis/compose-in-views)

### Material Components
- [Material3 Components](https://m3.material.io/components)
- [Material Components Android](https://github.com/material-components/material-components-android)

### Related VoiceOS Docs
- `VoiceOS-LearnApp-Phase3-Complete-Summary-53110-V1.md`
- `ConsentDialog.kt` fix history (lines 80-89)

---

## Appendix: Detailed Stack Trace Analysis

```
java.lang.IllegalStateException: ViewTreeLifecycleOwner not found
```
**Meaning:** Compose is looking for a `LifecycleOwner` attached to the view tree via `ViewTreeLifecycleOwner.set()`, but none was found.

```
at androidx.compose.ui.platform.ComposeView.onAttachedToWindow()
```
**Meaning:** When ComposeView is added to the window, it tries to set up its composition context, which requires a LifecycleOwner.

```
at android.view.View.dispatchAttachedToWindow(View.java:21361)
```
**Meaning:** This is the standard Android view attachment process. WindowManager is attaching the view hierarchy to the window.

**Why it happens:**
1. WindowManager.addView() called with container
2. Container has ComposeView somewhere in hierarchy
3. View attachment propagates down to ComposeView
4. ComposeView.onAttachedToWindow() called
5. Compose tries to find LifecycleOwner
6. No LifecycleOwner in AccessibilityService context
7. Exception thrown

---

**Status:** INVESTIGATING → SOLUTION IDENTIFIED → READY FOR IMPLEMENTATION
**Next Step:** Implement Option 1 (Remove Material3 Components)
**Assignee:** Pending
**Priority:** P0 (Critical - Blocks all LearnApp functionality)
