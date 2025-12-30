# Implementation Plan: LearnApp Widget Migration

**Spec Number:** 001
**Feature:** LearnApp Compose to Widget Migration
**Created:** 2025-10-24
**Status:** Draft
**Implements:** spec.md v1.0

---

## Executive Summary

This plan outlines the migration of 5 LearnApp UI components from Jetpack Compose to legacy Android widgets to resolve crashes in AccessibilityService context. The migration will be executed in 3 phases over 3-4 implementation cycles, with each cycle following the IDE Loop (Implement → Defend → Evaluate → Commit).

**Timeline:** 4-6 hours (3 phases)
**Risk Level:** Medium (well-understood technology, architectural change)
**Team:** 1 developer + @vos4-android-expert + @vos4-test-specialist

---

## Technical Approach

### Strategy: Incremental Migration

**Chosen Approach:** Migrate components one-by-one, starting with simplest (ProgressOverlay) to validate approach before tackling complex components (ConsentDialog).

**Rationale:**
- De-risks migration by proving approach early
- Allows rollback of individual components if issues arise
- Maintains working codebase throughout migration
- Provides learning opportunity for developer

### Alternative Approaches Considered

#### ❌ Option 1: Big Bang Migration (All at Once)
**Pros:** Faster if successful
**Cons:** High risk, hard to debug, all-or-nothing
**Rejected:** Too risky for production system

#### ❌ Option 2: Hybrid Compose + Widgets
**Pros:** Could keep some Compose components
**Cons:** Increases complexity, doesn't solve root cause
**Rejected:** Compose fundamentally incompatible with AccessibilityService

#### ✅ Option 3: Incremental Migration (SELECTED)
**Pros:** Low risk, easy rollback, validates approach early
**Cons:** Takes longer, requires coordination between old/new
**Selected:** Best balance of risk and timeline

---

## Architecture Overview

### Current Architecture (Compose)

```
AccessibilityService Context
    ↓
ComposeView (window overlay)
    ↓
Compose Runtime + Recomposer
    ↓
Custom LifecycleOwner (MyLifecycleOwner)
    ↓
Composable UI Functions
    ↓
Material Design 3 Components
```

**Problem:** Compose expects Activity lifecycle, AccessibilityService doesn't provide one.

### Target Architecture (Widgets)

```
AccessibilityService Context
    ↓
WindowManager.addView()
    ↓
Dialog (for modals) OR FrameLayout (for overlays)
    ↓
XML Layout OR Programmatic Views
    ↓
android.widget.* components
    ↓
Material Components styling
```

**Solution:** Direct window management, no lifecycle dependency.

---

## Implementation Phases

### Phase 1: Foundation & Simple Component (2-3 hours)

**Goal:** Establish widget pattern and migrate simplest component

**Components:**
1. Create widget utility classes
2. Migrate `ProgressOverlay.kt` and `ProgressOverlayManager.kt`

**Deliverables:**
- `WidgetOverlayHelper.kt` - Utility for WindowManager operations
- `ProgressOverlay.kt` (migrated to widgets)
- `ProgressOverlayManager.kt` (updated to use widgets)
- XML layout: `layout_progress_overlay.xml`
- Tests: 10+ tests for progress overlay

**Acceptance Criteria:**
- Progress overlay displays without crashes
- No ViewTreeLifecycleOwner exceptions
- Display latency <100ms
- Memory leak verified (LeakCanary)
- All tests passing (80%+ coverage)

---

### Phase 2: Modal Dialogs (2-3 hours)

**Goal:** Migrate consent and login dialogs

**Components:**
1. Migrate `ConsentDialog.kt` and `ConsentDialogManager.kt`
2. Migrate `LoginPromptOverlay.kt`

**Deliverables:**
- `ConsentDialog.kt` (migrated to AlertDialog + custom view)
- `ConsentDialogManager.kt` (updated to use AlertDialog)
- `LoginPromptOverlay.kt` (migrated to AlertDialog + custom view)
- XML layouts: `dialog_consent.xml`, `dialog_login.xml`
- Tests: 15+ tests for consent dialog and login prompt

**Acceptance Criteria:**
- Consent dialog displays without crashes
- Login prompt displays without crashes
- No BadTokenException errors
- All buttons functional
- State preservation working
- All tests passing (80%+ coverage)

---

### Phase 3: Polish & Cleanup (1 hour)

**Goal:** Visual parity, performance optimization, cleanup

**Tasks:**
1. Apply Material Design 3 styling
2. Add animations (if within performance budget)
3. Remove Compose dependencies
4. Update documentation
5. Final testing across Android versions

