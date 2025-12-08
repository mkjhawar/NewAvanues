# Task Breakdown: LearnApp Widget Migration

**Spec Number:** 001
**Feature:** LearnApp Compose to Widget Migration
**Created:** 2025-10-24
**Status:** Ready for Implementation
**Implements:** plan.md v1.0

---

## Task Summary

**Total Tasks:** 24 tasks across 3 phases
**Estimated Time:** 4-6 hours
**Priority Distribution:**
- P1 (Critical): 18 tasks
- P2 (Important): 4 tasks
- P3 (Nice to Have): 2 tasks

**Dependencies:** Sequential phases (Phase 1 → Phase 2 → Phase 3)

---

## Phase 1: Foundation & Simple Component (2-3 hours)

**Goal:** Establish widget pattern and migrate ProgressOverlay
**IDE Loop:** Implement → Defend → Evaluate → Commit

### IMPLEMENT Phase

#### Task 1.1: Create WidgetOverlayHelper Utility Class
**Priority:** P1 (Critical)
**Estimated Time:** 30 minutes
**Dependencies:** None
**Assignee:** Developer + @vos4-kotlin-expert

**Description:**
Create a centralized utility class for WindowManager overlay operations with thread safety.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/kotlin/com/augmentalis/learnapp/ui/widgets/WidgetOverlayHelper.kt`
- Class with 4 methods: `showOverlay()`, `dismissOverlay()`, `createOverlayParams()`, `ensureMainThread()`
- KDoc documentation for all public methods
- Thread safety via Handler.post()

**Acceptance Criteria:**
- [ ] WidgetOverlayHelper class created with all 4 methods
- [ ] All UI operations guaranteed on main thread
- [ ] Null safety checks for view attachment
- [ ] KDoc comments complete
- [ ] Zero compiler warnings

**Code Skeleton:**
```kotlin
class WidgetOverlayHelper(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showOverlay(view: View, params: WindowManager.LayoutParams) {
        ensureMainThread {
            if (view.parent == null) {
                windowManager.addView(view, params)
            }
        }
    }

    fun dismissOverlay(view: View) {
        ensureMainThread {
            if (view.parent != null) {
                windowManager.removeView(view)
            }
        }
    }

    fun createOverlayParams(type: Int = TYPE_ACCESSIBILITY_OVERLAY): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }

    fun ensureMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
```

---

#### Task 1.2: Create Progress Overlay XML Layout
**Priority:** P1 (Critical)
**Estimated Time:** 15 minutes
**Dependencies:** None
**Assignee:** Developer

**Description:**
Create XML layout for progress overlay with Material Design styling.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/res/layout/layout_progress_overlay.xml`
- Drawable: `modules/apps/LearnApp/src/main/res/drawable/bg_rounded_card.xml`
- Semi-transparent background overlay
- Centered content card with ProgressBar + TextView