**Deliverables:**
- Styled widgets matching Compose visual design
- Optional: Fade/slide animations
- Updated module dependencies (Compose removed)
- Updated documentation
- Performance benchmarks

**Acceptance Criteria:**
- Visual parity with Compose (90%+ match)
- Animations smooth (<300ms, no jank)
- All Compose dependencies removed from LearnApp
- Documentation updated
- Manual QA passed on API 29, 32, 34

---

## Technical Design

### Component 1: WidgetOverlayHelper

**Purpose:** Centralized utility for WindowManager overlay operations

**Class Design:**
```kotlin
class WidgetOverlayHelper(
    private val context: Context,
    private val windowManager: WindowManager
) {
    fun showOverlay(view: View, params: WindowManager.LayoutParams)
    fun dismissOverlay(view: View)
    fun createOverlayParams(type: Int): WindowManager.LayoutParams
    fun ensureMainThread(block: () -> Unit)
}
```

**Key Methods:**
- `showOverlay()`: Adds view to window manager on main thread
- `dismissOverlay()`: Removes view safely with null checks
- `createOverlayParams()`: Creates WindowManager.LayoutParams with TYPE_ACCESSIBILITY_OVERLAY
- `ensureMainThread()`: Ensures UI operations run on main thread via Handler

**Thread Safety:**
- All public methods ensure main thread via `Handler.post()`
- Internal checks for view attachment state before operations

---

### Component 2: ProgressOverlay (Widget Version)

**Current Implementation (Compose):**
```kotlin
@Composable
fun ProgressOverlay(message: String) {
    // Compose UI with CircularProgressIndicator
}
```

**Target Implementation (Widget):**
```kotlin
class ProgressOverlay(private val context: Context) {
    private val rootView: FrameLayout
    private val progressBar: ProgressBar
    private val messageText: TextView

    init {
        // Inflate from XML or create programmatically
    }

    fun show(windowManager: WindowManager, message: String)
    fun dismiss(windowManager: WindowManager)
}
```

**XML Layout (`layout_progress_overlay.xml`):**
```xml
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#80000000"> <!-- Semi-transparent overlay -->

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:orientation="vertical"
        android:background="@drawable/bg_rounded_card"
        android:padding="24dp">

        <ProgressBar
            android:id="@+id/progress_bar"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:layout_gravity="center" />

        <TextView
            android:id="@+id/message_text"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:textSize="16sp"
            android:textColor="?android:textColorPrimary" />
    </LinearLayout>
</FrameLayout>
```

**State Management:**
- No state preservation needed (ephemeral overlay)
- Message passed as parameter

---

### Component 3: ConsentDialog (Widget Version)

**Current Implementation (Compose):**
```kotlin
@Composable
fun ConsentDialog(
    command: String,
    explanation: String,
    onAllow: () -> Unit,
    onDeny: () -> Unit
)
```

**Target Implementation (Widget):**
```kotlin
class ConsentDialog(private val context: Context) {

    fun show(
        command: String,
        explanation: String,
        onAllow: () -> Unit,
        onDeny: () -> Unit
    ) {
        val customView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_consent, null)

        // Configure view with data
        customView.findViewById<TextView>(R.id.command_text).text = command
        customView.findViewById<TextView>(R.id.explanation_text).text = explanation

        // Create AlertDialog
        val dialog = AlertDialog.Builder(context, R.style.Theme_VOS_Dialog)
            .setView(customView)
            .setCancelable(false)
            .create()

        // Set window type for AccessibilityService
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)

        // Wire up buttons
        customView.findViewById<Button>(R.id.btn_allow).setOnClickListener {
            onAllow()
            dialog.dismiss()
        }
        customView.findViewById<Button>(R.id.btn_deny).setOnClickListener {
            onDeny()
            dialog.dismiss()
        }

        // Show on main thread
        Handler(Looper.getMainLooper()).post {
            dialog.show()
        }
    }
}
```

**XML Layout (`dialog_consent.xml`):**
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <!-- Title -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/consent_title"
        android:textSize="20sp"
        android:textStyle="bold"
        android:textColor="?android:textColorPrimary" />

    <!-- Command -->
    <TextView
        android:id="@+id/command_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:textSize="16sp"
        android:textColor="?android:textColorSecondary" />

    <!-- Explanation -->
    <TextView
        android:id="@+id/explanation_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textSize="14sp"
        android:textColor="?android:textColorTertiary" />

    <!-- Privacy Notice -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="@string/consent_privacy_notice"
        android:textSize="12sp"
        android:textColor="?android:textColorTertiary" />

    <!-- Buttons -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:orientation="horizontal"
        android:gravity="end">

        <Button
            android:id="@+id/btn_deny"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/consent_deny"
            android:minWidth="88dp" />

        <Button
            android:id="@+id/btn_allow"
            style="@style/Widget.Material3.Button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:text="@string/consent_allow"
            android:minWidth="88dp" />
    </LinearLayout>
</LinearLayout>
```

**State Management:**
- No state preservation needed (modal, decision-based)
- Command/explanation passed as parameters
- Callbacks handle result

---

### Component 4: LoginPromptOverlay (Widget Version)

**Similar structure to ConsentDialog:**
- Uses AlertDialog with custom view
- XML layout with username/password fields
- TextInputLayout from Material Components
- Login/Cancel buttons
- Window type: TYPE_ACCESSIBILITY_OVERLAY

---

### Component 5: Manager Classes

**ConsentDialogManager & ProgressOverlayManager:**
- Keep coroutine-based event handling (non-UI logic)
- Replace Compose lifecycle management with simple show/dismiss
- Use new widget-based dialog/overlay classes
- Ensure main thread via Handler for UI operations

---

## Data Models & Contracts

### No Changes Required

All data models and interfaces remain unchanged:
- `LearnedCommand` entity
- `ConsentResult` sealed class
- `AuthenticationState` sealed class
- Event flows and callbacks

**Rationale:** This is purely a UI implementation change. Data layer is unaffected.

---

## Threading Model

### Current (Compose)

```kotlin
// Compose handles threading internally
LaunchedEffect {
    withContext(Dispatchers.Main) {
        // UI operations
    }
}
```

### Target (Widgets)

```kotlin
// Explicit main thread management
private val mainHandler = Handler(Looper.getMainLooper())