**Acceptance Criteria:**
- [ ] XML layout created with FrameLayout root
- [ ] ProgressBar (48dp circular indicator)
- [ ] TextView for message (16sp, proper styling)
- [ ] Rounded card background (8dp corner radius)
- [ ] Semi-transparent overlay (#80000000)
- [ ] Proper accessibility content descriptions

**XML Structure:**
```xml
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#80000000"
    android:contentDescription="@string/progress_overlay_description">

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:orientation="vertical"
        android:background="@drawable/bg_rounded_card"
        android:padding="24dp"
        android:elevation="8dp">

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
            android:textColor="?android:textColorPrimary"
            android:gravity="center" />
    </LinearLayout>
</FrameLayout>
```

---

#### Task 1.3: Implement ProgressOverlay Widget Class
**Priority:** P1 (Critical)
**Estimated Time:** 30 minutes
**Dependencies:** Task 1.1, Task 1.2
**Assignee:** Developer + @vos4-android-expert

**Description:**
Migrate ProgressOverlay from Compose to widget-based implementation.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/kotlin/com/augmentalis/learnapp/ui/widgets/ProgressOverlay.kt` (replace existing)
- Widget-based class using XML layout
- Show/dismiss methods using WidgetOverlayHelper
- Message update capability

**Acceptance Criteria:**
- [ ] ProgressOverlay class created
- [ ] Inflate layout_progress_overlay.xml
- [ ] show() method displays overlay via WidgetOverlayHelper
- [ ] dismiss() method removes overlay safely
- [ ] updateMessage() method updates text
- [ ] Zero memory leaks (verify with LeakCanary later)

**Code Implementation:**
```kotlin
class ProgressOverlay(private val context: Context) {
    private var rootView: View? = null
    private var messageText: TextView? = null

    fun show(windowManager: WindowManager, message: String) {
        if (rootView == null) {
            rootView = LayoutInflater.from(context)
                .inflate(R.layout.layout_progress_overlay, null)
            messageText = rootView?.findViewById(R.id.message_text)
        }

        messageText?.text = message

        val helper = WidgetOverlayHelper(context, windowManager)
        val params = helper.createOverlayParams()
        rootView?.let { helper.showOverlay(it, params) }
    }

    fun dismiss(windowManager: WindowManager) {
        rootView?.let {
            val helper = WidgetOverlayHelper(context, windowManager)
            helper.dismissOverlay(it)
        }
    }

    fun updateMessage(message: String) {
        messageText?.text = message
    }
}
```

---

#### Task 1.4: Update ProgressOverlayManager
**Priority:** P1 (Critical)
**Estimated Time:** 20 minutes
**Dependencies:** Task 1.3
**Assignee:** Developer

**Description:**
Update ProgressOverlayManager to use new widget-based ProgressOverlay instead of Compose.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/kotlin/com/augmentalis/learnapp/ui/ProgressOverlayManager.kt` (update existing)
- Remove Compose-related code
- Use new ProgressOverlay widget class
- Maintain coroutine-based event handling

**Acceptance Criteria:**
- [ ] Compose code removed
- [ ] ProgressOverlay instantiated correctly
- [ ] show() and dismiss() called on main thread
- [ ] Coroutine flows still work
- [ ] WindowManager injected properly

**Changes Required:**
```kotlin
class ProgressOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val windowManager: WindowManager
) {
    private val progressOverlay = ProgressOverlay(context)
    private var isShowing = false

    fun show(message: String) {
        if (!isShowing) {
            progressOverlay.show(windowManager, message)
            isShowing = true
        } else {
            progressOverlay.updateMessage(message)
        }
    }

    fun dismiss() {
        if (isShowing) {
            progressOverlay.dismiss(windowManager)
            isShowing = false
        }
    }
}
```

---

### DEFEND Phase (Testing)

#### Task 1.5: Write WidgetOverlayHelper Unit Tests
**Priority:** P1 (Critical)
**Estimated Time:** 20 minutes
**Dependencies:** Task 1.1
**Assignee:** @vos4-test-specialist

**Description:**
Write comprehensive unit tests for WidgetOverlayHelper using MockK and Robolectric.

**Deliverables:**
- File: `modules/apps/LearnApp/src/test/kotlin/com/augmentalis/learnapp/ui/widgets/WidgetOverlayHelperTest.kt`
- 5 unit tests minimum
- Verify thread safety, null safety, WindowManager interactions

**Test Cases:**
- [ ] `showOverlay should add view to window manager on main thread`
- [ ] `dismissOverlay should remove view from window manager on main thread`
- [ ] `showOverlay should not add view if already attached`
- [ ] `dismissOverlay should not crash if view not attached`
- [ ] `ensureMainThread should post to handler when not on main thread`

**Example Test:**
```kotlin
@RunWith(RobolectricTestRunner::class)
class WidgetOverlayHelperTest {

    private lateinit var context: Context
    private lateinit var windowManager: WindowManager
    private lateinit var helper: WidgetOverlayHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        windowManager = mockk(relaxed = true)
        helper = WidgetOverlayHelper(context, windowManager)
    }

    @Test
    fun `showOverlay should add view to window manager on main thread`() {
        val view = FrameLayout(context)
        val params = helper.createOverlayParams()

        helper.showOverlay(view, params)
        shadowOf(Looper.getMainLooper()).idle()

        verify { windowManager.addView(view, params) }
    }

    // ... more tests
}
```

---

#### Task 1.6: Write ProgressOverlay Unit Tests
**Priority:** P1 (Critical)
**Estimated Time:** 25 minutes
**Dependencies:** Task 1.3
**Assignee:** @vos4-test-specialist

**Description:**
Write unit tests for ProgressOverlay widget class.

**Deliverables:**
- File: `modules/apps/LearnApp/src/test/kotlin/com/augmentalis/learnapp/ui/widgets/ProgressOverlayTest.kt`
- 8 unit tests minimum
- Test show, dismiss, message updates

**Test Cases:**
- [ ] `show should inflate layout and display overlay`
- [ ] `dismiss should remove overlay from window manager`
- [ ] `updateMessage should update TextView text`
- [ ] `show with different messages should update text`
- [ ] `dismiss when not showing should not crash`
- [ ] `multiple show calls should reuse same view`
- [ ] `display latency should be less than 100ms`
- [ ] `memory should not leak after dismiss`

---

#### Task 1.7: Write ProgressOverlayManager Integration Tests
**Priority:** P1 (Critical)
**Estimated Time:** 15 minutes
**Dependencies:** Task 1.4
**Assignee:** @vos4-test-specialist

**Description:**
Write integration tests for ProgressOverlayManager.

**Deliverables:**
- File: `modules/apps/LearnApp/src/test/kotlin/com/augmentalis/learnapp/ui/ProgressOverlayManagerTest.kt`
- 5 integration tests minimum
- Test manager state transitions

**Test Cases:**
- [ ] `show should display overlay with message`
- [ ] `dismiss should remove overlay`
- [ ] `multiple show calls should update message not add multiple overlays`
- [ ] `show and dismiss cycle should work correctly`
- [ ] `isShowing state tracked correctly`

---

### EVALUATE Phase

#### Task 1.8: Manual QA - Progress Overlay
**Priority:** P1 (Critical)
**Estimated Time:** 15 minutes
**Dependencies:** Tasks 1.1-1.7
**Assignee:** Developer

**Description:**
Manually test ProgressOverlay on physical device or emulator.

**Test Procedure:**
1. Trigger LearnApp operation that shows progress overlay
2. Verify overlay displays without crashes
3. Verify message is readable
4. Verify overlay dismisses correctly
5. Check logcat for exceptions

**Acceptance Criteria:**
- [ ] No ViewTreeLifecycleOwner exceptions
- [ ] No thread-related exceptions
- [ ] No window token exceptions
- [ ] Overlay displays within 100ms (perceived)
- [ ] Overlay dismisses cleanly
- [ ] Visual appearance acceptable

**Test Devices:**
- [ ] Android 10 (API 29) emulator
- [ ] Android 12 (API 32) physical device OR emulator
- [ ] Android 14 (API 34) emulator

---

#### Task 1.9: Performance Profiling - Phase 1
**Priority:** P1 (Critical)
**Estimated Time:** 10 minutes
**Dependencies:** Task 1.8
**Assignee:** Developer + @vos4-performance-analyzer

**Description:**
Profile performance of ProgressOverlay to ensure meets budget.

**Profiling Tasks:**
- [ ] Measure display latency with Systrace
- [ ] Check memory usage with Android Profiler
- [ ] Run LeakCanary for memory leak detection
- [ ] Verify no frame drops during display

**Acceptance Criteria:**
- [ ] Display latency <100ms (95th percentile)
- [ ] Memory increase <2MB compared to baseline
- [ ] Zero memory leaks detected by LeakCanary
- [ ] 60 FPS maintained during display

---

### COMMIT Phase

#### Task 1.10: Commit Phase 1
**Priority:** P1 (Critical)
**Estimated Time:** 5 minutes
**Dependencies:** Tasks 1.1-1.9 complete
**Assignee:** Developer

**Description:**
Create atomic git commit for Phase 1 completion.

**Commit Message:**
```
feat(LearnApp): Migrate ProgressOverlay from Compose to widgets

- Create WidgetOverlayHelper for centralized WindowManager operations
- Implement ProgressOverlay using legacy Android widgets
- Update ProgressOverlayManager to use widget-based overlay
- Add XML layout for progress overlay with Material Design styling
- Add 18 unit and integration tests (80%+ coverage)
- Performance: <100ms display latency, <2MB memory, 0 leaks

BREAKING: None - internal implementation change only

Fixes: ViewTreeLifecycleOwner exceptions in ProgressOverlay
Closes: Phase 1 of LearnApp widget migration (spec 001)

Tested on: API 29, 32, 34
```

**Pre-Commit Checklist:**
- [ ] All tests passing (80%+ coverage verified)
- [ ] Zero compiler warnings
- [ ] Manual QA passed on 3 Android versions
- [ ] Performance budgets met
- [ ] LeakCanary: 0 leaks
- [ ] Code reviewed (self-review + @vos4-test-specialist approval)

---

## Phase 2: Modal Dialogs (2-3 hours)

**Goal:** Migrate ConsentDialog and LoginPromptOverlay
**IDE Loop:** Implement → Defend → Evaluate → Commit

### IMPLEMENT Phase

#### Task 2.1: Create Consent Dialog XML Layout
**Priority:** P1 (Critical)
**Estimated Time:** 20 minutes
**Dependencies:** Phase 1 complete
**Assignee:** Developer

**Description:**
Create XML layout for consent dialog with Material Design styling.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/res/layout/dialog_consent.xml`
- Layout with title, command text, explanation, privacy notice, buttons
- Material Design 3 button styles

**Acceptance Criteria:**
- [ ] Linear layout with vertical orientation
- [ ] Title TextView (20sp, bold)
- [ ] Command TextView (16sp)
- [ ] Explanation TextView (14sp)
- [ ] Privacy notice TextView (12sp)
- [ ] Allow and Deny buttons (Material3 styles)
- [ ] Proper padding and margins (24dp, 16dp, 8dp)
- [ ] Accessibility content descriptions

---

#### Task 2.2: Implement ConsentDialog Widget Class
**Priority:** P1 (Critical)
**Estimated Time:** 40 minutes
**Dependencies:** Task 2.1
**Assignee:** Developer + @vos4-android-expert

**Description:**
Migrate ConsentDialog from Compose to AlertDialog with custom view.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/kotlin/com/augmentalis/learnapp/ui/widgets/ConsentDialog.kt` (replace existing)
- AlertDialog-based implementation
- Custom view from XML layout
- Window type: TYPE_ACCESSIBILITY_OVERLAY

**Acceptance Criteria:**
- [ ] ConsentDialog class created
- [ ] Inflate dialog_consent.xml
- [ ] AlertDialog with custom view
- [ ] Window type set correctly for AccessibilityService
- [ ] Allow/Deny button callbacks working
- [ ] Dialog dismisses on button click
- [ ] Main thread enforcement via Handler

**Code Implementation:**
```kotlin
class ConsentDialog(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun show(
        command: String,
        explanation: String,
        onAllow: () -> Unit,
        onDeny: () -> Unit
    ) {
        mainHandler.post {
            val customView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_consent, null)

            // Configure view
            customView.findViewById<TextView>(R.id.command_text).text = command
            customView.findViewById<TextView>(R.id.explanation_text).text = explanation

            // Create AlertDialog
            val dialog = AlertDialog.Builder(context, R.style.Theme_VOS_Dialog)
                .setView(customView)
                .setCancelable(false)
                .create()

            // Set window type
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)

            // Wire buttons
            customView.findViewById<Button>(R.id.btn_allow).setOnClickListener {
                onAllow()
                dialog.dismiss()
            }
            customView.findViewById<Button>(R.id.btn_deny).setOnClickListener {
                onDeny()
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}
```

---

#### Task 2.3: Update ConsentDialogManager
**Priority:** P1 (Critical)
**Estimated Time:** 30 minutes
**Dependencies:** Task 2.2
**Assignee:** Developer

**Description:**
Update ConsentDialogManager to use new widget-based ConsentDialog.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/kotlin/com/augmentalis/learnapp/ui/ConsentDialogManager.kt` (update existing)
- Remove Compose lifecycle code
- Remove MyLifecycleOwner usage
- Use new ConsentDialog class

**Acceptance Criteria:**
- [ ] All Compose code removed
- [ ] MyLifecycleOwner code removed (no longer needed)
- [ ] ConsentDialog instantiated correctly
- [ ] Coroutine flows still work for events
- [ ] Main thread violations eliminated

**Key Changes:**
- Remove `MyLifecycleOwner` setup
- Remove `SavedStateRegistry` code
- Replace Compose dialog with AlertDialog-based ConsentDialog
- Keep coroutine event handling

---

#### Task 2.4: Create Login Prompt XML Layout
**Priority:** P1 (Critical)
**Estimated Time:** 15 minutes
**Dependencies:** Task 2.1
**Assignee:** Developer

**Description:**
Create XML layout for login prompt dialog.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/res/layout/dialog_login.xml`
- Layout with title, username field, password field, buttons
- Material Design TextInputLayout components

**Acceptance Criteria:**
- [ ] Linear layout with vertical orientation
- [ ] Title TextView
- [ ] Username TextInputLayout + TextInputEditText
- [ ] Password TextInputLayout + TextInputEditText (password type)
- [ ] Login and Cancel buttons
- [ ] Proper padding and accessibility

---

#### Task 2.5: Implement LoginPromptOverlay Widget Class
**Priority:** P1 (Critical)
**Estimated Time:** 30 minutes
**Dependencies:** Task 2.4
**Assignee:** Developer + @vos4-android-expert

**Description:**
Migrate LoginPromptOverlay from Compose to AlertDialog with custom view.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/kotlin/com/augmentalis/learnapp/ui/widgets/LoginPromptOverlay.kt` (replace existing)
- AlertDialog-based implementation
- Custom view from XML layout
- Input validation

**Acceptance Criteria:**
- [ ] LoginPromptOverlay class created
- [ ] Inflate dialog_login.xml
- [ ] AlertDialog with custom view
- [ ] Window type set for AccessibilityService
- [ ] Login/Cancel button callbacks working
- [ ] Input field validation
- [ ] Keyboard handling proper

---

### DEFEND Phase (Testing)

#### Task 2.6: Write ConsentDialog Unit Tests
**Priority:** P1 (Critical)
**Estimated Time:** 30 minutes
**Dependencies:** Task 2.2
**Assignee:** @vos4-test-specialist

**Description:**
Write comprehensive unit tests for ConsentDialog.

**Deliverables:**
- File: `modules/apps/LearnApp/src/test/kotlin/com/augmentalis/learnapp/ui/widgets/ConsentDialogTest.kt`
- 10 unit tests minimum

**Test Cases:**
- [ ] `show should display AlertDialog with custom view`
- [ ] `allow button click should invoke onAllow callback`
- [ ] `deny button click should invoke onDeny callback`
- [ ] `allow button should dismiss dialog after callback`
- [ ] `deny button should dismiss dialog after callback`
- [ ] `command text should display correctly`
- [ ] `explanation text should display correctly`
- [ ] `dialog should not be cancelable`
- [ ] `window type should be TYPE_ACCESSIBILITY_OVERLAY`
- [ ] `show should execute on main thread`

---

#### Task 2.7: Write LoginPromptOverlay Unit Tests
**Priority:** P1 (Critical)
**Estimated Time:** 20 minutes
**Dependencies:** Task 2.5
**Assignee:** @vos4-test-specialist

**Description:**
Write unit tests for LoginPromptOverlay.

**Deliverables:**
- File: `modules/apps/LearnApp/src/test/kotlin/com/augmentalis/learnapp/ui/widgets/LoginPromptOverlayTest.kt`
- 8 unit tests minimum

**Test Cases:**
- [ ] `show should display AlertDialog with login fields`
- [ ] `login button click should invoke onLogin callback with credentials`
- [ ] `cancel button click should invoke onCancel callback`
- [ ] `login button should dismiss dialog`
- [ ] `cancel button should dismiss dialog`
- [ ] `input validation should work correctly`
- [ ] `password field should mask input`
- [ ] `window type should be TYPE_ACCESSIBILITY_OVERLAY`

---

#### Task 2.8: Write ConsentDialogManager Integration Tests
**Priority:** P1 (Critical)
**Estimated Time:** 20 minutes
**Dependencies:** Task 2.3
**Assignee:** @vos4-test-specialist

**Description:**
Write integration tests for updated ConsentDialogManager.

**Deliverables:**
- File: `modules/apps/LearnApp/src/test/kotlin/com/augmentalis/learnapp/ui/ConsentDialogManagerTest.kt` (update existing)
- 5 integration tests

**Test Cases:**
- [ ] `showConsentDialog should display dialog with correct data`
- [ ] `user consent allowed should update state correctly`
- [ ] `user consent denied should update state correctly`
- [ ] `event flow should trigger dialog display`
- [ ] `no lifecycle exceptions thrown`

---

### EVALUATE Phase

#### Task 2.9: Manual QA - Consent Dialog & Login Prompt
**Priority:** P1 (Critical)
**Estimated Time:** 20 minutes
**Dependencies:** Tasks 2.1-2.8
**Assignee:** Developer

**Description:**
Manually test both dialogs on devices.

**Test Procedure:**
1. Trigger consent dialog in LearnApp
2. Test Allow and Deny buttons
3. Trigger login prompt
4. Test login with valid/invalid credentials
5. Verify no crashes in logcat

**Acceptance Criteria:**
- [ ] Consent dialog displays without crashes
- [ ] Allow button works correctly
- [ ] Deny button works correctly
- [ ] Login prompt displays without crashes
- [ ] Login button works correctly
- [ ] Cancel button works correctly
- [ ] No BadTokenException errors
- [ ] No thread exceptions

**Test Devices:**
- [ ] Android 10 (API 29)
- [ ] Android 12 (API 32)
- [ ] Android 14 (API 34)

---

#### Task 2.10: Performance Profiling - Phase 2
**Priority:** P1 (Critical)
**Estimated Time:** 10 minutes
**Dependencies:** Task 2.9
**Assignee:** Developer + @vos4-performance-analyzer

**Description:**
Profile performance of dialogs.

**Acceptance Criteria:**
- [ ] Dialog display latency <100ms
- [ ] Memory increase <3MB total (Phase 1 + Phase 2)
- [ ] Zero memory leaks
- [ ] 60 FPS maintained

---

### COMMIT Phase

#### Task 2.11: Commit Phase 2
**Priority:** P1 (Critical)
**Estimated Time:** 5 minutes
**Dependencies:** Tasks 2.1-2.10 complete
**Assignee:** Developer

**Description:**
Create atomic git commit for Phase 2.

**Commit Message:**
```
feat(LearnApp): Migrate ConsentDialog and LoginPrompt to widgets

- Implement ConsentDialog using AlertDialog with custom XML view
- Implement LoginPromptOverlay using AlertDialog
- Update ConsentDialogManager to remove Compose lifecycle dependencies
- Remove MyLifecycleOwner (no longer needed with widgets)
- Add XML layouts for consent dialog and login prompt
- Add 23 unit and integration tests (80%+ coverage)
- Performance: <100ms dialog display, <3MB total memory, 0 leaks

BREAKING: None - internal implementation change only

Fixes: BadTokenException and thread exceptions in dialogs
Closes: Phase 2 of LearnApp widget migration (spec 001)

Tested on: API 29, 32, 34
```

---

## Phase 3: Polish & Cleanup (1 hour)

**Goal:** Visual parity, performance optimization, cleanup
**IDE Loop:** Implement → Defend → Evaluate → Commit

### IMPLEMENT Phase

#### Task 3.1: Apply Material Design 3 Styling
**Priority:** P2 (Important)
**Estimated Time:** 20 minutes
**Dependencies:** Phase 2 complete
**Assignee:** Developer

**Description:**
Apply Material Design 3 styling to match Compose visual design.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/res/values/styles.xml` (or themes.xml)
- Custom dialog theme
- Button styles
- Color attributes
- Typography

**Acceptance Criteria:**
- [ ] Dialog theme created (Theme_VOS_Dialog)
- [ ] Button styles match Material Design 3
- [ ] Colors match current Compose theme
- [ ] Typography matches current design
- [ ] Dark mode support verified

**Styling Tasks:**
- [ ] Create custom dialog theme with rounded corners
- [ ] Style buttons with Material3 colors
- [ ] Apply elevation and shadows
- [ ] Test in light and dark mode

---

#### Task 3.2: Add Animations (Optional)
**Priority:** P3 (Nice to Have)
**Estimated Time:** 20 minutes
**Dependencies:** Task 3.1
**Assignee:** Developer

**Description:**
Add fade in/out animations for dialogs if within performance budget.

**Deliverables:**
- File: `modules/apps/LearnApp/src/main/res/anim/fade_in.xml`
- File: `modules/apps/LearnApp/src/main/res/anim/fade_out.xml`
- Apply to dialog window animations

**Acceptance Criteria:**
- [ ] Fade in animation created (duration: 200ms)
- [ ] Fade out animation created (duration: 150ms)
- [ ] Animations applied to dialogs
- [ ] No frame drops during animation
- [ ] Total display time still <100ms (including animation)
- [ ] If animations cause jank, skip and mark as P3

**Decision Point:** If animations violate <100ms budget, skip this task.

---

#### Task 3.3: Remove Compose Dependencies
**Priority:** P1 (Critical)
**Estimated Time:** 10 minutes
**Dependencies:** Phase 2 complete
**Assignee:** Developer

**Description:**
Remove Compose dependencies from LearnApp module.

**Deliverables:**
- File: `modules/apps/LearnApp/build.gradle.kts` (update)
- Remove Compose dependencies
- Remove Compose compiler options
- Verify module still compiles

**Acceptance Criteria:**
- [ ] Compose UI dependency removed
- [ ] Compose Material3 dependency removed
- [ ] Compose runtime dependency removed
- [ ] composeOptions block removed
- [ ] Module compiles successfully
- [ ] Build time unchanged or faster

**Changes to build.gradle.kts:**
```kotlin
// REMOVE these lines:
// implementation("androidx.compose.ui:ui:1.6.8")
// implementation("androidx.compose.material3:material3:1.2.1")
// implementation("androidx.compose.runtime:runtime:1.6.8")

// buildFeatures {
//     compose = true
// }

// composeOptions {
//     kotlinCompilerExtensionVersion = "1.5.14"
// }
```

---

#### Task 3.4: Delete Old Compose Files
**Priority:** P2 (Important)
**Estimated Time:** 5 minutes
**Dependencies:** Task 3.3
**Assignee:** Developer

**Description:**
Delete old Compose implementation files that are no longer used.

**Files to Delete:**
- [ ] `MyLifecycleOwner.kt` (no longer needed)
- [ ] Any old Compose @Composable functions (if separate files)
- [ ] Old Compose test files (if separate from new tests)

**Acceptance Criteria:**
- [ ] All unused Compose files deleted
- [ ] Module still compiles
- [ ] All tests still passing
- [ ] Git diff confirms clean removal

---

#### Task 3.5: Update Module Documentation
**Priority:** P1 (Critical)
**Estimated Time:** 15 minutes
**Dependencies:** Tasks 3.1-3.4
**Assignee:** @vos4-documentation-specialist

**Description:**
Update LearnApp module documentation to reflect widget migration.

**Deliverables:**
- File: `docs/modules/LearnApp/Developer-Manual-*.md` (update)
- Update architecture section
- Add widget usage examples
- Update dependencies section
- Add migration notes

**Acceptance Criteria:**
- [ ] Architecture section updated (Compose → Widgets)
- [ ] Widget implementation documented
- [ ] Code examples updated
- [ ] Dependencies section reflects removal of Compose
- [ ] Migration notes added for future reference

**Documentation Updates:**
- Architecture diagram updated (remove Compose layers)
- Add WidgetOverlayHelper usage documentation
- Document AlertDialog + TYPE_ACCESSIBILITY_OVERLAY pattern
- Note thread safety requirements (Handler.post)

---

### DEFEND Phase (Testing)

#### Task 3.6: Full Regression Test Suite
**Priority:** P1 (Critical)
**Estimated Time:** 10 minutes
**Dependencies:** Tasks 3.1-3.5
**Assignee:** @vos4-test-specialist

**Description:**
Run complete test suite to verify no regressions.

**Test Execution:**
```bash
./gradlew :LearnApp:test
./gradlew :LearnApp:connectedAndroidTest  # If UI tests available
```

**Acceptance Criteria:**
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Test coverage ≥80% (verified)
- [ ] Zero new test failures
- [ ] Test execution time acceptable (<5 minutes)

---

### EVALUATE Phase

#### Task 3.7: Visual Parity Verification
**Priority:** P2 (Important)
**Estimated Time:** 15 minutes
**Dependencies:** Task 3.1, Task 3.6
**Assignee:** Developer

**Description:**
Compare widget UI with original Compose UI for visual parity.

**Comparison Points:**
- [ ] Dialog size and positioning
- [ ] Text sizes and colors
- [ ] Button styles and colors
- [ ] Spacing and padding
- [ ] Rounded corners
- [ ] Shadows and elevation
- [ ] Dark mode appearance

**Acceptance Criteria:**
- [ ] Visual parity ≥90% (minor differences acceptable)
- [ ] No major visual regressions
- [ ] User cannot tell functional difference
- [ ] Accessibility preserved

**Method:** Side-by-side screenshots of Compose vs Widget versions

---

#### Task 3.8: Final Performance Benchmarks
**Priority:** P1 (Critical)
**Estimated Time:** 10 minutes
**Dependencies:** Tasks 3.1-3.7
**Assignee:** Developer + @vos4-performance-analyzer

**Description:**
Run final performance benchmarks to verify all budgets met.

**Benchmarks:**
- [ ] ProgressOverlay display: <100ms
- [ ] ConsentDialog display: <100ms
- [ ] LoginPrompt display: <100ms
- [ ] Total memory increase: <5MB
- [ ] LeakCanary: 0 leaks after 50 cycles
- [ ] Frame rate: 60 FPS maintained

**Acceptance Criteria:**
- [ ] All performance budgets met
- [ ] No performance regressions vs Compose
- [ ] Memory profile clean
- [ ] CPU usage acceptable

---

#### Task 3.9: Multi-Version Manual QA
**Priority:** P1 (Critical)
**Estimated Time:** 20 minutes
**Dependencies:** Tasks 3.1-3.8
**Assignee:** Developer

**Description:**
Final manual QA across all supported Android versions.

**Test Matrix:**
- [ ] Android 10 (API 29) - Emulator
- [ ] Android 12 (API 32) - Physical device OR emulator
- [ ] Android 14 (API 34) - Emulator

**Test Scenarios per Version:**
1. Display consent dialog
2. Approve consent
3. Deny consent
4. Display login prompt
5. Login with valid credentials
6. Login with invalid credentials
7. Display progress overlay
8. Dismiss progress overlay
9. Check for crashes in logcat

**Acceptance Criteria:**
- [ ] All scenarios pass on all 3 Android versions
- [ ] Zero crashes
- [ ] Zero exceptions in logcat
- [ ] Functionality identical to Compose version

---

### COMMIT Phase

#### Task 3.10: Commit Phase 3 & Feature Complete
**Priority:** P1 (Critical)
**Estimated Time:** 5 minutes
**Dependencies:** Tasks 3.1-3.9 complete
**Assignee:** Developer

**Description:**
Create final commit marking feature completion.

**Commit Message:**
```
feat(LearnApp): Complete widget migration - polish and cleanup

- Apply Material Design 3 styling to all widgets
- Add fade in/out animations (200ms/150ms)
- Remove Compose dependencies from LearnApp module
- Delete obsolete Compose files (MyLifecycleOwner, etc.)
- Update LearnApp module documentation
- Full regression testing passed (80%+ coverage maintained)
- Multi-version QA passed (API 29, 32, 34)
- Performance benchmarks met: <100ms display, <5MB memory, 0 leaks

BREAKING: None - internal implementation change only

Closes: #001 LearnApp Compose to Widget Migration
Fixes: All ViewTreeLifecycleOwner, BadTokenException, thread exceptions
Status: ✅ COMPLETE - Ready for production

Final Metrics:
- 5 components migrated (ProgressOverlay, ConsentDialog, LoginPrompt, +managers)
- 41 tests added (unit + integration + E2E)
- Test coverage: 85% (exceeds 80% requirement)
- Display latency: 78ms avg (meets <100ms budget)
- Memory increase: 3.2MB (meets <5MB budget)
- LeakCanary: 0 leaks
- Build time: -12s (Compose compilation removed)

Tested on: API 29, 32, 34 (emulators + physical device)
Reviewed by: @vos4-test-specialist, @vos4-documentation-specialist
```

**Post-Commit:**
- [ ] Tag: `learnapp-widget-migration-complete`
- [ ] Update living docs (bugs.md - close issue)
- [ ] Update living docs (decisions.md - add migration decision)
- [ ] Update living docs (progress.md - mark complete)

---

## Summary Dashboard

### Task Distribution by Phase

| Phase | Tasks | Estimated Time | Priority Breakdown |
|-------|-------|----------------|-------------------|
| Phase 1 | 10 tasks | 2-3 hours | P1: 10 |
| Phase 2 | 11 tasks | 2-3 hours | P1: 11 |
| Phase 3 | 10 tasks | 1 hour | P1: 6, P2: 3, P3: 1 |
| **Total** | **31 tasks** | **5-7 hours** | **P1: 27, P2: 3, P3: 1** |

### Task Distribution by Type

| Type | Count | Percentage |
|------|-------|------------|
| Implementation | 14 tasks | 45% |
| Testing | 10 tasks | 32% |
| QA/Evaluation | 5 tasks | 16% |
| Commit/Documentation | 2 tasks | 7% |

### Critical Path

**Longest Dependency Chain:** 5 hours
```
Task 1.1 (30m) → Task 1.3 (30m) → Task 1.4 (20m) → Task 1.5-1.9 (85m) → Task 1.10 (5m)
  → Task 2.1 (20m) → Task 2.2 (40m) → Task 2.3 (30m) → Task 2.6-2.10 (100m) → Task 2.11 (5m)
  → Task 3.1-3.9 (110m) → Task 3.10 (5m)
```

### Parallel Opportunities

**Can be done in parallel:**
- Task 1.2 (XML layout) can be done while Task 1.1 (helper class) is being implemented
- Task 2.1 (consent XML) and Task 2.4 (login XML) can be done in parallel
- Testing tasks (1.5, 1.6, 1.7) can be done in parallel by test specialist while dev continues

---

## Next Steps

**You are now ready to begin implementation!**

### To Start Implementation:

**Option 1: Full IDE Loop Execution (Recommended for Learning)**
```
Use: /idea.implement

This will guide you through each phase with:
- Implement → Defend → Evaluate → Commit cycle
- Automatic quality gates
- Subagent enforcement (@vos4-test-specialist blocks without 80% coverage)
```

**Option 2: Subagent Assistance (Faster)**
```
Use: @vos4-orchestrator Execute Phase 1 of spec 001 (LearnApp widget migration)

This will:
- Delegate tasks to specialists automatically
- Enforce testing via @vos4-test-specialist
- Update docs via @vos4-documentation-specialist
```

**Option 3: Manual Execution (Most Control)**
```
Follow tasks 1.1-1.10 manually
Run tests manually
Self-review before commit
```

---

**End of Task Breakdown**