fun showDialog() {
    mainHandler.post {
        // UI operations guaranteed on main thread
    }
}
```

**Key Principles:**
1. All WindowManager operations on main thread
2. Use `Handler.post()` for thread safety
3. Coroutines still used for non-UI operations (network, database)
4. No lifecycle observer overhead

---

## Testing Strategy

### Unit Tests (70% of coverage)

**Tools:** JUnit 5, MockK, Robolectric

**Test Coverage:**
- WidgetOverlayHelper class
- View inflation logic
- Button click handlers
- State management
- Thread safety (main thread enforcement)

**Example Test:**
```kotlin
@Test
fun `showOverlay should add view to window manager on main thread`() {
    // Given
    val helper = WidgetOverlayHelper(context, windowManager)
    val view = FrameLayout(context)
    val params = helper.createOverlayParams(TYPE_ACCESSIBILITY_OVERLAY)

    // When
    helper.showOverlay(view, params)

    // Then
    shadowOf(Looper.getMainLooper()).idle() // Process main thread
    verify { windowManager.addView(view, params) }
}
```

---

### Integration Tests (25% of coverage)

**Tools:** Espresso, AndroidX Test

**Test Coverage:**
- End-to-end dialog display
- Button interactions
- Callback verification
- Multiple show/dismiss cycles
- Memory leak detection

**Example Test:**
```kotlin
@Test
fun consentDialog_whenAllowClicked_shouldInvokeCallback() = runTest {
    // Given
    var callbackInvoked = false
    val dialog = ConsentDialog(context)

    // When
    dialog.show(
        command = "test command",
        explanation = "test explanation",
        onAllow = { callbackInvoked = true },
        onDeny = {}
    )

    // Interact with UI
    onView(withId(R.id.btn_allow)).perform(click())

    // Then
    assertThat(callbackInvoked).isTrue()
}
```

---

### E2E Tests (5% of coverage)

**Tools:** Manual QA, automated E2E suite

**Test Scenarios:**
1. Real AccessibilityService displays consent dialog
2. User approves learned command
3. User denies learned command
4. Login flow from start to finish
5. Progress overlay during long operation

**Manual QA Matrix:**
- Android 10 (API 29)
- Android 12 (API 32)
- Android 14 (API 34)
- Different screen sizes
- Dark mode / Light mode

---

## Performance Considerations

### Optimization Targets

1. **Display Latency:**
   - Target: <100ms from trigger to visible
   - Measurement: Systrace / Android Profiler
   - Optimization: Pre-inflate views, cache layouts

2. **Memory Footprint:**
   - Target: <5MB increase vs Compose baseline
   - Measurement: Android Profiler heap dumps
   - Optimization: Recycle views, use ViewStub for complex layouts

3. **Render Performance:**
   - Target: 60 FPS during animations
   - Measurement: GPU Profiler
   - Optimization: Hardware acceleration, simple animations

### Performance Testing

```kotlin
@Test
fun `progress overlay should display within 100ms`() {
    val startTime = System.currentTimeMillis()

    progressOverlay.show(windowManager, "Testing...")
    shadowOf(Looper.getMainLooper()).idle()

    val displayTime = System.currentTimeMillis() - startTime
    assertThat(displayTime).isLessThan(100)
}
```

---

## Migration Sequence

### Step-by-Step Execution

#### Phase 1: Foundation (Day 1, Morning)

1. **Create WidgetOverlayHelper** (30 min)
   - Implement class with WindowManager operations
   - Add thread safety via Handler
   - Write 5 unit tests

2. **Migrate ProgressOverlay** (1 hour)
   - Create XML layout
   - Implement widget-based class
   - Update ProgressOverlayManager
   - Write 8 unit tests

3. **Test & Verify** (30 min)
   - Run test suite (must pass 80%+)
   - Manual QA on device
   - LeakCanary verification
   - Performance profiling

**IDE Loop:**
- ✅ IMPLEMENT: Create helper + migrate progress overlay
- ✅ DEFEND: Write 13 tests, verify 80%+ coverage
- ✅ EVALUATE: Manual QA passes, no crashes, <100ms display
- ✅ COMMIT: Phase 1 complete

---

#### Phase 2: Modal Dialogs (Day 1, Afternoon)

1. **Migrate ConsentDialog** (1.5 hours)
   - Create XML layout for consent
   - Implement AlertDialog-based class
   - Update ConsentDialogManager
   - Write 10 unit tests

2. **Migrate LoginPromptOverlay** (1 hour)
   - Create XML layout for login
   - Implement AlertDialog-based class
   - Write 5 unit tests

3. **Test & Verify** (30 min)
   - Run test suite (must pass 80%+)
   - Manual QA for both dialogs
   - LeakCanary verification

**IDE Loop:**
- ✅ IMPLEMENT: Migrate consent dialog + login prompt
- ✅ DEFEND: Write 15 tests, verify 80%+ coverage
- ✅ EVALUATE: Manual QA passes, no BadTokenException
- ✅ COMMIT: Phase 2 complete

---

#### Phase 3: Polish (Day 2, Morning)

1. **Apply Material Design Styling** (30 min)
   - Add Material Components theme
   - Style buttons, text, backgrounds
   - Test dark mode compatibility

2. **Add Animations (if budget allows)** (20 min)
   - Fade in/out for dialogs
   - Measure performance impact
   - Skip if exceeds 100ms budget

3. **Cleanup** (10 min)
   - Remove Compose dependencies from LearnApp
   - Delete old Compose files
   - Update build.gradle

4. **Final Testing** (30 min)
   - Full regression test
   - Manual QA on API 29, 32, 34
   - Performance benchmarks
   - Documentation update

**IDE Loop:**
- ✅ IMPLEMENT: Polish UI, cleanup Compose
- ✅ DEFEND: Final test suite run, all tests pass
- ✅ EVALUATE: Visual parity achieved, no regressions
- ✅ COMMIT: Phase 3 complete, migration done

---

## Constitution Check

### Alignment with VOS4 Principles

#### ✅ Principle I: Performance-First Architecture

**Compliance:**
- Display latency <100ms (constitution requirement)
- Memory budget <5MB increase
- Performance profiling before merge
- Widgets typically faster than Compose (less overhead)

**Evidence:** Performance benchmarks in test suite

---

#### ✅ Principle II: Direct Implementation (No Interfaces)

**Compliance:**
- WidgetOverlayHelper is concrete class (no interface)
- Dialog classes are concrete implementations
- No abstraction layers added

**Exception:** None - no interfaces needed

---

#### ✅ Principle III: Privacy & Accessibility First

**Compliance:**
- All UI components remain voice-navigable
- TalkBack compatibility tested
- Content descriptions on all interactive elements
- Touch targets ≥48dp
- No data privacy changes (UI-only migration)

**Evidence:** Accessibility tests in test suite

---

#### ✅ Principle IV: Modular Independence

**Compliance:**
- Changes isolated to LearnApp module
- No cross-module dependencies added
- Self-contained migration
- LearnApp can still build independently

**Evidence:** Module continues to compile standalone

---

#### ✅ Principle V: Quality Through Enforcement

**Compliance:**
- 80%+ test coverage (MANDATORY)
- @vos4-test-specialist will enforce testing
- @vos4-documentation-specialist will update docs
- Zero compiler warnings
- IDE Loop enforced for all 3 phases

**Evidence:** Test coverage reports, subagent approval

---

### Quality Gates Verification

1. **Architecture Gate:** ✅ Direct implementation, no unapproved interfaces
2. **Testing Gate:** ✅ 80%+ coverage, all tests passing
3. **Performance Gate:** ✅ <100ms latency, <5MB memory increase
4. **Namespace Gate:** ✅ All code in `com.augmentalis.learnapp.*`
5. **Documentation Gate:** ✅ Module docs updated with widget architecture
6. **Subagent Gate:** ✅ Requires @vos4-test-specialist + @vos4-documentation-specialist approval

---

## Documentation Updates Required

### During Implementation

1. **Module Developer Manual:**
   - Update LearnApp architecture diagram (Compose → Widgets)
   - Document WidgetOverlayHelper usage
   - Add widget layout guidelines

2. **Living Docs:**
   - `notes.md`: Add gotchas about AccessibilityService overlays
   - `decisions.md`: Document decision to use widgets over Compose
   - `bugs.md`: Close this issue when complete

3. **Code Documentation:**
   - KDoc for all new classes
   - XML layout comments
   - Thread safety notes in WidgetOverlayHelper

### After Completion

4. **Architecture Diagrams:**
   - Update LearnApp UI architecture diagram
   - Add sequence diagram for consent flow

5. **Testing Documentation:**
   - Document widget testing patterns
   - Add Espresso test examples

---

## Rollback Plan

### If Migration Fails

**Trigger Conditions:**
- Unable to achieve <100ms display time
- Memory leaks cannot be resolved
- Test coverage cannot reach 80%
- Visual degradation unacceptable

**Rollback Procedure:**
1. Revert commits for failed phase
2. Keep completed phases (incremental rollback)
3. Document issues encountered
4. Investigate alternative approaches (e.g., custom Compose window lifecycle)

**Git Strategy:**
- Each phase is atomic commit
- Tags: `phase-1-complete`, `phase-2-complete`, `phase-3-complete`
- Easy to cherry-pick or revert individual phases

---

## Dependencies & Prerequisites

### External Dependencies

- Material Components for Android (already in project)
- Android SDK APIs 29-34 (already supported)
- No new third-party libraries required

### Internal Prerequisites

- AccessibilityService running (VoiceOSCore)
- WindowManager permissions granted
- LearnApp module compiling

### Developer Prerequisites

- Understanding of Android widgets (XML layouts, AlertDialog, WindowManager)
- Experience with AccessibilityService overlays
- Familiarity with thread safety (Handler, main thread)

---

## Success Metrics

### Functional Success

- ✅ All 5 components migrated
- ✅ Zero ViewTreeLifecycleOwner exceptions
- ✅ Zero thread-related exceptions
- ✅ Zero window token exceptions
- ✅ 100% feature parity with Compose

### Quality Success

- ✅ 80%+ test coverage (MANDATORY)
- ✅ All tests passing
- ✅ Zero compiler warnings
- ✅ Manual QA passed on 3+ Android versions
- ✅ LeakCanary: 0 memory leaks

### Performance Success

- ✅ Display latency <100ms (95th percentile)
- ✅ Memory increase <5MB
- ✅ 60 FPS during animations (if added)
- ✅ Build time unchanged or faster

---

## Risk Mitigation

### Risk: UX Degradation

**Mitigation:**
- Spend extra time on XML styling
- Use Material Components library
- Get user feedback on visual parity
- Accept minor differences if functionally equivalent

### Risk: Thread Complexity

**Mitigation:**
- Create WidgetOverlayHelper for centralized thread management
- Use Handler.post() consistently
- Add thread safety tests
- Document threading model clearly

### Risk: State Management Issues

**Mitigation:**
- Simplify state (most dialogs are ephemeral)
- Use Bundle for any needed state preservation
- Test show/dismiss cycles thoroughly

---

## Timeline Estimate

**Total: 4-6 hours**

- Phase 1 (Foundation): 2-3 hours
- Phase 2 (Dialogs): 2-3 hours
- Phase 3 (Polish): 1 hour

**Best Case:** 4 hours (no issues, smooth implementation)
**Expected:** 5 hours (minor debugging, styling iteration)
**Worst Case:** 6 hours (unexpected threading issues, visual parity challenges)

---

**Next Steps:**

1. ✅ Specification approved
2. ✅ Implementation plan approved
3. → Generate task breakdown (`/idea.tasks`)
4. → Execute Phase 1 with IDE Loop (`/idea.implement`)

---

**End of Implementation Plan**
